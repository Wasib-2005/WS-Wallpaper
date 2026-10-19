import json
import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path


def report_error(config_dir, error):
    config_dir.mkdir(parents=True, exist_ok=True)
    log_file = config_dir / "wallpaper.log"
    timestamp = datetime.now().astimezone().strftime("%Y-%m-%d %H:%M:%S")
    with log_file.open("a", encoding="utf-8") as activity_log:
        activity_log.write(f"[{timestamp}] Windows startup error: {error}\n")


def main():
    app_data = Path(os.environ.get(
        "APPDATA", str(Path.home() / "AppData" / "Roaming"),
    ))
    config_dir = app_data / "Wallhaven"
    config_file = config_dir / "config.json"
    if not config_file.is_file():
        return

    try:
        with config_file.open(encoding="utf-8") as settings_file:
            config = json.load(settings_file)
        if not isinstance(config, dict):
            raise ValueError(f"Settings file at {config_file} must contain a JSON object.")
        timer = float(config.get("timer", 0) or 0)
    except (OSError, json.JSONDecodeError, TypeError, ValueError) as error:
        report_error(config_dir, error)
        return
    if timer <= 0:
        return

    app_dir = Path(sys.executable).resolve().parent
    cli = app_dir / "wallpaper.exe"
    if not cli.is_file():
        report_error(config_dir, f"Could not find the wallpaper CLI at {cli}")
        return

    try:
        subprocess.Popen(
            [str(cli), "--daemon"],
            stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP,
            close_fds=True,
        )
    except OSError as error:
        report_error(config_dir, error)


if __name__ == "__main__":
    main()
