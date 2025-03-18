package com.kryeit.stripe;

import com.kryeit.Config;
import com.kryeit.Database;
import com.kryeit.cosmetics.CosmeticApi;
import com.kryeit.merch.Order;
import com.kryeit.merch.StockUtils;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.javalin.http.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class PaymentHandler {

    /**
     * HTTP POST Request to /api/payment/webhook
     * Handles Stripe webhook events.
     * Only handled by Stripe.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void handleStripeWebhook(Context ctx) {
        String payload = ctx.body();
        String sigHeader = ctx.header("Stripe-Signature");
        String endpointSecret = Config.stripeEntrypointSecret;

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            ctx.status(400).result("Webhook signature verification failed.");
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) event.getData().getObject();

                String uuid = session.getMetadata().get("uuid");
                UUID uuidValue;
                if (uuid != null && !uuid.isEmpty()) {
                    uuidValue = UUID.fromString(uuid);
                } else {
                    uuidValue = null;
                }
                String email = session.getMetadata().get("email");
                String phone = session.getMetadata().get("phone");
                String destination = session.getMetadata().get("destination");

                if (email == null || email.isEmpty()) {
                    ctx.status(400).result("Email is required.");
                    return;
                }
                if (destination == null || destination.isEmpty()) {
                    ctx.status(400).result("Destination is required.");
                    return;
                }

                JSONObject cart = new JSONObject(session.getMetadata().get("cart"));
                List<Long> productIds = cart.keySet().stream()
                        .flatMap(id -> {
                            Long productId = Long.valueOf(id);
                            int quantity = cart.getJSONObject(id).getInt("quantity");
                            return java.util.Collections.nCopies(quantity, productId).stream();
                        })
                        .toList();


                long orderId = Database.getJdbi().withHandle(handle -> handle.createUpdate("""
                INSERT INTO orders (uuid, cart, destination, phone, status, transaction, email)
                VALUES (:uuid, :cart::jsonb, :destination, :phone, :status, :transaction, :email)
                RETURNING id
                """)
                        .bindBySqlType("uuid", uuidValue, java.sql.Types.OTHER) // Bind as UUID object or null
                        .bind("cart", new JSONArray(productIds).toString())
                        .bind("destination", destination)
                        .bind("phone", phone)
                        .bind("status", Order.Status.PENDING)
                        .bind("transaction", session.getId())
                        .bind("email", email)
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(Long.class)
                        .findOne()
                        .orElseThrow(() -> new IllegalStateException("Failed to insert order into database."))
                );

                StockUtils.reduceStock(productIds);

                for (String productId : cart.keySet()) {
                    boolean isVirtual = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                                    SELECT virtual
                                    FROM products
                                    WHERE id = :id
                                    """)
                            .bind("id", Long.parseLong(productId))
                            .mapTo(Boolean.class)
                            .findFirst()).orElse(false);

                    if (isVirtual) {
                        CosmeticApi.addItemToWardrobe(uuidValue, Long.parseLong(productId));
                    }
                }

                ctx.status(200).result("Order created with ID: " + orderId);
                break;

            case "payment_intent.payment_failed":
                PaymentIntent failedPaymentIntent = (PaymentIntent) event.getData().getObject();
                ctx.status(400).result("Payment failed.");
                break;
        }
    }
}