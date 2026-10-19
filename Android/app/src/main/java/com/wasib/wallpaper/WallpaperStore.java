package com.wasib.wallpaper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

final class WallpaperStore {
    static final String PREFS = "ws_wallpaper";
    static final String SAVE_TREE = "save_tree_uri";
    private static final String DEFAULT_DIR_NAME = "WS Wallpaper";
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"};

    static final class Item {
        final String name;
        final Uri uri;
        final File file;

        Item(String name, Uri uri, File file) {
            this.name = name;
            this.uri = uri;
            this.file = file;
        }

        String mimeType() {
            String extension = name.lastIndexOf('.') >= 0
                    ? name.substring(name.lastIndexOf('.') + 1).toLowerCase()
                    : "jpg";
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            return type == null ? "image/*" : type;
        }

        Uri shareUri(Context context) throws IOException {
            if (file != null) {
                return androidx.core.content.FileProvider.getUriForFile(
                        context, context.getPackageName() + ".files", file);
            }
            return uri;
        }
    }

    private WallpaperStore() {
    }

    static Uri selectedTree(Context context) {
        String raw = preferences(context).getString(SAVE_TREE, null);
        return raw == null ? null : Uri.parse(raw);
    }

    static List<Item> list(Context context) throws IOException {
        List<Item> items = new ArrayList<>();
        Uri tree = selectedTree(context);
        if (tree != null) {
            DocumentFile root = DocumentFile.fromTreeUri(context, tree);
            if (root == null || !root.canRead()) {
                throw new IOException("The selected wallpaper folder is no longer available.");
            }
            for (DocumentFile child : root.listFiles()) {
                if (child.isFile() && isImage(child.getName())) {
                    items.add(new Item(child.getName(), child.getUri(), null));
                }
            }
        } else {
            File folder = defaultDirectory(context);
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && isImage(file.getName())) {
                        items.add(new Item(file.getName(), Uri.fromFile(file), file));
                    }
                }
            }
        }
        items.sort(Comparator.comparingLong((Item item) -> modified(context, item)).reversed());
        return items;
    }

    static File defaultDirectory(Context context) throws IOException {
        File externalPictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File folder = new File(externalPictures != null ? externalPictures : context.getFilesDir(),
                DEFAULT_DIR_NAME);
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("Could not create wallpaper folder: " + folder);
        }
        return folder;
    }

    static OutputStream create(Context context, String name, String mimeType) throws IOException {
        Uri tree = selectedTree(context);
        if (tree != null) {
            DocumentFile root = DocumentFile.fromTreeUri(context, tree);
            if (root == null || !root.canWrite()) {
                throw new IOException("The selected wallpaper folder is not writable.");
            }
            DocumentFile existing = root.findFile(name);
            if (existing != null && !existing.delete()) {
                throw new IOException("Could not replace the existing wallpaper file.");
            }
            DocumentFile file = root.createFile(mimeType, name);
            if (file == null) {
                throw new IOException("Could not create the wallpaper file in the selected folder.");
            }
            OutputStream stream = context.getContentResolver().openOutputStream(file.getUri(), "w");
            if (stream == null) {
                file.delete();
                throw new IOException("Could not open the wallpaper file for writing.");
            }
            return stream;
        }
        return new java.io.FileOutputStream(new File(defaultDirectory(context), name));
    }

    static Uri findUri(Context context, String name) throws IOException {
        Uri tree = selectedTree(context);
        if (tree != null) {
            DocumentFile root = DocumentFile.fromTreeUri(context, tree);
            if (root != null) {
                DocumentFile file = root.findFile(name);
                return file == null ? null : file.getUri();
            }
        } else {
            File file = new File(defaultDirectory(context), name);
            return file.isFile() ? Uri.fromFile(file) : null;
        }
        return null;
    }

    static void prune(Context context, int keep) throws IOException {
        if (keep <= 0) {
            return;
        }
        List<Item> downloaded = new ArrayList<>();
        for (Item item : list(context)) {
            if (item.name.startsWith("wallhaven-")) {
                downloaded.add(item);
            }
        }
        for (int index = keep; index < downloaded.size(); index++) {
            delete(context, downloaded.get(index));
            WallpaperWorker.log(context, "Removed old wallpaper: " + downloaded.get(index).name);
        }
    }

    static void delete(Context context, Item item) throws IOException {
        if (item.file != null) {
            if (!item.file.delete()) {
                throw new IOException("Could not delete " + item.file);
            }
            return;
        }
        DocumentFile document = DocumentFile.fromSingleUri(context, item.uri);
        if (document == null || !document.delete()) {
            throw new IOException("Could not delete " + item.name);
        }
    }

    static InputStream open(Context context, Uri uri) throws IOException {
        InputStream stream = context.getContentResolver().openInputStream(uri);
        if (stream == null) {
            throw new IOException("Could not open wallpaper image.");
        }
        return stream;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static boolean isImage(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return Arrays.stream(IMAGE_EXTENSIONS).anyMatch(lower::endsWith);
    }

    private static long modified(Context context, Item item) {
        if (item.file != null) {
            return item.file.lastModified();
        }
        DocumentFile document = DocumentFile.fromSingleUri(context, item.uri);
        return document == null ? 0 : document.lastModified();
    }
}
