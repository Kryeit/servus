package com.kryeit.merch;

import com.kryeit.Database;
import org.json.JSONObject;

import java.util.List;
import java.util.Optional;

public class StockUtils {

    public static boolean areAllProductsStocked(JSONObject cart) {
        for (String key : cart.keySet()) {
            Long productId = Long.parseLong(key);
            int quantity = cart.getJSONObject(key).getInt("quantity");

            Optional<Integer> stockQuantity = Database.getJdbi().withHandle(handle ->
                    handle.createQuery("""
                    SELECT quantity
                    FROM stocks
                    WHERE product_id = :product_id
                """)
                            .bind("product_id", productId)
                            .mapTo(Integer.class)
                            .findOne()
            );

            if (stockQuantity.isEmpty() || stockQuantity.get() < quantity) {
                return false;
            }
        }
        return true;
    }

    // Reduces the stock of every item id by one
    public static void reduceStock(List<Long> productIds) {
        for (long productId : productIds) {
            Database.getJdbi().useHandle(handle -> {
                handle.createUpdate("""
            UPDATE stocks
            SET quantity = quantity - 1
            WHERE product_id = :product_id AND product_id IN (
                SELECT id FROM products WHERE id = :product_id AND virtual = false
            )
            """)
                        .bind("product_id", productId)
                        .execute();
            });
        }
    }
}
