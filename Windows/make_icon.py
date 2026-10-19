import sys
from pathlib import Path

from PIL import Image


def main():
    source = Path(sys.argv[1])
    destination = Path(sys.argv[2])
    with Image.open(source) as image:
        image.save(
            destination,
            format="ICO",
            sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
        )


if __name__ == "__main__":
    main()
