package com.kryeit.auth;

import com.kryeit.Config;
import com.kryeit.Database;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.util.*;


public class LoginApi {

    /**
     * HTTP POST Request to /api/account/logout
     * Logs out the user by invalidating their tokens.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void logout(Context ctx) {
    }

    public static User getUser(Context ctx) {
        return User.fromRequest(ctx).orElseThrow(UnauthorizedResponse::new);
    }

    /**
     * HTTP GET Request to /api/account
     * Retrieves the account details of the logged-in user.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getAccount(Context ctx) {
        User user = getUser(ctx);
        User.Data data = user.data();
        // No need to convert stats as it's already a JSONObject
        ctx.json(Map.of(
                "uuid", user.uuid(),
                "username", data.username(),
                "creation", data.creation(),
                "last-seen", data.lastSeen(),
                "roles", data.roles(),
                "stats", data.stats()
        ));
    }

    /**
     * Generates an encrypted token for the given username and UUID.
     *
     * @param username the username
     * @param uuid the UUID
     * @return the encrypted token
     */
    public static String getEncryptedToken(String username, UUID uuid) {
        JSONObject body = new JSONObject(Map.of("username", username, "uuid", uuid.toString()));

        try {
            SecretKeySpec secretKey = new SecretKeySpec(Config.encryptionPassword.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedToken = cipher.doFinal(body.toString().getBytes());
            return Base64.getEncoder().encodeToString(encryptedToken);
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting token", e);
        }
    }

    /**
     * HTTP GET Request to /api/login/link
     * Generates a login link with an encrypted token.
     *
     * Body Parameters:
     * - username: The username.
     * - uuid: The UUID.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void generateLink(Context ctx) {
        JSONObject body = new JSONObject(ctx.body());
        String authApiSecret = body.getString("authApiSecret");

        if (!authApiSecret.equals(Config.authApiSecret)) {
            ctx.status(403).result("Forbidden");
            return;
        }

        String username = body.getString("username");
        UUID uuid = UUID.fromString(body.getString("uuid"));

        String token = URLEncoder.encode(getEncryptedToken(username, uuid));
        ctx.json(Map.of("link", "https://kryeit.com/login?t=" + token));
    }

    /**
     * HTTP GET Request to /api/login/validate-login
     * Validates the login token and returns the user information.
     *
     * Query Parameters:
     * - t: The encrypted token.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void validateLogin(Context ctx) {
        String encryptedToken = ctx.queryParam("t");
        if (encryptedToken == null) {
            throw new UnauthorizedResponse("Token is missing");
        }

        String decryptedToken;
        try {
            SecretKeySpec secretKey = new SecretKeySpec(Config.encryptionPassword.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedToken = Base64.getDecoder().decode(encryptedToken);
            decryptedToken = new String(cipher.doFinal(decodedToken));
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting token", e);
        }

        JSONObject body = new JSONObject(decryptedToken);
        UUID uuid = UUID.fromString(body.getString("uuid"));

        User.Data user = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT uuid, username, creation, last_seen, roles, stats
                        FROM users
                        WHERE uuid = :uuid
                        """)
                .bind("uuid", uuid)
                .mapTo(User.Data.class)
                .findOne()
                .orElse(null));

        if (user == null) {
            throw new UnauthorizedResponse("User not found");
        }

        // Stats is already a JSONObject so it can be directly returned
        ctx.json(Map.of(
                "uuid", uuid,
                "username", user.username(),
                "creation", user.creation(),
                "last-seen", user.lastSeen(),
                "roles", user.roles(),
                "stats", user.stats(),
                "token", Jwt.generateToken(uuid)
        ));
    }

    /** HTTP PATCH Request to /api/account/roles
     * Updates the roles of the logged-in user.
     *
     * Body Parameters:
     * - roles: The new roles of the user.
     * - action: The action to perform (add, remove).
     *
     * @param ctx the Javalin HTTP context
     */
    public static void updateRoles(Context ctx) {
        User user = getUser(ctx);
        User.Data data = user.data();
        String action = new JSONObject(ctx.body()).getString("action");
        List<User.Role> roles = new JSONObject(ctx.body()).getJSONArray("roles").toList().stream()
                .map(String::valueOf)
                .map(User.Role::valueOf)
                .toList();

        Set<User.Role> updatedRoles = new HashSet<>(data.roles());

        if ("add".equalsIgnoreCase(action)) {
            updatedRoles.addAll(roles);
        } else if ("remove".equalsIgnoreCase(action)) {
            updatedRoles.removeAll(roles);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }

        Database.getJdbi().useHandle(h -> h.createUpdate("""
        UPDATE users
        SET roles = :roles
        WHERE uuid = :uuid
        """)
                .bind("roles", new ArrayList<>(updatedRoles))
                .bind("uuid", user.uuid())
                .execute());
    }

    /**
     * HTTP GET Request to /api/account/roles
     * Retrieves the roles of the logged-in user.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getRoles(Context ctx) {
        User user = getUser(ctx);
        User.Data data = user.data();
        ctx.json(Map.of("roles", data.roles()));
    }

    /**
     * HTTP GET Request to /api/account/validate
     * Validates the user's token and returns the user information.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void validateToken(Context ctx) {
        String token = ctx.cookie("auth");
        if (token == null) {
            throw new UnauthorizedResponse();
        }

        UUID uuid = Jwt.validateToken(token);

        if (uuid == null) {
            throw new UnauthorizedResponse();
        }

        // Updated to properly handle JSONB column
        User.Data user = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT uuid, username, creation, last_seen, roles, stats
                        FROM users
                        WHERE uuid = :uuid
                        """)
                .bind("uuid", uuid)
                .mapTo(User.Data.class)
                .one());

        // Stats is already a JSONObject so it can be directly returned
        ctx.json(Map.of(
                "uuid", uuid,
                "username", user.username(),
                "creation", user.creation(),
                "last-seen", user.lastSeen(),
                "roles", user.roles(),
                "stats", user.stats()
        ));
    }
}