package com.kryeit.panel.auth;

import com.kryeit.Database;
import com.kryeit.auth.Jwt;
import com.kryeit.auth.User;
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

        Admin user = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT * FROM admins WHERE username = :username")
                        .bind("username", username)
                        .mapTo(Admin.class)
                        .findOne()
                        .orElse(null)
        );

        if (user != null && BCrypt.checkpw(password, user.password())) {
            String token = AdminJwt.generateToken(user.id());
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.username());
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

        Admin user = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT * FROM admins")
                        .mapTo(Admin.class)
                        .findOne()
                        .orElse(null)
        );

        if (user != null) {
            ctx.status(400).result("heh.");
            return;
        }

        try {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            Database.getJdbi().withHandle(handle ->
                    handle.createUpdate("INSERT INTO admins (username, password) VALUES (:username, :password)")
                            .bind("username", username)
                            .bind("password", hashedPassword)
                            .execute()
            );

            ctx.status(200).result("User registered successfully.");

        } catch (Exception e) {
            ctx.status(500).result("Registration failed due to internal error.");
        }
    }

    /**
     * HTTP GET Request to /api/admin/validate
     * Validates the user's token and returns the user information.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void validate(Context ctx) {
        String token = ctx.cookie("auth");

        if (token == null) {
            throw new UnauthorizedResponse();
        }

        long id = AdminJwt.validateToken(token);

        if (id == -1) {
            throw new UnauthorizedResponse();
        }

        Map<String, Object> data = Database.getJdbi().withHandle(handle -> handle.createQuery("""
            SELECT id, username
            FROM admins
            WHERE id = :id
            """)
                .bind("id", id)
                .mapToMap()
                .one());

        ctx.json(Map.of(
                "id", data.get("id"),
                "username", data.get("username")
        ));
    }
}