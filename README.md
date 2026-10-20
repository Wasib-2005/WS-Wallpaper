# WS Wallpaper

WS Wallpaper helps you find, download, organize, and apply desktop wallpapers.
The desktop app runs on Linux and Windows. A separate Android app is available
for phones and tablets. The desktop command-line tool also supports macOS.

Choose wallpapers from **Wallhaven**, **Pixabay**, or **Both (random)**. In
random mode, the app chooses a provider for each wallpaper change. You can
change one wallpaper on demand or set up automatic rotation.

> **Quick start:** Install the app for your platform, open **Preferences**,
> choose a source, add its API key if needed, set your search and timer, and
> choose **Save preferences**. Use **Change wallpaper** for a one-time change,
> or start rotation for automatic changes.

## What it can do

- Search Wallhaven with resolution, aspect-ratio, category, content, and sort
  filters.
- Search Pixabay for safe, horizontal photos.
- Use either provider or let **Both (random)** pick a provider on each refresh.
- Download wallpapers to a folder you choose, browse the saved library, and
  apply images to your desktop.
- Set an automatic desktop rotation interval; refresh manually at any time.
- Save activity logs and clean up older downloads.
- Check for desktop-app updates from GitHub.

## Choose where to install

| Platform | Start here |
| --- | --- |
| Linux | [Install on Linux](#linux-installation) |
| Windows | [Install on Windows](#windows-installation) |
| Android | [Install or build for Android](#android) |
| macOS | Use the [command-line tool](#command-line-usage); there is no macOS GUI installer. |

## Get an API key

The API key belongs to the provider you select. You can start with Wallhaven's
free search without a key for SFW content; add its key if you need NSFW results.
Pixabay requires its own API key. **Do not share your keys or publish them in
screenshots, source code, or public config files.**

### Wallhaven

1. Sign in to [Wallhaven](https://wallhaven.cc/).
2. Open the [Wallhaven API help page](https://wallhaven.cc/help/api).
3. Generate or copy your API key.
4. In the desktop app, open **Preferences** and enter it in **Wallhaven API
   key**. The key is required for NSFW results.

### Pixabay

1. Create an account or sign in at [Pixabay](https://pixabay.com/).
2. Follow the instructions on the [Pixabay API documentation page](https://pixabay.com/api/docs/)
   to get an API key.
3. In **Preferences**, enter it in **Pixabay API key**.

The Pixabay integration searches for safe, horizontal photos. Search responses
are cached for 24 hours to follow Pixabay's API requirements; the app can still
select a different photo from the cached results on each wallpaper refresh.
Pixabay photo attribution is recorded in Activity.

## Linux installation

The Linux installer adds the app to your applications menu and installs the
desktop app and `wallpaper` command for your user. It supports Debian/Ubuntu,
Fedora/RHEL, Arch, and openSUSE. Python 3, Tkinter, and Pillow with Tk support
are required; the installer checks for them and can ask for administrator
authorization to install missing packages.

1. Clone the repository and enter its directory:

   ```bash
   git clone https://github.com/Wasib-2005/WS-Wallpaper.git
   cd WS-Wallpaper
   ```

2. Run the installer:

   ```bash
   chmod +x install.sh
   ./install.sh
   ```

3. Launch **WS Wallpaper** from your applications menu, or run:

   ```bash
   ~/.local/bin/ws-wallpaper
   ```

### First-time Linux setup

1. Open **Preferences**.
2. Select **Wallhaven**, **Pixabay**, or **Both (random)**.
3. Enter the API key for the selected provider. **Both (random)** requires a
   Pixabay key; the Wallhaven key is optional for SFW searches.
4. Enter search terms and adjust the filters that apply to your selected
   provider.
5. Choose a wallpaper folder and save your preferences.
6. Choose **Change wallpaper** to fetch one now, or **Start rotation** to run
   automatically.

**Both (random)** selects one provider per refresh, not both providers for the
same refresh. Wallhaven-specific resolution, aspect-ratio, category, purity,
and sorting filters do not apply to Pixabay.

### Linux files and config

- App: `~/.local/bin/ws-wallpaper`
- CLI: `~/.local/bin/wallpaper`
- Settings, API keys, cache, and logs: `~/.config/ws-wallpaper/`
- Wallpapers: `~/Pictures/Wallhaven/` by default; the folder is configurable.

If `~/.config/wallhaven/` exists and `~/.config/ws-wallpaper/` does not, the
app moves the old directory on startup to retain settings and runtime files.

To uninstall the app, run `./uninstall.sh` from the repository. The uninstaller
asks whether to remove your settings and logs.

## Windows installation

The current desktop release is **1.0.0.3**. Download
**WS-Wallpaper-Setup.exe** from
[GitHub Releases](https://github.com/Wasib-2005/WS-Wallpaper/releases/latest)
and run it. The per-user installer does not require administrator access.
During setup, you can choose whether to create a desktop shortcut and whether
to start rotation when you sign in. Uninstall it from Windows Settings or
Control Panel.

The release also includes a portable ZIP. Extract it and launch
`ws-wallpaper.exe`.

The desktop app checks for newer releases at startup and has a manual
**Check for updates** action. If an update is available, it opens the release
page; installation is not automatic.

Windows user settings and logs are stored in `%APPDATA%\Wallhaven`; wallpapers
are saved to `Pictures\Wallhaven` by default. Python does not need to be
installed to use the released Windows packages.

### Build Windows packages from source

On a Windows PC, install Python 3. To build the installer, also install Inno
Setup 6. From PowerShell at the repository root, run:

```powershell
.\Windows\build.ps1
```

The build creates `dist\WS-Wallpaper-Setup.exe` and
`dist\WS-Wallpaper-Windows-portable.zip`. To build only the portable ZIP without
Inno Setup, run:

```powershell
.\Windows\build.ps1 -PortableOnly
```

## Android

The repository does not currently include a prebuilt APK. To build a debug APK
yourself, install JDK 17 and Android SDK platform 36, then run:

```bash
cd Android
./gradlew assembleDebug
```

The APK is written to
`Android/app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected
device with Android Debug Bridge:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The Android app currently searches Wallhaven; the desktop Pixabay source
selector is not available on Android. Open **Preferences** to set the Wallhaven
search and API key, then use **Change wallpaper now** or enable automatic
rotation. Android uses WorkManager: the system controls exact execution times,
and background rotation cannot run more frequently than every 15 minutes.

## Use the desktop app

The desktop GUI is the easiest way to get started:

1. **Overview:** change the wallpaper now, check rotation status, or open the
   library.
2. **Preferences:** select a source, configure its API key and search options,
   choose the download folder, and set the rotation interval.
3. Select **Save preferences** to apply settings.
4. Select **Start rotation** for automatic changes, or **Stop rotation** to
   stop it.
5. Visit **Library** to preview, open, or delete saved wallpapers; use
   **Activity** to inspect recent actions and errors.

The desktop timer accepts values from **one second** (`00:00:01`) upwards.
Very frequent Wallhaven requests can be limited by Wallhaven's API. The Pixabay
24-hour cache limits how often search results are requested from Pixabay, not
how often the app can choose and apply a cached photo.

## Command-line usage

The `wallpaper` CLI is installed on Linux and included in the Windows packages.
On Linux, use `wallpaper` if `~/.local/bin` is on your `PATH`; otherwise run
`~/.local/bin/wallpaper`.

### Examples

Download and apply a random SFW Wallhaven wallpaper:

```bash
wallpaper -Q "nature"
```

Choose a source and save the choice and search term:

```bash
wallpaper --source pixabay --pixabay-key "YOUR_PIXABAY_KEY" -Q "mountains" --set
```

Choose randomly between providers on each wallpaper change:

```bash
wallpaper --source both --pixabay-key "YOUR_PIXABAY_KEY" -Q "nature" --set
```

Set a Wallhaven API key for NSFW searches:

```bash
wallpaper --key "YOUR_WALLHAVEN_KEY" --purity 001 --set
```

Start automatic rotation every 30 minutes:

```bash
wallpaper --timer 30 --daemon
```

The CLI timer is in minutes. The minimum is one second, or approximately
`0.0166667` minutes:

```bash
wallpaper --timer 0.0166667 --set
```

`--set` saves the supplied settings without changing the wallpaper or starting
rotation. Settings are read from the shared config file on the next run and by
the running service. Avoid putting API keys directly in shell history; enter
them in the app's masked Preferences fields when possible.

### Service commands

| Command | What it does |
| --- | --- |
| `wallpaper --now` | Ask a running service to change the wallpaper now. |
| `wallpaper --status` | Show whether automatic rotation is running. |
| `wallpaper --stop` | Stop the background service. |
| `wallpaper --once` | Fetch and apply one wallpaper without starting a service. |
| `wallpaper --config` | Print the active configuration. Treat the output as secret if it contains API keys. |
| `wallpaper --log` | Print recent service logs. |
| `wallpaper --log --follow` | Follow service logs as they are written. |

### Common CLI options

| Option | Purpose |
| --- | --- |
| `-Q`, `--query` | Search terms. |
| `-T`, `--timer` | Rotation interval in minutes; minimum one second. |
| `-R`, `--resolution` | Wallhaven minimum image resolution, for example `1920x1080`. |
| `-A`, `--ratio` | Wallhaven aspect ratios, for example `16x9,21x9`. |
| `-P`, `--purity` | Wallhaven content mask: `100` SFW, `010` Sketchy, `001` NSFW. |
| `-C`, `--categories` | Wallhaven category mask: General, Anime, People. |
| `-S`, `--sort` | Wallhaven sorting option, such as `random`, `relevance`, or `toplist`. |
| `--top-range` | Wallhaven toplist period, for example `1M` or `1y`. |
| `-K`, `--key` | Wallhaven API key. |
| `--source` | `wallhaven`, `pixabay`, or `both` (`random` is an alias). |
| `--pixabay-key` | Pixabay API key. |
| `-M`, `--keep` | Maximum number of downloaded wallpapers to retain. |
| `--save-dir` | Wallpaper download folder. |

For the full option list, run `wallpaper --help`.

## Troubleshooting

- **No wallpapers found:** Check the spelling of your search terms and loosen
  provider-specific filters. Wallhaven can return zero results for a
  misspelled or overly restrictive search.
- **Pixabay does not download:** Confirm Pixabay is selected and its API key is
  entered. Pixabay search terms cannot exceed 100 characters.
- **NSFW search fails:** Confirm the Wallhaven API key is set and NSFW is
  enabled in the Wallhaven purity filters.
- **Wallpaper downloads but does not appear:** Check your desktop environment
  and the Activity log. Some Linux desktop environments need an image utility
  such as `feh`; KDE and GNOME use their desktop-specific integration.
- **Automatic rotation is delayed on Android:** Android WorkManager is
  controlled by the operating system and battery settings; its minimum
  interval is 15 minutes.
- **Inspect Linux service logs:** run `wallpaper --log`.

## Privacy and API use

The app uses the provider API you choose to search for images. API keys are
stored in your local app settings so the app can make those requests. Do not
share the settings file or CLI configuration output publicly. Pixabay search
responses are cached locally for 24 hours. Downloaded images are stored in the
folder you configure.

Review the providers' current terms and rate limits:
[Wallhaven API help](https://wallhaven.cc/help/api) ·
[Pixabay API documentation and terms](https://pixabay.com/api/docs/).

## Project layout

- `Linux/` — Linux GUI, wallpaper CLI, and installer.
- `Windows/` — Windows build scripts and installer definition.
- `Android/` — native Android app.
- `install.sh` / `uninstall.sh` — Linux install and uninstall entry points.
- `.github/workflows/` — automated Windows build and release workflow.

## License

See [LICENSE](LICENSE).
