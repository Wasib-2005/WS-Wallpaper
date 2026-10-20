#!/usr/bin/env bash

set -e

TARGET_BIN="$HOME/.local/bin/wallpaper"
TARGET_GUI="$HOME/.local/bin/ws-wallpaper"
LEGACY_GUI="$HOME/.local/bin/wallpaper-manager"
APP_ENTRY="$HOME/.local/share/applications/ws-wallpaper.desktop"
ICON_FILE="$HOME/.local/share/icons/hicolor/512x512/apps/ws-wallpaper.png"
LEGACY_APP_ENTRY="$HOME/.local/share/applications/wallhaven-wallpaper-manager.desktop"
DESKTOP_ENTRY="$HOME/.config/autostart/wallhaven-daemon.desktop"
CONFIG_DIR="$HOME/.config/ws-wallpaper"
PID_FILE="$CONFIG_DIR/wallpaper.pid"

echo "Removing WS Wallpaper..."

# 1. Stop any running daemon instance (daemon runs without -d, so use the PID file)
if [ -f "$PID_FILE" ]; then
    echo "🛑 Stopping running wallpaper daemon..."
    if [ -x "$TARGET_BIN" ]; then
        "$TARGET_BIN" --stop 2>/dev/null || true
    elif command -v wallpaper >/dev/null 2>&1; then
        wallpaper --stop 2>/dev/null || true
    fi

    # Fallback kill if process still lingers
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE" 2>/dev/null || true)
        if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
            kill "$PID" 2>/dev/null || true
        fi
        rm -f "$PID_FILE"
    fi
fi

# 2. Remove binary executable
if [ -f "$TARGET_BIN" ]; then
    rm -f "$TARGET_BIN"
    echo "🗑️ Removed executable: $TARGET_BIN"
else
    echo "ℹ️ Binary not found at $TARGET_BIN (skipping)."
fi

# 3. Remove graphical app and launchers
if [ -f "$TARGET_GUI" ]; then
    rm -f "$TARGET_GUI"
    echo "🗑️ Removed GUI: $TARGET_GUI"
fi
if [ -L "$LEGACY_GUI" ] || [ -f "$LEGACY_GUI" ]; then
    rm -f "$LEGACY_GUI"
    echo "🗑️ Removed compatibility launcher: $LEGACY_GUI"
fi
if [ -f "$APP_ENTRY" ]; then
    rm -f "$APP_ENTRY"
    echo "🗑️ Removed application menu entry: $APP_ENTRY"
fi
if [ -f "$ICON_FILE" ]; then
    rm -f "$ICON_FILE"
    echo "🗑️ Removed application icon: $ICON_FILE"
fi
if [ -f "$LEGACY_APP_ENTRY" ]; then
    rm -f "$LEGACY_APP_ENTRY"
    echo "🗑️ Removed previous application menu entry: $LEGACY_APP_ENTRY"
fi
if [ -f "$DESKTOP_ENTRY" ]; then
    rm -f "$DESKTOP_ENTRY"
    echo "🗑️ Removed autostart entry: $DESKTOP_ENTRY"
else
    echo "ℹ️ Autostart entry not found (skipping)."
fi

# 4. Prompt for configuration & cache cleanup
if [ -d "$CONFIG_DIR" ]; then
    read -r -p "Do you also want to delete your settings and logs ($CONFIG_DIR)? (y/N): " rm_config || rm_config=""
    case "$rm_config" in
        [yY][eE][sS]|[yY])
            rm -rf "$CONFIG_DIR"
            echo "🗑️ Removed configuration and log directory: $CONFIG_DIR"
            ;;
        *)
            echo "📁 Kept configuration directory at $CONFIG_DIR"
            ;;
    esac
else
    echo "ℹ️ Configuration directory not found (skipping)."
fi

echo ""
echo "✨ Uninstallation complete!"

if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$HOME/.local/share/applications" >/dev/null 2>&1 || true
fi
