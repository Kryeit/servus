package com.kryeit.merch;

import com.kryeit.Database;
import com.kryeit.auth.Jwt;
import com.kryeit.utils.Utils;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.jdbi.v3.core.generic.GenericType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public class OrderApi {

    /**
     * HTTP GET Request to /api/orders
     * Retrieves a list of all orders.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getOrders(Context ctx) {
        List<Order> orders = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                SELECT *
                FROM orders
                """)
                .mapTo(Order.class)
                .list());

        ctx.json(orders);
    }

    /**
     * HTTP GET Request to /api/orders/{id}
     * Retrieves an order by its ID.
     *
     * Path Parameters:
     * - id: The ID of the order to retrieve.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getOrder(Context ctx) {
        long id = Utils.getIdFromPath(ctx);

        Order order = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                SELECT id, uuid, cart, destination, phone, email, status, transaction
                FROM orders
                WHERE id = :id
                """)
                .bind("id", id)
                .mapTo(Order.class)
                .first());

        ctx.json(order);
    }

    public static void createOrder(Context ctx) {
        String token = ctx.cookie("auth");
        if (token == null) {
            throw new UnauthorizedResponse("Auth token is missing");
        }

        UUID userUuid = Jwt.validateToken(token);
        if (userUuid == null) {
            throw new UnauthorizedResponse("Invalid auth token");
        }

        JSONObject body = new JSONObject(ctx.body());

        String uuid = userUuid.toString();
        String cart = body.getString("cart");
        String destination = body.getString("destination");
        String phone = body.getString("phone");
        String email = body.getString("email");
        String transaction = body.getString("transaction");
        Timestamp now = new Timestamp(System.currentTimeMillis());

        List<Long> productIds = new JSONArray(cart).toList().stream()
                .map(Object::toString)
                .map(Long::valueOf)
                .toList();

        // Check stock availability
        boolean outOfStock = Database.getJdbi().withHandle(handle -> {
            for (Long productId : productIds) {
                Integer stock = handle.createQuery("""
            SELECT stock
            FROM products
            WHERE id = :id
            """)
                        .bind("id", productId)
                        .mapTo(Integer.class)
                        .one();
                if (stock == null || stock <= 0) {
                    return true;
                }
            }
            return false;
        });

        if (outOfStock) {
            ctx.status(400).result("One or more products are out of stock");
            return;
        }

        // Update stock
        Database.getJdbi().useHandle(handle -> {
            for (Long productId : productIds) {
                handle.createUpdate("""
            UPDATE products
            SET stock = stock - 1
            WHERE id = :id AND stock > 0
            """)
                        .bind("id", productId)
                        .execute();
            }
        });

        // Create order
        Order order = Database.getJdbi().withHandle(handle -> handle.createUpdate("""
    INSERT INTO orders (uuid, cart, destination, phone, email, status, transaction, created_at)
    VALUES (:uuid, :cart, :destination, :phone, :email, :status, :transaction, :createdAt)
    """)
                .bind("uuid", uuid)
                .bind("cart", cart)
                .bind("destination", destination)
                .bind("phone", phone)
                .bind("email", email)
                .bind("status", Order.Status.UNPAID)
                .bind("transaction", transaction)
                .bind("createdAt", now)
                .executeAndReturnGeneratedKeys("id", "uuid", "cart", "destination", "phone", "email", "status", "transaction", "created_at")
                .mapTo(Order.class)
                .one());

        ctx.json(order);
    }

    /**
     * HTTP DELETE Request to /api/orders
     * Deletes an order by its ID.
     *
     * Query Parameters:
     * - id: The ID of the order to delete.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void deleteOrder(Context ctx) {
        long id = Utils.getIdFromParam(ctx);

        Database.getJdbi().useHandle(handle -> handle.createUpdate("""
                DELETE FROM orders
                WHERE id = :id
                """)
                .bind("id", id)
                .execute());
    }

    /**
     * HTTP PATCH Request to /api/orders
     * Updates an order by its ID.
     *
     * Query Parameters:
     * - id: The ID of the order to update.
     * - uuid (optional): The new UUID of the user.
     * - cart (optional): The new cart details of the order.
     * - destination (optional): The new destination address of the order.
     * - phone (optional): The new phone number associated with the order.
     * - status (optional): The new status of the order.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void updateOrder(Context ctx) {
        long id = Utils.getIdFromParam(ctx);

        String uuid = ctx.queryParam("uuid");
        String cart = ctx.queryParam("cart");
        String destination = ctx.queryParam("destination");
        String phone = ctx.queryParam("phone");
        String email = ctx.queryParam("email");
        String status = ctx.queryParam("status");
        String transaction = ctx.queryParam("transaction");

        StringBuilder sql = new StringBuilder("UPDATE orders SET ");
        boolean first = true;

        if (uuid != null) {
            sql.append("uuid = :uuid");
            first = false;
        }
        if (cart != null) {
            if (!first) sql.append(", ");
            sql.append("cart = :cart");
            first = false;
        }
        if (destination != null) {
            if (!first) sql.append(", ");
            sql.append("destination = :destination");
            first = false;
        }
        if (phone != null) {
            if (!first) sql.append(", ");
            sql.append("phone = :phone");
            first = false;
        }
        if (email != null) {
            if (!first) sql.append(", ");
            sql.append("email = :email");
            first = false;
        }
        if (status != null) {
            if (!first) sql.append(", ");
            sql.append("status = :status");
            first = false;
        }
        if (transaction != null) {
            if (!first) sql.append(", ");
            sql.append("transaction = :transaction");
        }
        sql.append(" WHERE id = :id");

        Order order = Database.getJdbi().withHandle(handle -> {
            var update = handle.createUpdate(sql.toString()).bind("id", id);
            if (uuid != null) update.bind("uuid", uuid);
            if (cart != null) update.bind("cart", cart);
            if (destination != null) update.bind("destination", destination);
            if (phone != null) update.bind("phone", phone);
            if (email != null) update.bind("email", email);
            if (status != null) update.bind("status", Order.Status.valueOf(status));
            if (transaction != null) update.bind("transaction", transaction);
            update.execute();
            return handle.createQuery("""
            SELECT id, uuid, cart, destination, phone, email, status, transaction
            FROM orders
            WHERE id = :id
            """)
                    .bind("id", id)
                    .mapTo(Order.class)
                    .one();
        });

        ctx.json(order);
    }

    public static void getUserOrders(Context ctx) {
        String token = ctx.cookie("auth");

        if (token == null) {
            throw new UnauthorizedResponse("Auth token is missing");
        }

        UUID uuid = Jwt.validateToken(token);
        if (uuid == null) {
            throw new UnauthorizedResponse("Invalid auth token");
        }

        List<Order> orders = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                SELECT id, uuid, cart, destination, phone, email, status, transaction
                FROM orders
                WHERE uuid = :uuid
                """)
                .bind("uuid", uuid)
                .mapTo(Order.class)
                .list());

        ctx.json(orders);
    }

    public static void createTest(Context ctx) {
        UUID uuid = UUID.fromString("c637c833-8821-42a8-9c0f-28a3c62d4017");
        List<Long> cart = List.of(208L, 208L, 209L);
        String email = "kryeit.minecraft@gmail.com";
        String destination = "Sringfield";
        String phone = "+34 1234567890";

        Order order = Database.getJdbi().withHandle(handle -> handle.createUpdate("""
            INSERT INTO orders (uuid, cart, destination, phone, status, transaction)
            VALUES (:uuid, :cart, :destination, :phone, :status, :transaction)
            """)
                .bind("uuid", uuid)
                .bindByType("cart", cart, new GenericType<List<Long>>() {})
                .bind("destination", destination)
                .bind("phone", phone)
                .bind("email", email)
                .bind("status", Order.Status.UNPAID)
                .bind("transaction", "123213131231233")
                .executeAndReturnGeneratedKeys("id", "uuid", "cart", "destination", "phone", "email", "status", "transaction")
                .mapTo(Order.class)
                .one());

        ctx.json(order);
    }
}