package com.kryeit;

public class Config {
    public static final int apiPort = 6969;
    public static final boolean production = false;

    public static final String dbUrl = production
            ? "jdbc:postgresql://kryeit.com:5432/servus"
            : "jdbc:postgresql://localhost:5432/postgres";

    public static final String dbUser = production ?
            System.getenv("DB_USER")
            : "postgres";
    public static final String dbPassword = production ?
            System.getenv("DB_PASSWORD")
            : "lel";

    public static final String jwtSecret = System.getenv("JWT_SECRET");

    // Used for decrypting minecraft details
    public static final String encryptionPassword = System.getenv("ENCRYPTION_PASSWORD");

    public static final String stripeApiKey = System.getenv("STRIPE_API_KEY");

    public static final String stripeEntrypointSecret = System.getenv("STRIPE_ENTRYPOINT_SECRET");

    public static final String cosmeticApiSecret = System.getenv("COSMETIC_API_SECRET");

    public static final String authApiSecret = System.getenv("AUTH_API_SECRET");
}
