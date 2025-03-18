package com.kryeit.merch;

import com.kryeit.Database;
import com.kryeit.utils.Utils;
import io.javalin.http.Context;
import org.json.JSONObject;

import java.util.List;

import static com.kryeit.merch.ProductApi.handleSecurity;

public class StockApi {

    /**
     * HTTP PATCH Request to /api/stock?id={id}
     * Updates the stock quantity based on an action.
     *
     * JSON Parameters:
     * - id: The ID of the stock to update.
     * - action: The action to perform (add, remove, remove-all).
     * - quantity: The quantity to add or remove.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void updateStock(Context ctx) {
        handleSecurity(ctx);

        long id = Utils.getIdFromParam(ctx);

        JSONObject body = new JSONObject(ctx.body());

        long quantity = body.optLong("quantity");
        double discount = body.optDouble("discount");

        String updateQuery = """
            UPDATE stocks SET
            quantity = :quantity,
            discount = :discount
            WHERE id = :id
            """;

        Stock stock = Database.getJdbi().withHandle(handle -> {
            handle.createUpdate(updateQuery)
                    .bind("quantity", quantity)
                    .bind("discount", discount)
                    .bind("id", id)
                    .execute();
            return handle.createQuery("""
                SELECT id, product_id, quantity, discount
                FROM stocks
                WHERE id = :id
                """)
                    .bind("id", id)
                    .mapTo(Stock.class)
                    .one();
        });

        ctx.json(stock);
    }

    /**
     * Function to set up the stock of a product when created.
     *
     * @param id the ID of the product
     */
    public static void setupStock(long id) {
        Database.getJdbi().useHandle(handle -> handle.createUpdate("""
                INSERT INTO stocks (product_id, quantity, discount)
                VALUES (:product_id, 0, 0)
                """)
                .bind("product_id", id)
                .execute());
    }

    /**
     * HTTP GET Request to /api/stock/{id}
     * Retrieves the stock details by its ID.
     *
     * Path Parameters:
     * - id: The ID of the stock to retrieve.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getStock(Context ctx) {
        String product = ctx.pathParam("id");
        if (product.equals("[object Object]") || product.equals("undefined")) {
            ctx.status(400).result("Invalid product ID");
            return;
        }
        long productId = Long.parseLong(product);

        Stock stock = Database.getJdbi().withHandle(handle -> handle.createQuery("""
        SELECT id, product_id, quantity, discount
        FROM stocks
        WHERE product_id = :product_id
        """)
                .bind("product_id", productId)
                .mapTo(Stock.class)
                .findOne()
                .orElseGet(() -> new Stock(productId, 0, 0, 0)));

        ctx.json(stock);
    }

    /**
     * HTTP GET Request to /api/stock
     * Retrieves a paginated list of stock entries.
     *
     * Query Parameters:
     * - page: The page number to retrieve (default is 1).
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getStocks(Context ctx) {
        List<Stock> stocks = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                SELECT id, product_id, quantity, discount
                FROM stocks
                """)
                .mapTo(Stock.class)
                .list());

        ctx.json(stocks);
    }

    public static void getStocksByName(Context ctx) {
        String name = ctx.queryParam("name");

        if (name == null) {
            ctx.status(400);
            return;
        }

        int stocks = new ProductGroup(name).getStock();
        ctx.json(stocks);
    }
}