package com.kryeit;

import com.kryeit.auth.LoginApi;
import com.kryeit.cosmetics.CosmeticApi;
import com.kryeit.merch.OrderApi;
import com.kryeit.merch.ProductApi;
import com.kryeit.merch.ProductStructureApi;
import com.kryeit.merch.StockApi;
import com.kryeit.panel.auth.AdminLoginApi;
import com.kryeit.stripe.PaymentApi;
import com.kryeit.stripe.PaymentHandler;
import com.stripe.Stripe;
import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import io.javalin.http.Context;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.UUID;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Main {
    public static void main(String[] args) {

        SslPlugin sslPlugin = new SslPlugin(sslConfig -> {
            sslConfig.http2 = true;
            sslConfig.secure = false;
            sslConfig.insecurePort = Config.apiPort;
        });

        Stripe.apiKey = Config.stripeApiKey;

        Javalin app = Javalin.create(config -> {
            config.registerPlugin(sslPlugin);

            config.validation.register(LocalDate.class, s -> {
                String[] split = s.split("-");
                int year = Integer.parseInt(split[0]);
                int month = Integer.parseInt(split[1]);
                int day = Integer.parseInt(split[2]);
                return LocalDate.of(year, month, day);
            });

            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowCredentials = true;
                    it.allowHost("https://servus.kryeit.com", "http://localhost:5173", "http://localhost:5174");
                });
            });

            config.validation.register(JSONObject.class, JSONObject::new);
            config.validation.register(UUID.class, UUID::fromString);

            config.showJavalinBanner = false;

            config.router.apiBuilder(() -> {

                path("api", () -> {
                    path("images", () -> {
                        path("products", () -> {
                            path("{name}", () -> {
                                get(ProductApi::getImages);

                                post(ProductApi::uploadImage);
                                delete(ProductApi::deleteImage);
                                patch(ProductApi::swapImage);
                            });
                        });
                    });

                    path("admin", () -> {
                        post("login", AdminLoginApi::login);
                        post("register", AdminLoginApi::register);
                    });

                    path("login", () -> {
                        get("validate-login", LoginApi::validateLogin);
                        get("validate", LoginApi::validateToken);

                        get("link", LoginApi::generateLink);
                    });

                    path("account", () -> {
                        get(LoginApi::getAccount);
                        post("logout", LoginApi::logout);
                    });
                    path("stock", () -> {
                        get("by-name", StockApi::getStocksByName);
                        get(StockApi::getStocks);
                        get("{id}", StockApi::getStock);

                        patch(StockApi::updateStock);
                    });

                    path("products", () -> {
                        // Add new comprehensive endpoints
                        get("catalog", ProductStructureApi::getProductCatalog);
                        get("details", ProductStructureApi::getProductDetails);

                        path("{id}", () -> {
                            get(ProductApi::getProduct);
                        });

                        get(ProductApi::getProducts);

                        post(ProductApi::createProduct);
                        delete(ProductApi::deleteProduct);
                        patch(ProductApi::updateProduct);
                    });

                    path("orders", () -> {
                        get("by-user", OrderApi::getUserOrders);

                        get("{id}", OrderApi::getOrder);
                        get(OrderApi::getOrders);

                        post("test", OrderApi::createTest);
                        post(OrderApi::createOrder);
                        delete(OrderApi::deleteOrder);
                        patch(OrderApi::updateOrder);
                    });

                    path("payment", () -> {
                        post("create", PaymentApi::createPaymentIntent);
                        post("webhook", PaymentHandler::handleStripeWebhook);
                    });

                    path("assets", () -> {
                        get("cursor", AssetsAPI::getCursorResources);
                    });

                    path("modpack", () -> {
                        get(ModPackApi::getModPack);
                    });

                    path("cosmetics", () -> {
                        before(CosmeticApi::validateRequest);
                        get("cosmetic/{id}", CosmeticApi::getCosmeticData);
                        path("player/{player}", () -> {
                            post("equip", CosmeticApi::equipCosmetic);
                            post("unequip", CosmeticApi::unequipCosmetic);
                            get("wardrobe", CosmeticApi::getWardrobe);
                        });
                        get("equipped", CosmeticApi::getEquippedCosmetics);
                    });
                });
            });
        }).start();

        app.get("/api/products/images/{productName}/{index}", Main::serveImage);
    }

    public static void serveImage(Context ctx) {
        String productName = ctx.pathParam("productName");
        String imageName = ctx.pathParam("index");
        File imageFile = new File("db/product_images/" + productName + "/" + imageName);

        if (imageFile.exists()) {
            try {
                ctx.result(Files.readAllBytes(imageFile.toPath())).contentType("image/webp");
            } catch (IOException e) {
                ctx.status(500).result("Failed to read image file");
            }
        } else {
            ctx.status(404).result("Image not found");
        }
    }
}