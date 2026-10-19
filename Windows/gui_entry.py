import runpy
import sys
from pathlib import Path


def main():
    if getattr(sys, "frozen", False):
        app_script = Path(sys._MEIPASS) / "wallpaper-manager"
    else:
        app_script = Path(__file__).resolve().parents[1] / "Linux" / "wallpaper-manager"
    runpy.run_path(str(app_script), run_name="__main__")


if __name__ == "__main__":
    main()
