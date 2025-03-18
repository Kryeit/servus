package com.kryeit.stripe;

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

    /**
     * HTTP POST Request to /api/payment/create
     * Creates a new payment intent.
     * @param ctx the Javalin HTTP context
     * @throws StripeException if there is an error with the Stripe API
     */
    public static void createPaymentIntent(Context ctx) throws StripeException {

        JSONObject body = new JSONObject(ctx.body());

        JSONObject cart = body.getJSONObject("cart");

        String currency = "eur";

        // Extract additional order details
        String email = body.optString("email", "");
        String phone = body.optString("phone", "");
        String destination = body.optString("destination", "");

        if (Objects.equals(email, "") || Objects.equals(destination, "")) {
            throw new IllegalArgumentException("Email and destination are required");
        }

        if (!StockUtils.areAllProductsStocked(cart)) {
            throw new IllegalArgumentException("One or more products are out of stock");
        }

        // Prepare Stripe line items from cart
        List<Map<String, Object>> lineItems = new ArrayList<>();
        boolean hasNonVirtualProduct = false;
        boolean hasVirtualProduct = false;

        for (String id : cart.keySet()) {
            Long productId = Long.parseLong(id);
            JSONObject item = cart.getJSONObject(id);
            int quantity = item.getInt("quantity");

            Product product = Database.getJdbi().withHandle(handle -> handle.createQuery("""
            SELECT id, name, description, price, size, color, material, virtual, listed
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

        // Add shipping fee if there is at least one non-virtual product
        if (hasNonVirtualProduct) {
            lineItems.add(Map.of(
                    "price_data", Map.of(
                            "currency", currency,
                            "product_data", Map.of(
                                    "name", "Shipping Fee"
                            ),
                            "unit_amount", 1000 // 10€ in cents
                    ),
                    "quantity", 1
            ));
        }

        // Prepare metadata for the payment intent
        Map<String, String> metadata = new HashMap<>();

        String token = ctx.cookie("auth");

        UUID uuid = Jwt.validateToken(token);

        if (token != null && uuid != null) {
            metadata.put("uuid", uuid.toString());
        } else if (hasVirtualProduct) {
            throw new UnauthorizedResponse("Authentication is required for virtual products");
        }

        if (uuid != null) {
            metadata.put("uuid", uuid.toString());
        }
        metadata.put("cart", cart.toString());
        metadata.put("email", email);
        metadata.put("phone", phone);
        metadata.put("destination", destination);

        // Create Checkout session parameters
        Map<String, Object> params = new HashMap<>();
        params.put("payment_method_types", List.of("card"));
        params.put("line_items", lineItems);
        params.put("mode", "payment");
        params.put("success_url", "https://kryeit.com/orders");
        params.put("cancel_url", "https://kryeit.com/store");
        params.put("metadata", metadata); // Attach metadata to the session

        // Create the Checkout session
        Session session = Session.create(params);

        // Return session ID
        ctx.json(Map.of(
                "id", session.getId()
        ));
    }
}