package com.kryeit;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.InternalServerErrorResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ModPackApi {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void getModPack(Context ctx) throws IOException, InterruptedException {
        HttpRequest versionsRequest = HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/duaqEXgz/version")).build();

        HttpResponse<String> versionsResponse = HTTP_CLIENT.send(versionsRequest, HttpResponse.BodyHandlers.ofString());
        JSONArray jsonArray = new JSONArray(versionsResponse.body());
        String packUrl = jsonArray.getJSONObject(0).getJSONArray("files").getJSONObject(0).getString("url");

        HttpRequest packRequest = HttpRequest.newBuilder(URI.create(packUrl)).build();
        HttpResponse<InputStream> packResponse = HTTP_CLIENT.send(packRequest, HttpResponse.BodyHandlers.ofInputStream());

        ZipInputStream zipInputStream = new ZipInputStream(packResponse.body());
        ctx.contentType(ContentType.APPLICATION_ZIP);
        ctx.header(Header.CONTENT_DISPOSITION, "attachment; filename=mods.zip");
        ZipOutputStream zipOutputStream = new ZipOutputStream(ctx.outputStream());

        JSONObject index = null;
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            String name = zipEntry.getName();
            if (name.equals("modrinth.index.json")) {
                index = new JSONObject(new String(zipInputStream.readAllBytes()));
            } else {
                int nameStart = name.lastIndexOf('/');
                if (nameStart > 0 && name.substring(0, nameStart).equals("overrides/mods")) {
                    zipOutputStream.putNextEntry(new ZipEntry(name.substring(nameStart + 1)));
                    zipInputStream.transferTo(zipOutputStream);
                }
            }
            zipEntry = zipInputStream.getNextEntry();
        }

        if (index == null) throw new InternalServerErrorResponse();

        for (Object fileObject : index.getJSONArray("files")) {
            JSONObject file = (JSONObject) fileObject;

            String path = file.getString("path");
            int nameStart = path.lastIndexOf('/');
            if (nameStart > 0 && path.substring(0, nameStart).equals("mods")) {
                zipOutputStream.putNextEntry(new ZipEntry(path.substring(nameStart + 1)));
                String downloadUrl = file.getJSONArray("downloads").getString(0);
                HttpRequest downloadRequest = HttpRequest.newBuilder(URI.create(downloadUrl)).build();
                HttpResponse<InputStream> response = HTTP_CLIENT.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    body.transferTo(zipOutputStream);
                }
            }
        }

        zipOutputStream.close();
    }
}
