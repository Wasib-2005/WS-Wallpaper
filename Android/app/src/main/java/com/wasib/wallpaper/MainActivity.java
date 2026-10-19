package com.wasib.wallpaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final String TAG = "WSWallpaper";
    private static final int PICK_WALLPAPER_FOLDER = 40;
    private static final int LIBRARY_BATCH_SIZE = 12;
    private static final int BG = Color.rgb(16, 21, 18);
    private static final int SIDEBAR = Color.rgb(21, 29, 25);
    private static final int CARD = Color.rgb(28, 38, 33);
    private static final int CARD_ALT = Color.rgb(34, 46, 39);
    private static final int ACCENT = Color.rgb(192, 255, 62);
    private static final int CYAN = Color.rgb(34, 211, 238);
    private static final int TEXT = Color.rgb(242, 246, 243);
    private static final int MUTED = Color.rgb(154, 168, 158);
    private static final int BORDER = Color.rgb(42, 57, 48);

    private SharedPreferences prefs;
    private LinearLayout screen;
    private LinearLayout page;
    private String currentPage = "Overview";
    private EditText queryInput;
    private EditText resolutionInput;
    private EditText ratioInput;
    private EditText apiKeyInput;
    private EditText timerInput;
    private EditText keepInput;
    private EditText saveFolderInput;
    private android.widget.Spinner sortingInput;
    private android.widget.Spinner toplistInput;
    private CheckBox[] purityInputs;
    private CheckBox[] categoryInputs;
    private TextView statusLabel;
    private TextView rotationStatusLabel;
    private Button rotationToggleButton;
    private Button libraryMoreButton;
    private List<WallpaperStore.Item> libraryImages = new ArrayList<>();
    private int libraryShown;
    private final ExecutorService thumbnailExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(WallpaperStore.PREFS, MODE_PRIVATE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        render("Overview");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (page != null && currentPage.equals("Overview")) {
            renderOverview();
        }
    }

    @Override
    protected void onDestroy() {
        thumbnailExecutor.shutdownNow();
        super.onDestroy();
    }

    private void render(String name) {
        currentPage = name;
        screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(BG);
        ViewCompat.setOnApplyWindowInsetsListener(screen, (view, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(14), dp(20), dp(10));
        header.setBackgroundColor(BG);
        ImageView icon = new ImageView(this);
        icon.setImageResource(getResources().getIdentifier("ws_wallpaper_icon", "drawable", getPackageName()));
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        header.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(10), 0, 0, 0);
        TextView brandName = text("WS WALLPAPER", ACCENT, 16, true);
        TextView subtitle = text("YOUR WALLPAPER, YOUR STYLE", CYAN, 12, true);
        brand.addView(brandName);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        statusLabel = text("Ready", MUTED, 12, true);
        header.addView(statusLabel);
        screen.addView(header);

        TextView title = text(name, TEXT, 24, true);
        title.setPadding(dp(22), dp(10), dp(22), dp(10));
        screen.addView(title);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(8), dp(18), dp(20));
        scroll.addView(page);
        screen.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        screen.addView(buildNavigation());
        setContentView(screen);

        switch (name) {
            case "Overview":
                renderOverview();
                break;
            case "Library":
                renderLibrary();
                break;
            case "Preferences":
                renderPreferences();
                break;
            default:
                renderActivity();
                break;
        }
    }

    private View buildNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setBackgroundColor(SIDEBAR);
        nav.setMinimumHeight(dp(76));
        String[] names = {"Overview", "Library", "Preferences", "Activity"};
        String[] icons = {"⌂", "▧", "⚙", "≡"};
        for (int index = 0; index < names.length; index++) {
            final String name = names[index];
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(8), dp(4), dp(8));
            item.setMinimumHeight(dp(68));
            item.setClickable(true);
            item.setFocusable(true);
            TextView glyph = text(icons[index], name.equals(currentPage) ? ACCENT : MUTED, 23, true);
            glyph.setGravity(Gravity.CENTER);
            TextView label = text(name, name.equals(currentPage) ? ACCENT : MUTED, 12, true);
            label.setGravity(Gravity.CENTER);
            item.addView(glyph);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
            labelParams.topMargin = dp(2);
            item.addView(label, labelParams);
            item.setOnClickListener(view -> render(name));
            nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
        return nav;
    }

    private void renderOverview() {
        page.removeAllViews();
        LinearLayout hero = card(page);
        sectionLabel(hero, "YOUR WALLPAPER, YOUR WAY", CYAN);
        hero.addView(text("Refresh your\nview.", TEXT, 27, true));
        hero.addView(text("Discover beautiful wallpapers from Wallhaven.", MUTED, 14, false));
        addButton(hero, "Change wallpaper now", ACCENT, this::requestWallpaper);
        addButton(hero, "Manage rotation", CARD_ALT, () -> render("Preferences"));

        LinearLayout library = card(page);
        sectionLabel(library, "YOUR LIBRARY", CYAN);
        int count = 0;
        try {
            count = WallpaperStore.list(this).size();
        } catch (Exception error) {
            toast(error.getMessage());
        }
        library.addView(text(count + (count == 1 ? " saved wallpaper" : " saved wallpapers"),
                TEXT, 18, true));
        library.addView(text("Saved on this device", MUTED, 14, false));
        addButton(library, "Open library", CARD_ALT, () -> render("Library"));

        LinearLayout rotation = card(page);
        sectionLabel(rotation, "AUTOMATIC ROTATION", CYAN);
        rotationStatusLabel = text(rotationStatus(), TEXT, 18, true);
        rotation.addView(rotationStatusLabel);
        rotationToggleButton = addButton(rotation, rotationActionLabel(),
                rotationActionColor(), this::toggleRotation);
        List<String> logs = activityLines();
        if (!logs.isEmpty()) {
            LinearLayout recent = card(page);
            sectionLabel(recent, "LATEST ACTIVITY", CYAN);
            recent.addView(text(logs.get(logs.size() - 1), MUTED, 14, false));
        }
    }

    private void renderPreferences() {
        LinearLayout discovery = card(page);
        sectionLabel(discovery, "DISCOVERY", CYAN);
        discovery.addView(text("Search Wallhaven and choose your image quality.", MUTED, 14, false));
        queryInput = edit(discovery, "Search terms", prefs.getString("query", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        resolutionInput = edit(discovery, "Minimum resolution", prefs.getString("resolution", "1920x1080"),
                InputType.TYPE_CLASS_TEXT);
        ratioInput = edit(discovery, "Aspect ratios, comma separated", prefs.getString("ratio", "16x9,16x10,21x9"),
                InputType.TYPE_CLASS_TEXT);
        sortingInput = spinner(discovery, "Sort by",
                new String[]{"random", "date_added", "relevance", "views", "favorites", "toplist", "hot"},
                prefs.getString("sort", "random"));
        toplistInput = spinner(discovery, "Toplist range",
                new String[]{"1d", "3d", "1w", "1M", "3M", "6M", "1y"},
                prefs.getString("top_range", "1M"));

        LinearLayout filters = card(page);
        sectionLabel(filters, "CONTENT FILTERS", CYAN);
        purityInputs = checkboxGroup(filters, "Content", new String[]{"SFW", "Sketchy", "NSFW"},
                prefs.getString("purity", "100"));
        categoryInputs = checkboxGroup(filters, "Categories", new String[]{"General", "Anime", "People"},
                prefs.getString("categories", "111"));
        apiKeyInput = edit(filters, "Wallhaven API key (required for NSFW)", prefs.getString("api_key", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout storage = card(page);
        sectionLabel(storage, "STORAGE", CYAN);
        Uri selectedTree = WallpaperStore.selectedTree(this);
        String folder = selectedTree == null
                ? "App pictures folder (tap to choose a shared folder)"
                : DocumentFile.fromTreeUri(this, selectedTree).getName();
        saveFolderInput = edit(storage, "Wallpaper folder", folder, InputType.TYPE_CLASS_TEXT);
        saveFolderInput.setFocusable(false);
        saveFolderInput.setOnClickListener(view -> chooseWallpaperFolder());
        addButton(storage, "Choose folder", CARD_ALT, this::chooseWallpaperFolder);

        LinearLayout rotation = card(page);
        sectionLabel(rotation, "AUTOMATIC ROTATION", CYAN);
        rotation.addView(text("Android requires at least 15 minutes between background refreshes. "
                + "Battery settings may delay them further.", MUTED, 14, false));
        timerInput = edit(rotation, "Change every (HH:MM:SS)",
                formatClock(Math.round(prefs.getFloat("timer", 30f) * 60)),
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        keepInput = edit(rotation, "Keep downloaded images",
                Integer.toString(prefs.getInt("keep", 5)), InputType.TYPE_CLASS_NUMBER);

        LinearLayout actions = card(page);
        sectionLabel(actions, "ACTIONS", CYAN);
        addButton(actions, "Save preferences", ACCENT, () -> savePreferences());
        rotationToggleButton = addButton(actions, rotationActionLabel(),
                rotationActionColor(), this::toggleRotation);
    }

    private void renderLibrary() {
        libraryMoreButton = null;
        LinearLayout top = card(page);
        sectionLabel(top, "SAVED WALLPAPERS", CYAN);
        top.addView(text("Preview, open, or delete wallpapers on this device.", MUTED, 14, false));
        try {
            libraryImages = WallpaperStore.list(this);
        } catch (Exception error) {
            String message = error.getMessage();
            top.addView(text(message == null ? "Could not read the selected wallpaper folder." : message,
                    MUTED, 14, false));
            return;
        }
        if (libraryImages.isEmpty()) {
            top.addView(text("Your collection is empty. Change wallpaper to get started.",
                    MUTED, 15, false));
            return;
        }
        libraryShown = 0;
        appendLibraryItems();
        libraryMoreButton = addButton(page, "", CARD_ALT, this::appendLibraryItems);
        updateLibraryMoreButton();
    }

    private void appendLibraryItems() {
        if (libraryMoreButton != null && libraryMoreButton.getParent() == page) {
            page.removeView(libraryMoreButton);
        }
        int end = Math.min(libraryShown + LIBRARY_BATCH_SIZE, libraryImages.size());
        for (; libraryShown < end; libraryShown++) {
            WallpaperStore.Item item = libraryImages.get(libraryShown);
            LinearLayout row = horizontal(page);
            row.setBackground(roundRect(CARD, dp(14), BORDER));
            row.setPadding(dp(10), dp(10), dp(10), dp(10));
            ImageView thumbnail = new ImageView(this);
            thumbnail.setBackgroundColor(CARD_ALT);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setImageResource(android.R.drawable.ic_menu_report_image);
            thumbnail.setContentDescription("Preview of " + item.name);
            thumbnail.setTag(item.uri);
            loadThumbnail(thumbnail, item);
            row.addView(thumbnail, new LinearLayout.LayoutParams(dp(88), dp(88)));
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setPadding(dp(10), 0, dp(6), 0);
            TextView filename = text(item.name, TEXT, 14, true);
            filename.setMaxLines(2);
            filename.setEllipsize(android.text.TextUtils.TruncateAt.END);
            details.addView(filename);
            String modified = item.file == null ? "" : "\n" + formatBytes(item.file.length());
            details.addView(text("Saved wallpaper" + modified, MUTED, 12, false));
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.VERTICAL);
            actions.addView(smallButton("Open", CYAN, () -> openImage(item)));
            actions.addView(smallButton("Delete", TEXT, () -> confirmDelete(item)));
            row.addView(actions);
            LinearLayout.LayoutParams rowParams = (LinearLayout.LayoutParams) row.getLayoutParams();
            rowParams.bottomMargin = dp(9);
            row.setLayoutParams(rowParams);
        }
        if (libraryMoreButton != null) {
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, -2);
            buttonParams.topMargin = dp(10);
            page.addView(libraryMoreButton, buttonParams);
        }
        updateLibraryMoreButton();
    }

    private void updateLibraryMoreButton() {
        if (libraryMoreButton == null) {
            return;
        }
        int remaining = libraryImages.size() - libraryShown;
        libraryMoreButton.setText("Show more (" + remaining + " remaining)");
        libraryMoreButton.setVisibility(remaining > 0 ? View.VISIBLE : View.GONE);
    }

    private void renderActivity() {
        LinearLayout actions = card(page);
        sectionLabel(actions, "ACTIVITY LOG", CYAN);
        actions.addView(text("Recent wallpaper downloads and service events.", MUTED, 14, false));
        addButton(actions, "Clear activity", CARD_ALT, this::clearActivity);
        List<String> lines = activityLines();
        if (lines.isEmpty()) {
            actions.addView(text("No activity yet.", MUTED, 14, false));
            return;
        }
        for (int index = lines.size() - 1; index >= 0; index--) {
            TextView line = text(lines.get(index), TEXT, 14, false);
            line.setPadding(0, dp(7), 0, dp(7));
            actions.addView(line);
            if (index > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(BORDER);
                actions.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
    }

    private boolean savePreferences() {
        try {
            long seconds = parseClock(timerInput.getText().toString());
            if (seconds < RotationScheduler.MIN_INTERVAL_MINUTES * 60) {
                throw new IllegalArgumentException("Android background rotation requires at least 00:15:00.");
            }
            int keep = Integer.parseInt(keepInput.getText().toString().trim());
            if (keep < 0) {
                throw new IllegalArgumentException("Image count cannot be negative.");
            }
            String purity = bitmask(purityInputs);
            String categories = bitmask(categoryInputs);
            if ("000".equals(purity) || "000".equals(categories)) {
                throw new IllegalArgumentException("Select at least one content filter and one category.");
            }
            prefs.edit()
                    .putString("query", queryInput.getText().toString().trim())
                    .putString("resolution", resolutionInput.getText().toString().trim())
                    .putString("ratio", ratioInput.getText().toString().trim())
                    .putString("sort", sortingInput.getSelectedItem().toString())
                    .putString("top_range", toplistInput.getSelectedItem().toString())
                    .putString("purity", purity)
                    .putString("categories", categories)
                    .putString("api_key", apiKeyInput.getText().toString().trim())
                    .putFloat("timer", seconds / 60f)
                    .putInt("keep", keep)
                    .putBoolean("saved", true)
                    .apply();
            updateStatus("Preferences saved");
            toast("Preferences saved");
            render("Preferences");
            return true;
        } catch (IllegalArgumentException error) {
            new AlertDialog.Builder(this)
                    .setTitle("Check your preferences")
                    .setMessage(error.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
            return false;
        }
    }

    private void startRotation() {
        if (currentPage.equals("Preferences") && !savePreferences()) {
            return;
        }
        if (!prefs.getBoolean("saved", false)) {
            render("Preferences");
            toast("Save your preferences before starting rotation.");
            return;
        }
        long seconds = Math.round(prefs.getFloat("timer", 30f) * 60);
        if (seconds < RotationScheduler.MIN_INTERVAL_MINUTES * 60) {
            toast("Android background rotation requires at least 15 minutes.");
            return;
        }
        if (prefs.getBoolean("rotation_enabled", false) && currentPage.equals("Overview")) {
            toast("Wallpaper rotation is already running.");
            return;
        }
        RotationScheduler.start(this, seconds);
        prefs.edit().putBoolean("rotation_enabled", true).apply();
        WallpaperWorker.log(this, "Automatic rotation scheduled.");
        updateStatus("Rotation scheduled");
        updateRotationStatus();
        updateRotationToggle();
        toast("Wallpaper rotation scheduled");
    }

    private void stopRotation() {
        if (!prefs.getBoolean("rotation_enabled", false)) {
            toast("Wallpaper rotation is already stopped.");
            return;
        }
        RotationScheduler.stop(this);
        prefs.edit().putBoolean("rotation_enabled", false).apply();
        WallpaperWorker.log(this, "Automatic rotation stopped.");
        updateStatus("Rotation stopped");
        updateRotationStatus();
        updateRotationToggle();
        toast("Wallpaper rotation stopped");
    }

    private void toggleRotation() {
        if (prefs.getBoolean("rotation_enabled", false)) {
            stopRotation();
        } else {
            startRotation();
        }
    }

    private void requestWallpaper() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WallpaperWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(
                "ws-wallpaper-now", ExistingWorkPolicy.REPLACE, request);
        WallpaperWorker.log(this, "Wallpaper refresh requested.");
        updateStatus("Finding wallpaper…");
        toast("Finding and applying a wallpaper…");
    }

    private void chooseWallpaperFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_WALLPAPER_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_WALLPAPER_FOLDER || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri tree = data.getData();
        if (tree == null) {
            return;
        }
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(tree, flags);
            DocumentFile folder = DocumentFile.fromTreeUri(this, tree);
            if (folder == null || !folder.canWrite()) {
                throw new IllegalArgumentException("Choose a folder that allows writing files.");
            }
            prefs.edit().putString(WallpaperStore.SAVE_TREE, tree.toString()).apply();
            updateStatus("Wallpaper folder selected");
            toast("Wallpaper folder selected");
            render("Preferences");
        } catch (SecurityException | IllegalArgumentException error) {
            toast(error.getMessage() == null ? "Could not access that folder." : error.getMessage());
        }
    }

    private void openImage(WallpaperStore.Item item) {
        try {
            Uri uri = item.shareUri(this);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, item.mimeType());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception error) {
            toast("Could not open image: " + error.getMessage());
        }
    }

    private void confirmDelete(WallpaperStore.Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete wallpaper?")
                .setMessage("Delete " + item.name + " from your library? This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        WallpaperStore.delete(this, item);
                        WallpaperWorker.log(this, "Deleted wallpaper: " + item.name);
                        updateStatus("Wallpaper deleted");
                        render("Library");
                    } catch (Exception error) {
                        toast("Could not delete wallpaper: " + error.getMessage());
                    }
                })
                .show();
    }

    private void clearActivity() {
        new AlertDialog.Builder(this)
                .setTitle("Clear activity?")
                .setMessage("Delete all saved activity log entries? This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (dialog, which) -> {
                    prefs.edit().remove("activity").apply();
                    updateStatus("Activity cleared");
                    render("Activity");
                })
                .show();
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(15), dp(17), dp(15));
        card.setBackground(roundRect(CARD, dp(17), BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(13);
        parent.addView(card, params);
        return card;
    }

    private LinearLayout horizontal(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private void sectionLabel(LinearLayout parent, String label, int color) {
        TextView heading = text(label, color, 12, true);
        heading.setLetterSpacing(0.08f);
        heading.setPadding(0, 0, 0, dp(9));
        parent.addView(heading);
    }

    private TextView text(String value, int color, float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        view.setLineSpacing(dp(2), 1f);
        return view;
    }

    private EditText edit(LinearLayout parent, String label, String value, int inputType) {
        TextView heading = text(label, MUTED, 13, false);
        heading.setPadding(0, dp(10), 0, dp(5));
        parent.addView(heading);
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setText(value);
        field.setTextColor(TEXT);
        field.setHintTextColor(MUTED);
        field.setTextSize(16);
        field.setInputType(inputType);
        field.setPadding(dp(12), dp(9), dp(12), dp(9));
        field.setBackground(roundRect(CARD_ALT, dp(10), BORDER));
        parent.addView(field, new LinearLayout.LayoutParams(-1, -2));
        return field;
    }

    private android.widget.Spinner spinner(LinearLayout parent, String label, String[] values, String selected) {
        TextView heading = text(label, MUTED, 13, false);
        heading.setPadding(0, dp(10), 0, dp(5));
        parent.addView(heading);
        android.widget.Spinner spinner = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(selected)) {
                spinner.setSelection(index);
                break;
            }
        }
        spinner.setBackground(roundRect(CARD_ALT, dp(10), BORDER));
        parent.addView(spinner, new LinearLayout.LayoutParams(-1, dp(52)));
        return spinner;
    }

    private CheckBox[] checkboxGroup(LinearLayout parent, String title, String[] options, String bitmask) {
        parent.addView(text(title, MUTED, 13, false));
        CheckBox[] checks = new CheckBox[options.length];
        String bits = bitmask == null ? "" : bitmask;
        for (int index = 0; index < options.length; index++) {
            CheckBox check = new CheckBox(this);
            check.setText(options[index]);
            check.setTextColor(TEXT);
            check.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));
            check.setTextSize(15);
            check.setMinHeight(dp(44));
            check.setChecked(index < bits.length() && bits.charAt(index) == '1');
            parent.addView(check);
            checks[index] = check;
        }
        return checks;
    }

    private Button addButton(LinearLayout parent, String label, int color, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(color == ACCENT ? BG : TEXT);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(roundRect(color, dp(12), color == CARD_ALT ? BORDER : color));
        button.setMinHeight(dp(48));
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setOnClickListener(view -> action.run());
        boolean horizontal = parent.getOrientation() == LinearLayout.HORIZONTAL;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                horizontal ? 0 : -1, -2, horizontal ? 1 : 0);
        if (horizontal) {
            params.setMarginEnd(dp(6));
        } else {
            params.topMargin = dp(10);
        }
        parent.addView(button, params);
        return button;
    }

    private Button smallButton(String label, int color, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(color);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(dp(44));
        button.setMinimumHeight(dp(44));
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(roundRect(CARD_ALT, dp(10), BORDER));
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private android.graphics.drawable.Drawable roundRect(int fill, int radius, int stroke) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private void loadThumbnail(ImageView thumbnail, WallpaperStore.Item item) {
        int targetSize = dp(88);
        thumbnailExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {
                bitmap = decodeThumbnail(item.uri, targetSize);
            } catch (IOException | RuntimeException error) {
                Log.w(TAG, "Could not decode library thumbnail: " + item.name, error);
            } catch (OutOfMemoryError error) {
                Log.e(TAG, "Not enough memory to decode library thumbnail: " + item.name, error);
            }
            Bitmap result = bitmap;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed() && item.uri.equals(thumbnail.getTag())
                        && result != null) {
                    thumbnail.setImageBitmap(result);
                }
            });
        });
    }

    private Bitmap decodeThumbnail(Uri uri, int targetSize) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = WallpaperStore.open(this, uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        int sampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sampleSize > targetSize) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inTempStorage = new byte[16 * 1024];
        try (InputStream input = WallpaperStore.open(this, uri)) {
            return BitmapFactory.decodeStream(input, null, options);
        }
    }

    private String bitmask(CheckBox[] checks) {
        StringBuilder result = new StringBuilder();
        for (CheckBox check : checks) {
            result.append(check.isChecked() ? '1' : '0');
        }
        return result.toString();
    }

    private List<String> activityLines() {
        String content = prefs.getString("activity", "");
        return content.isEmpty() ? java.util.Collections.emptyList()
                : java.util.Arrays.asList(content.split("\n"));
    }

    private void updateStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    private String rotationStatus() {
        if (!prefs.getBoolean("rotation_enabled", false)) {
            return "Rotation is off";
        }
        long minutes = Math.round(prefs.getFloat("timer", 30f));
        return "Scheduled · about every " + minutes + " minutes";
    }

    private void updateRotationStatus() {
        if (rotationStatusLabel != null && currentPage.equals("Overview")) {
            rotationStatusLabel.setText(rotationStatus());
        }
    }

    private String rotationActionLabel() {
        return prefs.getBoolean("rotation_enabled", false) ? "Stop rotation" : "Start rotation";
    }

    private int rotationActionColor() {
        return prefs.getBoolean("rotation_enabled", false) ? CARD_ALT : ACCENT;
    }

    private void updateRotationToggle() {
        if (rotationToggleButton != null) {
            boolean enabled = prefs.getBoolean("rotation_enabled", false);
            rotationToggleButton.setText(enabled ? "Stop rotation" : "Start rotation");
            rotationToggleButton.setTextColor(enabled ? TEXT : BG);
            rotationToggleButton.setBackground(roundRect(
                    enabled ? CARD_ALT : ACCENT, dp(12), enabled ? BORDER : ACCENT));
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String formatClock(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainder = seconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, remainder);
    }

    private long parseClock(String value) {
        String[] parts = value.trim().split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Enter rotation time as HH:MM:SS.");
        }
        try {
            long hours = Long.parseLong(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            if (hours < 0 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
                throw new IllegalArgumentException("Minutes and seconds must be between 00 and 59.");
            }
            long total = hours * 3600 + minutes * 60L + seconds;
            if (total <= 0) {
                throw new IllegalArgumentException("Rotation interval must be greater than zero.");
            }
            return total;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter rotation time as HH:MM:SS.");
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024f * 1024f));
        }
        return String.format(Locale.ROOT, "%.0f KB", bytes / 1024f);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
