package com.kryeit.merch;

import com.kryeit.Database;
import com.kryeit.storage.ProductImages;
import io.javalin.http.Context;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * API for providing structured product information to the frontend
 * in a format that's easier to consume.
 */
public class ProductStructureApi {

    /**
     * Returns a complete product catalog structure that includes all variants
     * organized by product name, with colors and sizes properly nested.
     *
     * GET /api/products/catalog
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getProductCatalog(Context ctx) {
        List<Product> allProducts = Database.getJdbi().withHandle(handle -> handle.createQuery("""
            SELECT id, name, description, price, size, color, material, virtual, listed, created_at, updated_at
            FROM products
            WHERE listed = true
            ORDER BY name
            """)
                .mapTo(Product.class)
                .list());

        Map<String, List<Product>> productsByName = allProducts.stream()
                .collect(Collectors.groupingBy(Product::name));

        List<Map<String, Object>> catalog = new ArrayList<>();

        for (Map.Entry<String, List<Product>> entry : productsByName.entrySet()) {
            String productName = entry.getKey();
            List<Product> variants = entry.getValue();

            Product firstVariant = variants.get(0);

            Map<String, List<Product>> variantsByColor = variants.stream()
                    .collect(Collectors.groupingBy(Product::color));

            List<Map<String, Object>> colorVariants = new ArrayList<>();
            for (Map.Entry<String, List<Product>> colorEntry : variantsByColor.entrySet()) {
                String color = colorEntry.getKey();
                List<Product> colorProducts = colorEntry.getValue();

                List<Long> productIds = colorProducts.stream()
                        .map(Product::id)
                        .collect(Collectors.toList());

                int totalStock = Database.getJdbi().withHandle(handle ->
                        handle.createQuery("""
                        SELECT COALESCE(SUM(quantity), 0)
                        FROM stocks
                        WHERE product_id IN (<product_ids>)
                        """)
                                .bindList("product_ids", productIds)
                                .mapTo(Integer.class)
                                .one()
                );

                List<String> images;
                try {
                    images = ProductImages.getImages(productName);
                } catch (Exception e) {
                    images = new ArrayList<>();
                }

                List<Map<String, Object>> sizeVariants = new ArrayList<>();
                List<String> sizeOrder = List.of("XS", "S", "M", "L", "XL", "XXL");

                for (Product p : colorProducts) {
                    int stock = Database.getJdbi().withHandle(handle ->
                            handle.createQuery("""
                            SELECT COALESCE(quantity, 0)
                            FROM stocks
                            WHERE product_id = :product_id
                            """)
                                    .bind("product_id", p.id())
                                    .mapTo(Integer.class)
                                    .one()
                    );

                    Map<String, Object> sizeVariant = new HashMap<>();
                    sizeVariant.put("id", p.id());
                    sizeVariant.put("size", p.size());
                    sizeVariant.put("stock", stock);
                    sizeVariants.add(sizeVariant);
                }

                sizeVariants.sort((a, b) -> {
                    String sizeA = (String) a.get("size");
                    String sizeB = (String) b.get("size");
                    int indexA = sizeOrder.indexOf(sizeA);
                    int indexB = sizeOrder.indexOf(sizeB);
                    return Integer.compare(indexA, indexB);
                });

                Map<String, Object> colorVariant = new HashMap<>();
                colorVariant.put("color", color);
                colorVariant.put("colorCode", color.startsWith("#") ? color.substring(1) : color);
                colorVariant.put("stock", totalStock);
                colorVariant.put("sizes", sizeVariants);
                colorVariant.put("images", images);
                colorVariants.add(colorVariant);
            }

            Map<String, Object> productEntry = new HashMap<>();
            productEntry.put("name", productName);
            productEntry.put("description", firstVariant.description());
            productEntry.put("price", firstVariant.price());
            productEntry.put("virtual", firstVariant.virtual());
            productEntry.put("material", firstVariant.material());
            productEntry.put("colorVariants", colorVariants);
            catalog.add(productEntry);
        }

        ctx.json(catalog);
    }

    /**
     * Returns complete information about a specific product, including all its variants
     *
     * GET /api/products/details?name={name}
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getProductDetails(Context ctx) {
        String productName = ctx.queryParam("name");

        if (productName == null) {
            ctx.status(400).result("Product name is required");
            return;
        }

        // Get all variants of the product
        List<Product> variants = Database.getJdbi().withHandle(handle -> handle.createQuery("""
            SELECT id, name, description, price, size, color, material, virtual, listed, created_at, updated_at
            FROM products
            WHERE name = :name
            """)
                .bind("name", productName)
                .mapTo(Product.class)
                .list());

        if (variants.isEmpty()) {
            ctx.status(404).result("Product not found");
            return;
        }

        // Group variants by color
        Map<String, List<Product>> variantsByColor = variants.stream()
                .collect(Collectors.groupingBy(Product::color));

        // Get first variant for common details
        Product firstVariant = variants.get(0);

        // Create color variants with available sizes
        List<Map<String, Object>> colorVariants = new ArrayList<>();
        for (Map.Entry<String, List<Product>> entry : variantsByColor.entrySet()) {
            String color = entry.getKey();
            List<Product> colorProducts = entry.getValue();

            // Get stock for this color
            List<Long> productIds = colorProducts.stream()
                    .map(Product::id)
                    .collect(Collectors.toList());

            int totalStock = Database.getJdbi().withHandle(handle ->
                    handle.createQuery("""
                    SELECT COALESCE(SUM(quantity), 0)
                    FROM stocks
                    WHERE product_id IN (<product_ids>)
                    """)
                            .bindList("product_ids", productIds)
                            .mapTo(Integer.class)
                            .one()
            );

            // Get images
            List<String> images;
            try {
                images = ProductImages.getImages(productName);
            } catch (Exception e) {
                images = new ArrayList<>();
            }

            // Create size variants
            List<Map<String, Object>> sizeVariants = new ArrayList<>();
            List<String> sizeOrder = List.of("XS", "S", "M", "L", "XL", "XXL");

            for (Product p : colorProducts) {
                // Get stock for this specific size/color
                int stock = Database.getJdbi().withHandle(handle ->
                        handle.createQuery("""
                        SELECT COALESCE(quantity, 0)
                        FROM stocks
                        WHERE product_id = :product_id
                        """)
                                .bind("product_id", p.id())
                                .mapTo(Integer.class)
                                .one()
                );

                // Get discount for this specific product
                double discount = Database.getJdbi().withHandle(handle ->
                        handle.createQuery("""
                        SELECT COALESCE(discount, 0)
                        FROM stocks
                        WHERE product_id = :product_id
                        """)
                                .bind("product_id", p.id())
                                .mapTo(Double.class)
                                .one()
                );

                Map<String, Object> sizeVariant = new HashMap<>();
                sizeVariant.put("id", p.id());
                sizeVariant.put("size", p.size());
                sizeVariant.put("stock", stock);
                sizeVariant.put("discount", discount);
                sizeVariants.add(sizeVariant);
            }

            // Sort by size
            sizeVariants.sort((a, b) -> {
                String sizeA = (String) a.get("size");
                String sizeB = (String) b.get("size");
                int indexA = sizeOrder.indexOf(sizeA);
                int indexB = sizeOrder.indexOf(sizeB);
                return Integer.compare(indexA, indexB);
            });

            Map<String, Object> colorVariant = new HashMap<>();
            colorVariant.put("color", color);
            colorVariant.put("colorCode", color.startsWith("#") ? color.substring(1) : color);
            colorVariant.put("stock", totalStock);
            colorVariant.put("sizes", sizeVariants);
            colorVariant.put("images", images);
            colorVariants.add(colorVariant);
        }

        // Create product details
        Map<String, Object> productDetails = new HashMap<>();
        productDetails.put("name", productName);
        productDetails.put("description", firstVariant.description());
        productDetails.put("price", firstVariant.price());
        productDetails.put("virtual", firstVariant.virtual());
        productDetails.put("material", firstVariant.material());
        productDetails.put("colorVariants", colorVariants);

        ctx.json(productDetails);
    }
}