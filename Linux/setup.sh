#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_DIR="$HOME/.local/bin"
APP_DIR="$HOME/.local/share/applications"
AUTOSTART_DIR="$HOME/.config/autostart"
ICON_DIR="$HOME/.local/share/icons/hicolor/512x512/apps"
TARGET_GUI="$TARGET_DIR/ws-wallpaper"
LEGACY_GUI="$TARGET_DIR/wallpaper-manager"
APP_ENTRY="$APP_DIR/ws-wallpaper.desktop"
LEGACY_APP_ENTRY="$APP_DIR/wallhaven-wallpaper-manager.desktop"

OS_ID=""
OS_ID_LIKE=""
if [ -r /etc/os-release ]; then
    . /etc/os-release
    OS_ID="${ID:-}"
    OS_ID_LIKE="${ID_LIKE:-}"
fi

PYTHON_OK=false
TKINTER_OK=false
PILLOW_TK_OK=false

check_runtime_dependencies() {
    PYTHON_OK=false
    TKINTER_OK=false
    PILLOW_TK_OK=false

    if command -v python3 >/dev/null 2>&1; then
        PYTHON_OK=true
        if python3 -c 'import tkinter' >/dev/null 2>&1; then
            TKINTER_OK=true
        fi
        if python3 -c 'from PIL import Image, ImageTk' >/dev/null 2>&1; then
            PILLOW_TK_OK=true
        fi
    fi
}

show_dependency_status() {
    echo "Checking dependencies..."
    if [ "$PYTHON_OK" = true ]; then
        echo "  Python 3 ................ installed"
    else
        echo "  Python 3 ................ needs install"
    fi
    if [ "$TKINTER_OK" = true ]; then
        echo "  Tkinter ................. installed"
    else
        echo "  Tkinter ................. needs install"
    fi
    if [ "$PILLOW_TK_OK" = true ]; then
        echo "  Pillow with Tk support .. installed"
    else
        echo "  Pillow with Tk support .. needs install"
    fi
}

install_privileged() {
    if [ "${EUID:-$(id -u)}" -eq 0 ]; then
        "$@"
    elif command -v sudo >/dev/null 2>&1; then
        sudo "$@"
    else
        echo "Error: installing dependencies requires root access or sudo." >&2
        exit 1
    fi
}

install_runtime_dependencies() {
    local package_manager=""
    local -a packages=()
    local -a missing=()

    [ "$PYTHON_OK" = true ] || missing+=("Python 3")
    [ "$TKINTER_OK" = true ] || missing+=("Tkinter")
    [ "$PILLOW_TK_OK" = true ] || missing+=("Pillow with Tk support")

    case " $OS_ID $OS_ID_LIKE " in
        *" debian "*|*" ubuntu "*)
            package_manager="apt-get"
            [ "$PYTHON_OK" = true ] || packages+=(python3)
            [ "$TKINTER_OK" = true ] || packages+=(python3-tk)
            [ "$PILLOW_TK_OK" = true ] || packages+=(python3-pil python3-pil.imagetk)
            ;;
        *" fedora "*|*" rhel "*|*" centos "*)
            package_manager="dnf"
            [ "$PYTHON_OK" = true ] || packages+=(python3)
            [ "$TKINTER_OK" = true ] || packages+=(python3-tkinter)
            [ "$PILLOW_TK_OK" = true ] || packages+=(python3-pillow python3-pillow-tk)
            ;;
        *" arch "*)
            package_manager="pacman"
            [ "$PYTHON_OK" = true ] || packages+=(python)
            [ "$TKINTER_OK" = true ] || packages+=(tk)
            [ "$PILLOW_TK_OK" = true ] || packages+=(python-pillow)
            ;;
        *" opensuse "*|*" suse "*)
            package_manager="zypper"
            [ "$PYTHON_OK" = true ] || packages+=(python3)
            [ "$TKINTER_OK" = true ] || packages+=(python3-tk)
            [ "$PILLOW_TK_OK" = true ] || packages+=(python3-Pillow)
            ;;
        *)
            echo "Error: unable to identify a supported Linux package manager." >&2
            echo "Supported package managers: apt, dnf, pacman, and zypper." >&2
            echo "Install Python 3, Tkinter, and Pillow with Tk support, then retry." >&2
            exit 1
            ;;
    esac

    if ! command -v "$package_manager" >/dev/null 2>&1; then
        echo "Error: $package_manager is not available to install missing dependencies." >&2
        exit 1
    fi

    echo "Need to install: ${missing[*]}"
    echo "Installing ${packages[*]} with $package_manager..."
    case "$package_manager" in
        apt-get)
            install_privileged apt-get update
            install_privileged apt-get install -y "${packages[@]}"
            ;;
        dnf)
            install_privileged dnf install -y "${packages[@]}"
            ;;
        pacman)
            install_privileged pacman -Sy --needed --noconfirm "${packages[@]}"
            ;;
        zypper)
            install_privileged zypper --non-interactive install "${packages[@]}"
            ;;
    esac
}

