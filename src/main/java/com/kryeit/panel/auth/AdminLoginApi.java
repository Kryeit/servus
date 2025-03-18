package com.kryeit.panel.auth;

import com.kryeit.Database;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

public class AdminLoginApi {

    /**
     * HTTP POST Request to /api/login
     * Logs in an admin user.
     *
     * JSON Body Parameters:
     * - username: The username of the admin user.
     * - password: The password of the admin user.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void login(Context ctx) {
        JSONObject body = new JSONObject(ctx.body());

        String username = body.getString("username");
        String password = body.getString("password");

        String storedPasswordHash = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT password FROM admins WHERE username = :username")
                        .bind("username", username)
                        .mapTo(String.class)
                        .findOne()
                        .orElse(null)
        );

        if (storedPasswordHash != null && BCrypt.checkpw(password, storedPasswordHash)) {
            long adminId = Database.getJdbi().withHandle(handle ->
                    handle.createQuery("SELECT id FROM admins WHERE username = :username")
                            .bind("username", username)
                            .mapTo(Long.class)
                            .findOne()
                            .orElse(-1L)
            );

            if (adminId == -1) {
                ctx.status(401).result("Invalid username or password.");
                return;
            }

            String token = AdminJwt.generateToken(adminId);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", username);
            ctx.status(200).json(response);
        } else {
            ctx.status(401).result("Invalid username or password.");
        }
    }

    /**
     * HTTP POST Request to /api/register
     * Registers a new admin user.
     *
     * JSON Body Parameters:
     * - username: The username of the admin user.
     * - password: The password of the admin user.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void register(Context ctx) {
        JSONObject body = new JSONObject(ctx.body());

        String username = body.getString("username");
        String password = body.getString("password");

        long adminCount = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT COUNT(*) FROM admins")
                        .mapTo(Long.class)
                        .one()
        );

        if (adminCount > 0) {
            ctx.status(404);
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        Database.getJdbi().useHandle(handle ->
                handle.createUpdate("INSERT INTO admins (username, password) VALUES (:username, :password)")
                        .bind("username", username)
                        .bind("password", hashedPassword)
                        .execute()
        );

        ctx.status(201).result("Admin registered successfully.");
    }

    /**
     * HTTP GET Request to /api/admin/validate
     * Validates the user's token and returns the user information.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void validateToken(Context ctx) {
        String token = ctx.cookie("auth");
        if (token == null) {
            throw new UnauthorizedResponse();
        }

        long id = AdminJwt.validateToken(token);

        if (id == -1) {
            throw new UnauthorizedResponse();
        }

        Admin.Data admin = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT id, username
                        FROM admins
                        WHERE id = :id
                        """)
                .bind("id", id)
                .mapTo(Admin.Data.class)
                .one());

        ctx.json(Map.of(
                "id", id,
                "username", admin.username()
        ));
    }
}