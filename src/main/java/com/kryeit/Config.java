package com.kryeit;

public class Config {
    public static final int apiPort = 6969;
    public static final boolean production = false;

    public static final String dbUrl = production
            ? "jdbc:postgresql://kryeit.com:5432/servus"
            : "jdbc:postgresql://localhost:5432/servus";

    public static final String dbUser = production ?
            System.getenv("DB_USER")
            : "postgres";
    public static final String dbPassword = production ?
            System.getenv("DB_PASSWORD")
            : "lel";

    public static final String jwtSecret = production ?
            System.getenv("JWT_SECRET")
            : "b536ae347eed450cadafc820d7446b2addd29daf30bac13deb71eddab6c63c199e4cc50ff0f74d18d2cec5cd101814b9a174ec682ec01c30e484bfddfaca3e71db2be8c419dd8b2b9bf77d7b70b4ecc3634dc91da6e91d6cb113d69c6921a54afbfb5d5b87018a4401ddec3962dc1f02f6664b9b6156f962b1e7a8032cc7af0c50371f5594f5099b6734d803201dd97f049c787ea64b0e947be395b6f54b5537e8fd96a2e063513c24eccc2eaff1dfe72446dbfd11228d237f8e2669489af486b09eee317fb64362657c573acd908d0296f1718d9dc13ba046ab84861f93cc565738e86e3dab6a335a93fc67a306cbb3aed94bd90d70ff74d2e63ed477e2c552";

    public static final String adminJwtSecret = production
            ? System.getenv("ADMIN_JWT_SECRET")
            : "6a1cdb53cb9c87ba8e4c0cc8eef463e30bcb3991c8a6d7553880173b8bc0671504cd1f72592cc524003a7f05fc08a4ac03d8383f4c8a1ddec007aec9c80865ccfbf93c773c4d034d303b5461fd5cb61d946544dc8f494e76fad78dabfc790b4bb45d53d04babd0d85096ddd681446c9ff7f10f29c8d8f7e7c3e500c85bed13c04b839a2943aee1bfbbb3a92333372ef48bcfca42b1e386e48acbe2cd0bd7cb5d329fd9135452789b8a75602e9f27fccffcbff11c18d5ea1388b9ab3a027042ea84325f001b2f659da36e865614b804d879f69cdba49b31b2b6813c1a69256f11cf80504544ee76fa74b70b966b2e70cd71d2217c76aeb08870b8b43e2bbcc36b";

    public static final String encryptionPassword = System.getenv("ENCRYPTION_PASSWORD");

    public static final String stripeApiKey = production ?
            System.getenv("STRIPE_API_KEY")
            : "";

    public static final String stripeEntrypointSecret = production ?
            System.getenv("STRIPE_ENTRYPOINT_SECRET")
            : "";

    public static final String cosmeticApiSecret = System.getenv("COSMETIC_API_SECRET");

    public static final String authApiSecret = System.getenv("AUTH_API_SECRET");

    public static final String FRONTEND_DOMAIN = Config.production ?
            "https://kryeit.com" :
            "http://localhost:5173";
}
