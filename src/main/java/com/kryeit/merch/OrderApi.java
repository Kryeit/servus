package com.kryeit.merch;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.kryeit.Database;
import com.kryeit.auth.Jwt;
import com.kryeit.panel.auth.Admin;
import com.kryeit.utils.Utils;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrderApi {

    public static void getOrders(Context ctx) {
        Admin admin = Admin.fromRequest(ctx).orElseThrow(UnauthorizedResponse::new);
        if (!admin.username().equals("MuriPlz")) {
            throw new UnauthorizedResponse();
        }

        List<Order> orders = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT * FROM orders")
                        .mapTo(Order.class)
                        .list()
        );

        ctx.json(orders);
    }

    public static void deleteOrder(Context ctx) {
        Admin admin = Admin.fromRequest(ctx).orElseThrow(UnauthorizedResponse::new);

        if (!admin.username().equals("MuriPlz")) {
            throw new UnauthorizedResponse();
        }
        long id = Utils.getIdFromParam(ctx);

        Database.getJdbi().useHandle(handle ->
                handle.createUpdate("DELETE FROM orders WHERE id = :id")
                        .bind("id", id)
                        .execute()
        );
    }

    public static void getOrder(Context ctx) {
        Admin admin = Admin.fromRequest(ctx).orElseThrow(UnauthorizedResponse::new);

        if (!admin.username().equals("MuriPlz")) {
            throw new UnauthorizedResponse();
        }

        long id = Utils.getIdFromPath(ctx);

        Order order = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT * FROM orders WHERE id = :id")
                        .bind("id", id)
                        .mapTo(Order.class)
                        .first()
        );

        ctx.json(order);
    }

    public static void updateStatus(Context ctx) {
        Admin admin = Admin.fromRequest(ctx).orElseThrow(UnauthorizedResponse::new);

        if (!admin.username().equals("MuriPlz")) {
            throw new UnauthorizedResponse();
        }
        long id = Utils.getIdFromParam(ctx);
        Order.Status status = Order.Status.valueOf(new JSONObject(ctx.body()).getString("status"));

        Database.getJdbi().useHandle(handle ->
                handle.createUpdate("""
                UPDATE orders
                SET status = :status, edition = NOW()
                WHERE id = :id
                """)
                        .bind("status", status.toString())
                        .bind("id", id)
                        .execute()
        );
    }

    public static void getUserOrders(Context ctx) {
        String token = ctx.cookie("auth");
        if (token == null) {
            throw new UnauthorizedResponse("Auth token is missing");
        }

        UUID uuid;
        try {
            uuid = Jwt.validateToken(token);
        } catch (JWTVerificationException e) {
            throw new UnauthorizedResponse();
        }

        List<Order> orders = Database.getJdbi().withHandle(handle -> {
            if (uuid == null) {
                return new ArrayList<>();
            }

            return handle.createQuery("SELECT * FROM orders WHERE uuid = :uuid")
                    .bind("uuid", uuid)
                    .mapTo(Order.class)
                    .list();
        });

        ctx.json(orders);
    }
}