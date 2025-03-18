package com.kryeit.merch;

import com.kryeit.Database;
import com.kryeit.panel.auth.AdminJwt;
import com.kryeit.storage.ProductImages;
import com.kryeit.utils.Utils;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.UploadedFile;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductApi {
    private static final Logger logger = LoggerFactory.getLogger(ProductApi.class);

    public static void handleSecurity(Context ctx) {
        String token = ctx.cookie("auth");

        if (token == null) {
            throw new UnauthorizedResponse("Auth token is missing");
        }

        long id = AdminJwt.validateToken(token);

        // Check if the user is an admin
        boolean isAdmin = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT EXISTS (SELECT 1 FROM admins WHERE id = :id)")
                        .bind("id", id)
                        .mapTo(Boolean.class)
                        .one()
        );

        if (!isAdmin) {
            throw new UnauthorizedResponse("User is not an admin");
        }
    }
    /**
     * HTTP PUT Request to /api/products
     * Creates a new product.
     *
     * Query Parameters:
     * - name: The name of the product.
     * - description: The description of the product.
     * - price: The price of the product.
     * - size (optional): The size of the product.
     * - color (optional): The color of the product.
     * - material (optional): The material of the product.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void createProduct(Context ctx) {
        handleSecurity(ctx);

        JSONObject body = new JSONObject(ctx.body());
        String name = body.getString("name");
        String description = body.getString("description");
        long price = body.getLong("price");

        String size = body.optString("size", "");
        String color = body.optString("color", "");
        String material = body.optString("material", "");
        boolean virtual = body.optBoolean("virtual", false);
        boolean listed = body.optBoolean("listed", true);

        // Add timestamps
        Timestamp now = new Timestamp(System.currentTimeMillis());

        Product product = Database.getJdbi().withHandle(handle ->
                handle.createUpdate("""
                    INSERT INTO products (name, description, price, size, color, material, virtual, listed, created_at, updated_at)
                    VALUES (:name, :description, :price, :size, :color, :material, :virtual, :listed, :created_at, :updated_at)
                    """)
                        .bind("name", name)
                        .bind("description", description)
                        .bind("price", price)
                        .bind("size", size)
                        .bind("color", color)
                        .bind("material", material)
                        .bind("virtual", virtual)
                        .bind("listed", listed)
                        .bind("created_at", now)
                        .bind("updated_at", now)
                        .executeAndReturnGeneratedKeys("id", "name", "description", "price", "size", "color", "material", "virtual", "listed", "created_at", "updated_at")
                        .mapTo(Product.class)
                        .one()
        );

        // Setup stock
        StockApi.setupStock(product.id());

        ctx.json(product);
    }

    /**
     * HTTP PATCH Request to /api/products
     * Updates a product by its ID.
     *
     * Query Parameters:
     * - id: The ID of the product to update.
     * - name (optional): The new name of the product.
     * - description (optional): The new description of the product.
     * - price (optional): The new price of the product.
     * - size (optional): The new size of the product.
     * - color (optional): The new color of the product.
     * - material (optional): The new material of the product.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void updateProduct(Context ctx) {
        handleSecurity(ctx);

        long id = Utils.getIdFromParam(ctx);
        JSONObject body = new JSONObject(ctx.body());

        String previousName = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT name FROM products WHERE id = :id")
                        .bind("id", id)
                        .mapTo(String.class)
                        .one()
        );

        String name = body.optString("name", null);
        String description = body.optString("description", null);
        Long price = body.has("price") ? body.getLong("price") : null;
        String size = body.optString("size", null);
        String color = body.optString("color", null);
        String material = body.optString("material", null);
        String isVirtualParam = body.optString("virtual", null);
        String isListedParam = body.optString("listed", null);
        boolean virtual = isVirtualParam != null && isVirtualParam.equals("true");
        boolean listed = isListedParam != null && isListedParam.equals("true");

        // Always update the updated_at timestamp
        Timestamp now = new Timestamp(System.currentTimeMillis());

        StringBuilder sql = new StringBuilder("UPDATE products SET updated_at = :updated_at");

        if (name != null) {
            sql.append(", name = :name");
        }
        if (description != null) {
            sql.append(", description = :description");
        }
        if (price != null) {
            sql.append(", price = :price");
        }
        if (size != null) {
            sql.append(", size = :size");
        }
        if (color != null) {
            sql.append(", color = :color");
        }
        if (material != null) {
            sql.append(", material = :material");
        }
        if (isVirtualParam != null) {
            sql.append(", virtual = :virtual");
        }
        if (isListedParam != null) {
            sql.append(", listed = :listed");
        }
        sql.append(" WHERE id = :id");

        Product product = Database.getJdbi().withHandle(handle -> {
            var update = handle.createUpdate(sql.toString())
                    .bind("id", id)
                    .bind("updated_at", now);

            if (name != null) update.bind("name", name);
            if (description != null) update.bind("description", description);
            if (price != null) update.bind("price", price);
            if (size != null) update.bind("size", size);
            if (color != null) update.bind("color", color);
            if (material != null) update.bind("material", material);
            if (isVirtualParam != null) update.bind("virtual", virtual);
            if (isListedParam != null) update.bind("listed", listed);

            update.execute();
            return handle.createQuery("""
                SELECT id, name, description, price, size, color, material, virtual, listed, created_at, updated_at 
                FROM products 
                WHERE id = :id
                """)
                    .bind("id", id)
                    .mapTo(Product.class)
                    .one();
        });

        if (name != null && !name.equals(previousName)) {
            try {
                ProductImages.renameFolder(previousName, name);
                ctx.status(201).json(new JSONObject().put("message", "Image folder renamed successfully."));
            } catch (IOException e) {
                ctx.status(500).json(new JSONObject().put("error", "Failed to rename folder: " + e.getMessage()));
            }
        }

        ctx.json(product);
    }

    /**
     * HTTP GET Request to /api/products
     * Retrieves a list of all products.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getProducts(Context ctx) {
        List<Product> products = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                SELECT id, name, description, price, size, color, material, virtual, listed
                FROM products
                """)
                .mapTo(Product.class)
                .list());

        ctx.json(products);
    }

    /**
     * HTTP GET Request to /api/products/{id}
     * Retrieves a product by its ID.
     *
     * Path Parameters:
     * - id: The ID of the product to retrieve.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getProduct(Context ctx) {
        long id = Utils.getIdFromPath(ctx);

        Product product = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                SELECT id, name, description, price, size, color, material, virtual, listed
                FROM products
                WHERE id = :id
                """)
                .bind("id", id)
                .mapTo(Product.class)
                .findOne()
                .orElse(null)
        );

        if (product == null) {
            ctx.status(404).result("Product not found");
        } else {
            ctx.json(product);
        }
    }

    /**
     * HTTP DELETE Request to /api/products
     * Deletes a product by its ID.
     *
     * Query Parameters:
     * - id: The ID of the product to delete.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void deleteProduct(Context ctx) {
        handleSecurity(ctx);

        long id = Utils.getIdFromParam(ctx);

        String productName = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT name FROM products WHERE id = :id")
                        .bind("id", id)
                        .mapTo(String.class)
                        .one()
        );

        Database.getJdbi().useTransaction(handle -> {
            handle.createUpdate("""
                DELETE FROM stocks
                WHERE product_id = :product_id
                """)
                    .bind("product_id", id)
                    .execute();

            handle.createUpdate("""
                DELETE FROM products
                WHERE id = :id
                """)
                    .bind("id", id)
                    .execute();
        });

        boolean hasOtherProducts = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT COUNT(*) FROM products WHERE name = :name")
                        .bind("name", productName)
                        .mapTo(Integer.class)
                        .one() > 0
        );

        if (!hasOtherProducts) {
            try {
                ProductImages.deleteAllImages(productName);
            } catch (IOException e) {
                ctx.status(500).result("Failed to delete product images: " + e.getMessage());
            }
        }
    }

    public static void uploadImage(Context ctx) {
        handleSecurity(ctx);

        String productName = ctx.pathParam("name");
        try {
            UploadedFile file = ctx.uploadedFile("image");
            if (file == null) {
                ctx.status(400).json(new JSONObject().put("error", "No image uploaded."));
                return;
            }

            byte[] imageData = file.content().readAllBytes();
            ProductImages.uploadImage(productName, imageData);

            ctx.status(201).json(new JSONObject().put("message", "Image uploaded successfully."));
        } catch (IOException e) {
            ctx.status(500).json(new JSONObject().put("error", "Failed to upload image: " + e.getMessage()));
        } finally {
            ctx.req().removeAttribute("org.eclipse.jetty.multipartConfig");
        }
    }

    public static void deleteImage(Context ctx) {
        handleSecurity(ctx);

        String productName = ctx.pathParam("name");
        int imageIndex;
        try {
            imageIndex = Integer.parseInt(ctx.queryParam("index"));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid image index.");
            return;
        }

        try {
            ProductImages.deleteImage(productName, imageIndex);
            ctx.status(200).result("Image deleted successfully.");
        } catch (IOException e) {
            ctx.status(500).result("Failed to delete image: " + e.getMessage());
        }
    }

    public static void getImages(Context ctx) {
        String productName = ctx.pathParam("name");
        String indexParam = ctx.queryParam("index");

        if (indexParam != null) {
            try {
                int index = Integer.parseInt(indexParam);
                String imageUrl = ProductImages.getImage(productName, index);
                ctx.json(imageUrl);
            } catch (NumberFormatException e) {
                ctx.status(400).result("Invalid image index.");
            } catch (IOException e) {
                ctx.status(500).result("Failed to retrieve image: " + e.getMessage());
            }
        } else {
            try {
                List<String> imageUrls = ProductImages.getImages(productName);
                ctx.json(imageUrls);
            } catch (IOException e) {
                ctx.status(500).result("Failed to retrieve images: " + e.getMessage());
            }
        }
    }

    public static void swapImage(Context ctx) {
        handleSecurity(ctx);

        String productName = ctx.pathParam("name");
        int index1, index2;
        try {
            index1 = Integer.parseInt(ctx.queryParam("i"));
            index2 = Integer.parseInt(ctx.queryParam("j"));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid image indices.");
            return;
        }

        try {
            ProductImages.swap(productName, index1, index2);
            ctx.status(200).result("Images swapped successfully.");
        } catch (IOException e) {
            ctx.status(500).result("Failed to swap images: " + e.getMessage());
        }
    }
}