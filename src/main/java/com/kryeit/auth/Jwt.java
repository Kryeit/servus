package com.kryeit.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.kryeit.Config;
import io.javalin.http.UnauthorizedResponse;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

public class Jwt {

    public static final Algorithm algorithm = Algorithm.HMAC256(Config.jwtSecret);
    private static final long EXPIRATION = Duration.ofDays(30).toMillis();

    public static String generateToken(UUID uuid) {
        Date expirationDate = new Date(System.currentTimeMillis() + EXPIRATION);
        JWTCreator.Builder token = JWT.create()
                .withClaim("uuid", uuid.toString())
                .withExpiresAt(expirationDate);

        return token.sign(algorithm);
    }

    public static UUID validateToken(String token) throws JWTVerificationException {
        if (token == null) return null;

        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT jwt = verifier.verify(token);

        if (jwt.getClaim("uuid").asString() == null) {
            return null;
        }

        if (jwt.getExpiresAt().before(new Date())) {
            throw new UnauthorizedResponse("Token has expired");
        }

        return UUID.fromString(jwt.getClaim("uuid").asString());
    }
}