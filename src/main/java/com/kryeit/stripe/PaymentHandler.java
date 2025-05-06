package com.kryeit.stripe;

import com.kryeit.Config;
import com.kryeit.Database;
import com.kryeit.merch.Order;
import com.kryeit.merch.StockUtils;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.javalin.http.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PaymentHandler {

    public static void handleStripeWebhook(Context ctx) {
        String payload = ctx.body();
        String sigHeader = ctx.header("Stripe-Signature");

        if (sigHeader == null) {
            ctx.status(400).result("Missing Stripe signature header");
            return;
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, Config.stripeEntrypointSecret);
        } catch (SignatureVerificationException e) {
            ctx.status(400).result("Invalid signature: " + e.getMessage());
            return;
        } catch (Exception e) {
            ctx.status(400).result("Invalid JSON payload");
            return;
        }

        if ("checkout.session.completed".equals(event.getType())) {
            JSONObject jsonEvent = new JSONObject(payload);
            try {
                JSONObject sessionData = jsonEvent
                        .getJSONObject("data")
                        .getJSONObject("object");
                String sessionId = sessionData.getString("id");

                // Idempotency: avoid processing the same session twice
                Long existingOrder = Database.getJdbi().withHandle(h ->
                        h.createQuery("SELECT id FROM orders WHERE transaction = :tx")
                                .bind("tx", sessionId)
                                .mapTo(Long.class)
                                .findOne()
                                .orElse(null)
                );
                if (existingOrder != null) {
                    ctx.status(200).result("Already processed");
                    return;
                }

                // Extract metadata
                JSONObject metadata = sessionData.optJSONObject("metadata");
                if (metadata == null) {
                    ctx.status(400).result("No metadata in session");
                    return;
                }
                String email = metadata.optString("email", "");
                String phone = metadata.optString("phone", "");
                String destination = metadata.optString("destination", "");
                String cartJson = metadata.optString("cart", "");
                String uuidStr = metadata.optString("uuid", "");

                if (email.isEmpty() || destination.isEmpty()) {
                    ctx.status(400).result("Missing required order information");
                    return;
                }

                UUID uuidValue = null;
                if (!uuidStr.isBlank()) {
                    try {
                        uuidValue = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                // Parse cart JSON into a list of product IDs
                List<Long> productIds = new ArrayList<>();
                if (!cartJson.isBlank()) {
                    String trimmed = cartJson.trim();
                    if (trimmed.startsWith("{")) {
                        JSONObject cart = new JSONObject(cartJson);
                        for (String key : cart.keySet()) {
                            Object obj = cart.get(key);
                            int qty = 1;
                            if (obj instanceof JSONObject) {
                                qty = ((JSONObject) obj).optInt("quantity", 1);
                            } else if (obj instanceof Number) {
                                qty = ((Number) obj).intValue();
                            }
                            long pid = Long.parseLong(key);
                            for (int i = 0; i < qty; i++) {
                                productIds.add(pid);
                            }
                        }
                    } else if (trimmed.startsWith("[")) {
                        JSONArray arr = new JSONArray(cartJson);
                        for (int i = 0; i < arr.length(); i++) {
                            productIds.add(arr.getLong(i));
                        }
                    } else if (cartJson.contains(",")) {
                        for (String part : cartJson.split(",")) {
                            try {
                                productIds.add(Long.parseLong(part.trim()));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    } else {
                        try {
                            productIds.add(Long.parseLong(cartJson.trim()));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                // Insert the order, binding the cart array natively
                UUID finalUuidValue = uuidValue;
                long orderId = Database.getJdbi().withHandle(h ->
                        h.createUpdate("""
                        INSERT INTO orders
                          (uuid, cart, destination, phone, status, transaction, email)
                        VALUES
                          (:uuid, :cart, :destination, :phone, :status, :tx, :email)
                        RETURNING id
                    """)
                                .bindBySqlType("uuid", finalUuidValue, Types.OTHER)
                                .bind("cart", productIds.toArray(new Long[0]))
                                .bind("destination", destination)
                                .bind("phone", phone)
                                .bind("status", Order.Status.PENDING.toString())
                                .bind("tx", sessionId)
                                .bind("email", email)
                                .executeAndReturnGeneratedKeys("id")
                                .mapTo(Long.class)
                                .one()
                );

                // Reduce stock (let exceptions bubble so Stripe will retry on failure)
                StockUtils.reduceStock(productIds);

                // Fulfill virtual products
                if (uuidValue != null) {
                    productIds.stream()
                            .distinct()
                            .forEach(pid -> {
                                boolean isVirtual = Database.getJdbi().withHandle(h ->
                                        h.createQuery("SELECT virtual FROM products WHERE id = :id")
                                                .bind("id", pid)
                                                .mapTo(Boolean.class)
                                                .one()
                                );
                                if (isVirtual) {
                                    //CosmeticApi.addItemToWardrobe(uuidValue, pid);
                                }
                            });
                }

                ctx.status(200).result("Order processed successfully");

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error processing order: " + e.getMessage());
            }

        } else {
            ctx.status(200).result("Event received");
        }
    }
}
