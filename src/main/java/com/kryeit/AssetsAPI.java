package com.kryeit;

import io.javalin.http.Context;

import java.io.InputStream;

public class AssetsAPI {

    public static void getCursorResources(Context ctx) {
        InputStream inputStream = AssetsAPI.class.getResourceAsStream("/assets/cursor.rar");

        if (inputStream == null) {
            ctx.status(404).result("File not found");
            return;
        }

        ctx.contentType("application/octet-stream");
        ctx.header("Content-Disposition", "attachment; filename=cursor.rar");

        ctx.result(inputStream);
    }


}
