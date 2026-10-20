# WS Wallpaper: Wallhaven Wallpaper Downloader and Manager

<p align="center">
  <img src="Linux/ws-wallpaper.png" alt="WS Wallpaper app icon" width="160">
</p>

WS Wallpaper is a free, open-source wallpaper manager for Linux, Windows, and Android. Search Wallhaven or Pixabay, download and organize wallpapers, apply them to your desktop or phone, and automate wallpaper changes with built-in rotation controls.

The project includes a desktop GUI and command-line wallpaper tool for Linux and Windows, plus a native Android app. The CLI also supports macOS. Build installers and apps from source; prebuilt APK, EXE, `.deb`, and `.rpm` packages are not included in this repository.

The current desktop app version is **1.0.0.2**. The app checks GitHub for newer
releases at startup and provides a manual **Check for updates** action. A new
version opens its GitHub release page; updates are not installed automatically.
Pushing a `v*` tag builds and publishes Windows installer and portable assets
through GitHub Actions.

## ✨ Features

- **Wallhaven Search and Filtering:** Search by keywords and filter by resolution, aspect ratio, purity (SFW/Sketchy/NSFW), and categories.
- **Pixabay Source:** Optionally search safe, landscape photos with your Pixabay API key.
- **Desktop GUI:** Browse your collection, tune search preferences, review activity, and control rotation on Linux and Windows.
- **Wallpaper Library:** Preview thumbnails, open images in your desktop viewer, and delete saved wallpapers.
- **Automatic Wallpaper Rotation:** Run the background service on a customizable timer, or use Android WorkManager to schedule wallpaper changes.
- **Immediate Refresh:** Fetch and apply another wallpaper without waiting for the timer.
- **Persistent Configuration:** Save search, filtering, storage, and rotation settings.
- **Version Checks:** Check GitHub releases at startup or manually from the app.
- **Automatic Cleanup:** Remove old downloads automatically to save disk space.
- **Smart Logging:** Keep service logs with configurable log rotation.
- **Command-Line Tool:** Download and apply Wallhaven or Pixabay wallpapers from Linux and Windows; the CLI also supports macOS.

## Project Structure

- `Linux/` contains the Linux installer, desktop GUI, wallpaper CLI, and app icon.
- `Windows/` contains Windows build scripts, a startup helper, and installer packaging.
- `Android/` contains the native Android app and WorkManager-based wallpaper rotation.
- `install.sh` and `uninstall.sh` are the top-level Linux installation scripts.

---

## 📁 Repository Layout

```text
Wallpaper Manager/
├── README.md
├── install.sh
├── uninstall.sh
├── Linux/
│   ├── setup.sh
│   ├── wallpaper
│   ├── wallpaper-manager
│   └── ws-wallpaper.png
├── Windows/
│   ├── build.ps1
│   ├── cli_entry.py
│   ├── gui_entry.py
│   ├── make_icon.py
│   └── startup.py
├── Android/
│   ├── app/
│   ├── build.gradle
│   ├── gradlew
│   └── settings.gradle
└── .github/
```

> Generated build artifacts and local runtime files are intentionally excluded from Git via `.gitignore`.

## 🚀 Installation (Linux)

This project does not ship prebuilt APK, EXE, or Linux `.deb`/`.rpm` packages. Build your own from source using the scripts in this repository.

The installer adds **WS Wallpaper** (with its custom icon) to your applications menu and installs the CLI and background service for your user. Administrator authorization is only needed if it has to install missing system dependencies.

1. Download or clone the repository and open a terminal in its folder:
   ```bash
   git clone https://github.com/Wasib-2005/WS-Wallpaper
   cd WS-Wallpaper
   ```
2. Run the installer:
   ```bash
   chmod +x install.sh
   ./install.sh
   ```

The installer detects Debian/Ubuntu, Fedora/RHEL, Arch, and openSUSE. If Python 3, Tkinter, or Pillow with Tk support is missing, it installs them using the system package manager and asks for administrator authorization (`sudo`) when required.

3. Launch **WS Wallpaper** from your applications menu, or run:
   ```bash
   ~/.local/bin/ws-wallpaper
   ```

The installer also provides a `wallpaper` command-line tool. In Preferences, choose a folder for downloaded wallpapers and set the rotation interval in `HH:MM:SS` format (for example, `00:30:00`). New downloads, the Library, cleanup, and offline fallback use the selected folder; existing images are not moved. Pillow provides the in-app wallpaper thumbnails and preview; the installer checks that this dependency is available after installation.

To uninstall:
```bash
./uninstall.sh
```

The CLI can also be used directly as described below.

## Windows

Use the Windows build script to generate a per-user installer or portable package.
The resulting setup installs WS Wallpaper to the Start menu, can add a desktop
shortcut, and can start wallpaper rotation when you sign in. No administrator
access is required. The installer includes the GUI, CLI, and background startup
helper; Python does not need to be installed on the target PC. Uninstall it from
Windows Settings or Control Panel. Your settings, logs, and downloaded wallpapers
are kept.

To build the Windows installer yourself, use a Windows PC with Python 3 and
Inno Setup 6 installed. From PowerShell at the repository root, run:

```powershell
.\Windows\build.ps1
```

This creates `dist\WS-Wallpaper-Setup.exe` and
`dist\WS-Wallpaper-Windows-portable.zip`. To build just the portable package
without Inno Setup, run `.\Windows\build.ps1 -PortableOnly`. Both packages
contain standalone executables built from the same GUI and wallpaper CLI source
used by Linux. Windows user data is stored in `%APPDATA%\Wallhaven`; wallpapers
default to the user's `Pictures\Wallhaven` folder.

