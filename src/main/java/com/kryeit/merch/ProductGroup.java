package com.kryeit.merch;

import com.kryeit.Database;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductGroup {

    final List<String> sizeOrder = List.of("XS", "S", "M", "L", "XL", "XXL");

    String name;

    public ProductGroup(String name) {
        this.name = name;
    }

    public List<Long> getProducts() {
        return Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
            SELECT id
            FROM products
            WHERE name = :name
            """)
                        .bind("name", name)
                        .mapTo(Long.class)
                        .list()
        );
    }

    public List<String> getSizesByColor(String color) {
        List<String> sizes = Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
        SELECT size
        FROM products
        WHERE color = :color AND name = :name
        """)
                        .bind("color", "#" + color)
                        .bind("name", name)
                        .mapTo(String.class)
                        .list()
        );

        return sizeOrder.stream()
                .filter(sizes::contains)
                .collect(Collectors.toList());
    }

    public List<String> getSizes() {
        Set<String> sizes = new LinkedHashSet<>(Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
            SELECT size
            FROM products
            WHERE name = :name
            """)
                        .bind("name", name)
                        .mapTo(String.class)
                        .list()
        ));

        return sizeOrder.stream()
                .filter(sizes::contains)
                .collect(Collectors.toList());
    }

    public int getStock() {
        List<Long> productIds = getProducts();
        if (productIds.isEmpty()) {
            return 0;
        }
        return Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
        SELECT SUM(quantity)
        FROM stocks
        WHERE product_id IN (<products>)
        """)
                        .bindList("products", productIds)
                        .mapTo(Integer.class)
                        .one()
        );
    }

    public HashMap<String, List<Product>> getProductsByColor() {
        List<Product> products = Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
    SELECT id, name, description, price, size, color, material, virtual, listed
    FROM products
    WHERE name = :name
    """)
                        .bind("name", name)
                        .mapTo(Product.class)
                        .list()
        );

        return products.stream()
                .collect(Collectors.groupingBy(Product::color, HashMap::new, Collectors.toList()));
    }

    public long getProductBySizeAndColor(String size, String color) {
        return Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
            SELECT id
            FROM products
            WHERE name = :name AND size = :size AND color = :color
            """)
                        .bind("name", name)
                        .bind("size", size)
                        .bind("color", "#" + color)
                        .mapTo(Long.class)
                        .findOne()
                        .orElse(-1L)
        );
    }




}