check_runtime_dependencies
show_dependency_status

if [ "$PYTHON_OK" != true ] || [ "$TKINTER_OK" != true ] || [ "$PILLOW_TK_OK" != true ]; then
    install_runtime_dependencies
    check_runtime_dependencies
    echo "Verifying dependencies..."
    show_dependency_status
fi

if [ "$PYTHON_OK" != true ] || [ "$TKINTER_OK" != true ] || [ "$PILLOW_TK_OK" != true ]; then
    echo "Error: one or more dependencies are still missing after installation." >&2
    echo "Check that your distribution's package installation completed successfully, then retry." >&2
    exit 1
fi

if [ ! -f "$SCRIPT_DIR/wallpaper" ] || [ ! -f "$SCRIPT_DIR/wallpaper-manager" ] ||
   [ ! -f "$SCRIPT_DIR/ws-wallpaper.png" ]; then
    echo "Error: installer files are incomplete; keep setup.sh beside wallpaper, wallpaper-manager, and ws-wallpaper.png." >&2
    exit 1
fi

mkdir -p "$TARGET_DIR" "$APP_DIR" "$AUTOSTART_DIR" "$ICON_DIR"

install -m 755 "$SCRIPT_DIR/wallpaper" "$TARGET_DIR/wallpaper"
install -m 755 "$SCRIPT_DIR/wallpaper-manager" "$TARGET_GUI"
install -m 644 "$SCRIPT_DIR/ws-wallpaper.png" "$ICON_DIR/ws-wallpaper.png"
ln -sfn "$TARGET_GUI" "$LEGACY_GUI"

cat > "$APP_ENTRY" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=WS Wallpaper
Comment=Discover and manage desktop wallpapers
Exec="$TARGET_GUI"
Icon=ws-wallpaper
Terminal=false
Categories=Utility;Graphics;
StartupNotify=true
EOF
chmod 644 "$APP_ENTRY"
rm -f "$LEGACY_APP_ENTRY"

cat > "$AUTOSTART_DIR/wallhaven-daemon.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=WS Wallpaper background service
Exec="$TARGET_DIR/wallpaper" --daemon
Hidden=false
NoDisplay=true
X-GNOME-Autostart-enabled=true
Comment=Automatically changes the desktop wallpaper in the background
EOF
chmod 644 "$AUTOSTART_DIR/wallhaven-daemon.desktop"

if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$APP_DIR" >/dev/null 2>&1 || true
fi

echo "WS Wallpaper installed for $USER."
echo "Launch it from your applications menu or run: $TARGET_GUI"
echo "CLI available at: $TARGET_DIR/wallpaper"
echo "The background service starts on login when a rotation interval is configured."
case ":$PATH:" in
    *":$TARGET_DIR:"*) ;;
    *) echo "Note: add $TARGET_DIR to PATH to run 'wallpaper' from a terminal." ;;
esac
