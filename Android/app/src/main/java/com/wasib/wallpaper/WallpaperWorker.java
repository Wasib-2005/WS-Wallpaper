package com.wasib.wallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public final class WallpaperWorker extends Worker {
    private static final String TAG = "WSWallpaper";
    private static final String API = "https://wallhaven.cc/api/v1/search";

    public WallpaperWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            updateWallpaper(getApplicationContext());
            return Result.success();
        } catch (IOException | RuntimeException error) {
            Log.e(TAG, "Wallpaper refresh failed", error);
            log(getApplicationContext(), "Wallpaper refresh failed: " + error.getMessage());
            return Result.retry();
        }
    }

    static void updateWallpaper(Context context) throws IOException {
        SharedPreferences settings = context.getSharedPreferences(WallpaperStore.PREFS, Context.MODE_PRIVATE);
        JSONObject wallpaper;
        String imageUrl;
        String id;
        try {
            wallpaper = fetch(settings);
            imageUrl = wallpaper.getString("path");
            id = wallpaper.getString("id");
        } catch (org.json.JSONException error) {
            throw new IOException("Wallhaven returned incomplete wallpaper details.", error);
        }
        String extension = imageUrl.substring(imageUrl.lastIndexOf('.') + 1).replaceAll("[^A-Za-z0-9].*$", "");
        if (extension.isEmpty()) {
            extension = "jpg";
        }
        String filename = "wallhaven-" + id + "." + extension;
        String mimeType = extension.equalsIgnoreCase("png") ? "image/png"
                : extension.equalsIgnoreCase("webp") ? "image/webp" : "image/jpeg";
        Uri imageUri;

        HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "WS-Wallpaper-Android/1.0");
        try {
            int response = connection.getResponseCode();
            if (response < 200 || response >= 300) {
                throw new IOException("Wallpaper download returned HTTP " + response + ".");
            }
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 OutputStream output = new BufferedOutputStream(WallpaperStore.create(context, filename, mimeType))) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
            }
        } finally {
            connection.disconnect();
        }

        imageUri = WallpaperStore.findUri(context, filename);
        if (imageUri == null) {
            throw new IOException("Downloaded wallpaper was not found in the selected folder.");
        }
        WallpaperManager manager = WallpaperManager.getInstance(context);
        try (InputStream input = WallpaperStore.open(context, imageUri)) {
            manager.setStream(input);
        }
        int keep = Math.max(0, settings.getInt("keep", 5));
        WallpaperStore.prune(context, keep);
        log(context, "Wallpaper applied: " + filename);
    }

    private static JSONObject fetch(SharedPreferences settings) throws IOException {
        try {
            StringBuilder url = new StringBuilder(API).append('?')
                    .append("categories=").append(encode(settings.getString("categories", "111")))
                    .append("&purity=").append(encode(settings.getString("purity", "100")))
                    .append("&sorting=").append(encode(settings.getString("sort", "random")))
                    .append("&order=desc")
                    .append("&atleast=").append(encode(settings.getString("resolution", "1920x1080")))
                    .append("&ratios=").append(encode(settings.getString("ratio", "16x9,16x10,21x9")));
            String query = settings.getString("query", "").trim();
            if (!query.isEmpty()) {
                url.append("&q=").append(encode(query));
            }
            String apiKey = settings.getString("api_key", "").trim();
            if (!apiKey.isEmpty()) {
                url.append("&apikey=").append(encode(apiKey));
            }
            if ("toplist".equals(settings.getString("sort", "random"))) {
                url.append("&topRange=").append(encode(settings.getString("top_range", "1M")));
            }

            HttpURLConnection connection = (HttpURLConnection) new URL(url.toString()).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "WS-Wallpaper-Android/1.0");
            try {
                int response = connection.getResponseCode();
                if (response < 200 || response >= 300) {
                    throw new IOException("Wallhaven API returned HTTP " + response + ".");
                }
                JSONObject result;
                try (InputStream input = connection.getInputStream()) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] bytes = new byte[8192];
                    int count;
                    while ((count = input.read(bytes)) != -1) {
                        buffer.write(bytes, 0, count);
                    }
                    result = new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
                }
                JSONArray data = result.optJSONArray("data");
                if (data == null || data.length() == 0) {
                    throw new IOException("No wallpapers match the current filters.");
                }
                int selection = "random".equals(settings.getString("sort", "random"))
                        ? 0 : new Random().nextInt(Math.min(15, data.length()));
                return data.getJSONObject(selection);
            } finally {
                connection.disconnect();
            }
        } catch (org.json.JSONException error) {
            throw new IOException("Could not read the Wallhaven response.", error);
        }
    }

    private static String encode(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    static void log(Context context, String message) {
        SharedPreferences prefs = context.getSharedPreferences(WallpaperStore.PREFS, Context.MODE_PRIVATE);
        String old = prefs.getString("activity", "");
        String line = java.time.LocalDateTime.now() + "  " + message;
        String updated = (old.isEmpty() ? "" : old + "\n") + line;
        String[] lines = updated.split("\n");
        if (lines.length > 200) {
            StringBuilder trimmed = new StringBuilder();
            for (int index = lines.length - 200; index < lines.length; index++) {
                if (trimmed.length() > 0) {
                    trimmed.append('\n');
                }
                trimmed.append(lines[index]);
            }
            updated = trimmed.toString();
        }
        prefs.edit().putString("activity", updated).apply();
    }
}
