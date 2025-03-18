package com.kryeit.merch;

import com.kryeit.Database;

import java.util.List;

public class OrderUtils {

    public static boolean hasNonVirtual(List<Long> productIds) {
        List<Product> products = Database.getJdbi().withHandle(handle -> {
            return handle.createQuery("""
            SELECT id, virtual
            FROM products
            WHERE id IN (<product_ids>)
            """)
                    .bindList("product_ids", productIds)
                    .mapTo(Product.class)
                    .list();
        });

        return products.stream().anyMatch(product -> !product.virtual());
    }
}
