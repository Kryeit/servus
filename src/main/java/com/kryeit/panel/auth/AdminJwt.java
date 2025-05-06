package com.kryeit.panel.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.kryeit.Config;
import io.javalin.http.UnauthorizedResponse;

import java.time.Duration;
import java.util.Date;

public class AdminJwt {

    public static final Algorithm algorithm = Algorithm.HMAC256(Config.adminJwtSecret);
    private static final long EXPIRATION = Duration.ofDays(30).toMillis();

    public static String generateToken(long adminId) {
        JWTCreator.Builder token = JWT.create()
                .withClaim("admin_id", adminId)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION));

        return token.sign(algorithm);
    }

    public static long validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getClaim("admin_id").asLong();
        } catch (JWTVerificationException e) {
            throw new UnauthorizedResponse("Invalid token");
        }
    }


}