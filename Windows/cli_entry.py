import runpy
import sys
from pathlib import Path


def main():
    if getattr(sys, "frozen", False):
        app_script = Path(sys._MEIPASS) / "wallpaper"
    else:
        app_script = Path(__file__).resolve().parents[1] / "Linux" / "wallpaper"
    runpy.run_path(str(app_script), run_name="__main__")


if __name__ == "__main__":
    main()
