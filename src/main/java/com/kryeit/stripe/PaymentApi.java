package com.kryeit.stripe;

import com.kryeit.Config;
import com.kryeit.Database;
import com.kryeit.auth.Jwt;
import com.kryeit.merch.Product;
import com.kryeit.merch.StockUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.json.JSONObject;

import java.util.*;

public class PaymentApi {


    public static void createPaymentIntent(Context ctx) throws StripeException {
        JSONObject body = new JSONObject(ctx.body());
        JSONObject cart = body.getJSONObject("cart");
        String currency = "eur";
        String email = body.optString("email", "");
        String phone = body.optString("phone", "");
        String destination = body.optString("destination", "");

        if (Objects.equals(email, "") || Objects.equals(destination, "")) {
            throw new IllegalArgumentException("Email and destination are required");
        }
        if (email.isBlank() || destination.isBlank()) {
            throw new io.javalin.http.BadRequestResponse("Email and destination are required");
        }


        if (!StockUtils.areAllProductsStocked(cart)) {
            throw new IllegalArgumentException("One or more products are out of stock");
        }

        List<Map<String, Object>> lineItems = new ArrayList<>();
        boolean hasNonVirtualProduct = false;
        boolean hasVirtualProduct = false;

        for (String id : cart.keySet()) {
            Long productId = Long.parseLong(id);
            JSONObject item = cart.getJSONObject(id);
            int quantity = item.getInt("quantity");

            Product product = Database.getJdbi().withHandle(handle -> handle.createQuery("""
            SELECT *
            FROM products
            WHERE id = :id
            """)
                    .bind("id", productId)
                    .mapTo(Product.class)
                    .findOne()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + productId))
            );

            int priceInCents = (int) (product.price() * 100);

            lineItems.add(Map.of(
                    "price_data", Map.of(
                            "currency", currency,
                            "product_data", Map.of(
                                    "name", product.name() + " - " + product.size()
                            ),
                            "unit_amount", priceInCents
                    ),
                    "quantity", quantity
            ));

            if (!product.virtual()) {
                hasNonVirtualProduct = true;
            } else {
                hasVirtualProduct = true;
            }
        }

        if (hasNonVirtualProduct) {
            lineItems.add(Map.of(
                    "price_data", Map.of(
                            "currency", currency,
                            "product_data", Map.of(
                                    "name", "Shipping Fee"
                            ),
                            "unit_amount", 1000
                    ),
                    "quantity", 1
            ));
        }

        Map<String, String> metadata = new HashMap<>();
        String token = ctx.cookie("auth");
        UUID uuid = Jwt.validateToken(token);

        if (token != null && uuid != null) {
            metadata.put("uuid", uuid.toString());
        } else if (hasVirtualProduct) {
            throw new UnauthorizedResponse("Authentication is required for virtual products");
        }

        metadata.put("cart", cart.toString());
        metadata.put("email", email);
        metadata.put("phone", phone);
        metadata.put("destination", destination);

        Map<String, Object> params = new HashMap<>();
        params.put("payment_method_types", List.of("card"));
        params.put("line_items", lineItems);
        params.put("mode", "payment");
        params.put("success_url", Config.FRONTEND_DOMAIN + "/orders?checkout=success&session_id={CHECKOUT_SESSION_ID}");
        params.put("cancel_url", Config.FRONTEND_DOMAIN + "/store");
        params.put("metadata", metadata);

        Session session = Session.create(params);
        ctx.json(Map.of("id", session.getId()));
    }

    public static void handlePaymentSuccess(Context ctx) {
        String sessionId = ctx.queryParam("session_id");
        if (sessionId == null || sessionId.isEmpty()) {
            ctx.status(400).result("Missing session ID");
            return;
        }

        try {
            Session session = Session.retrieve(sessionId);
            if (!"complete".equals(session.getStatus())) {
                ctx.status(400).result("Payment incomplete");
                return;
            }

            // Don't create the order here - webhook will handle it
            ctx.status(200).result("Payment verified");
        } catch (Exception e) {
            ctx.status(500).result("Error verifying payment: " + e.getMessage());
        }
    }
}