The Windows build workflow can also be run from the repository's GitHub Actions
tab; it publishes the installer and portable ZIP as workflow artifacts.

## Android

The native Android app is in `Android/`. It requires JDK 17 and Android SDK
platform 36. To build a debug APK:

```bash
cd Android
./gradlew assembleDebug
```

The APK is created at `Android/app/build/outputs/apk/debug/app-debug.apk`.
Install it on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Android schedules automatic wallpaper rotation through WorkManager, so the
system controls the exact execution time and the minimum interval is 15 minutes.

---

## 📖 Usage

You can run the script manually, or set it up to run as a daemon.

### Basic Examples

**Set a random nature wallpaper immediately:**
```bash
wallpaper -Q "nature"
```

**Start the background daemon, changing a Cyberpunk wallpaper every 30 minutes:**
```bash
wallpaper -Q "cyberpunk" -T 30 -d
```

**Exclude anime wallpapers, set resolution to 4K, and run in the background:**
```bash
wallpaper -Q "-anime" -R 3840x2160 -d
```

**Pull from the all-time top-rated wallpapers instead of random:**
```bash
wallpaper -S toplist --top-range 1y
```

**Save settings without running (updates your default config):**
```bash
wallpaper -Q "landscape" -R 2560x1440 --set
```

---

## ⚙️ Service & Management Commands

Once the daemon is running in the background (`-d`), you can control it using these commands:

| Command | Description |
| :--- | :--- |
| `wallpaper --now` | Instantly fetches and applies a new wallpaper. |
| `wallpaper --status` | Checks if the background daemon is currently running. |
| `wallpaper --stop` | Stops the running background daemon. |
| `wallpaper --set` | Saves the passed flags to config without running or starting the daemon. |
| `wallpaper -LOG` | Prints the last 50 lines of the daemon's log. |
| `wallpaper -f` | Live streams the background service logs. |

---

## 🛠️ Configuration Options

In the desktop app's **Preferences**, select **Pixabay** as the wallpaper source
and enter your Pixabay API key. Get a key from
[Pixabay API documentation](https://pixabay.com/api/docs/). Pixabay search
responses are cached for 24 hours, and photo attribution is recorded in
Activity. Wallhaven remains the default source.

| Flag | Name | Description | Example |
| :--- | :--- | :--- | :--- |
| `-Q` | `--query` | Search term(s). Use `-` to exclude (e.g., `-anime`). | `-Q "cars"` |
| `-T` | `--timer` | Refresh timer in minutes. | `-T 15` (15 mins) |
| `-R` | `--resolution`| Minimum resolution. | `-R "1920x1080"` |
| `-A` | `--ratio` | Aspect ratio(s) separated by commas. | `-A "16x9,21x9"` |
| `-P` | `--purity` | Purity bitmask (100=SFW, 010=Sketchy, 001=NSFW). | `-P "110"` |
| `-C` | `--categories`| Category bitmask (100=General, 010=Anime, 001=People). | `-C "100"` |
| `-S` | `--sort` | Sorting method (random, date_added, toplist, etc.). | `-S "random"` |
| | `--top-range` | Timeframe when `--sort toplist` is used (1d, 3d, 1w, 1M, 3M, 6M, 1y). | `--top-range 1M` |
| `-K` | `--key` | Wallhaven API Key (Required for NSFW / 001 content). | `-K "your_api_key"` |
| | `--source` | Wallpaper source (`wallhaven` or `pixabay`). | `--source pixabay` |
| | `--pixabay-key` | Pixabay API key (required for Pixabay). | `--pixabay-key "your_api_key"` |
| `-M` | `--keep` | Max downloaded images to keep in the wallpaper folder. Default: 1. | `-M 5` |
| | `--save-dir` | Folder for downloaded wallpapers (defaults to `~/Pictures/Wallhaven`). | `--save-dir "~/Pictures/My Wallpapers"` |
| `-L` | `--max-log-size`| Maximum log file size limit before rotation. | `-L "5MB"` |

> **Note on Clearing Configs:** If you want to reset a saved setting back to default, pass `none` or an empty string. Example: `wallpaper -K none` will remove your saved API key. This also applies to numeric flags like `-M`/`--keep`: passing `-M 0` with `--set` won't persist as "unlimited" — it clears the setting back to the default (`1`) instead. `-M 0` still works for a single one-off run without `--set`.

---

## 📂 File Locations

- **CLI:** `~/.local/bin/wallpaper`
- **GUI:** `~/.local/bin/ws-wallpaper`
- **Config & Logs:** `~/.config/wallhaven/`
  - `config.json` (Saved settings)
  - `wallpaper.log` (Service logs)
- **Wallpapers:** `~/Pictures/Wallhaven/`
- **Application menu:** `~/.local/share/applications/ws-wallpaper.desktop`
- **Application icon:** `~/.local/share/icons/hicolor/512x512/apps/ws-wallpaper.png`
- **Autostart Entry:** `~/.config/autostart/wallhaven-daemon.desktop`

On Windows, the installer places the GUI, CLI, and startup helper under
`%LOCALAPPDATA%\Programs\WS Wallpaper`, and user settings/logs under
`%APPDATA%\Wallhaven`.

---

## 🗑️ Uninstallation

If you wish to remove the app, script, and autostart entry, run:

```bash
./uninstall.sh
```
