#!/usr/bin/env python3
"""
Modern Airbnb-style Android launcher icon generator

Creates launcher icons for all Android density buckets
with:
- Warm coral background
- Rounded corners
- Cleaner spacing
- Modern minimal aesthetic

Requires:
    pip install Pillow
"""

from PIL import Image, ImageDraw
import os

# Android launcher icon sizes
SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

# Airbnb-inspired coral color
BACKGROUND_COLOR = (255, 90, 95, 255)

# Optional:
# Set to True if your logo is already white
USE_ORIGINAL_LOGO_COLORS = False


def make_logo_white(logo):
    """
    Convert logo to solid white while preserving transparency.
    """

    logo = logo.convert("RGBA")

    white_logo = Image.new("RGBA", logo.size, (255, 255, 255, 0))

    pixels = logo.load()
    white_pixels = white_logo.load()

    for y in range(logo.size[1]):
        for x in range(logo.size[0]):
            r, g, b, a = pixels[x, y]

            if a > 0:
                white_pixels[x, y] = (255, 255, 255, a)

    return white_logo


def apply_rounded_corners(image, radius):
    """
    Apply rounded corners to icon.
    """

    mask = Image.new("L", image.size, 0)

    draw = ImageDraw.Draw(mask)

    draw.rounded_rectangle(
        [(0, 0), image.size],
        radius=radius,
        fill=255
    )

    rounded = Image.new("RGBA", image.size)

    rounded.paste(image, (0, 0), mask)

    return rounded


def create_launcher_icon(logo_path, output_dir, density, size):
    """
    Create a launcher icon with modern Airbnb-style design.
    """

    # Create background
    icon = Image.new(
        'RGBA',
        (size, size),
        BACKGROUND_COLOR
    )

    # Open logo
    logo = Image.open(logo_path).convert("RGBA")

    # Convert logo to white
    if not USE_ORIGINAL_LOGO_COLORS:
        logo = make_logo_white(logo)

    # Logo sizing
    # Smaller = more premium/minimal look
    logo_size = int(size * 0.56)

    logo = logo.resize(
        (logo_size, logo_size),
        Image.Resampling.LANCZOS
    )

    # Center logo
    offset = (
        (size - logo_size) // 2,
        (size - logo_size) // 2
    )

    # Paste logo
    icon.paste(logo, offset, logo)

    # Apply rounded corners
    corner_radius = int(size * 0.22)

    icon = apply_rounded_corners(
        icon,
        corner_radius
    )

    # Create output directory
    mipmap_dir = os.path.join(
        output_dir,
        f'mipmap-{density}'
    )

    os.makedirs(mipmap_dir, exist_ok=True)

    # Save launcher icons
    icon.save(
        os.path.join(mipmap_dir, 'ic_launcher.png'),
        'PNG'
    )

    icon.save(
        os.path.join(mipmap_dir, 'ic_launcher_round.png'),
        'PNG'
    )

    print(f"✓ Generated {density} icons ({size}x{size})")


def main():

    # Locate project paths
    script_dir = os.path.dirname(
        os.path.abspath(__file__)
    )

    logo_path = os.path.join(
        script_dir,
        'app',
        'src',
        'main',
        'res',
        'drawable',
        'verdant_logo.png'
    )

    res_dir = os.path.join(
        script_dir,
        'app',
        'src',
        'main',
        'res'
    )

    # Validate logo file
    if not os.path.exists(logo_path):

        print()
        print("❌ ERROR")
        print(f"Logo file not found:")
        print(logo_path)
        print()

        return

    print()
    print("══════════════════════════════════════")
    print(" Modern Launcher Icon Generator")
    print("══════════════════════════════════════")
    print()

    print(f"Source Logo:")
    print(f"  {logo_path}")
    print()

    print(f"Generating icons into:")
    print(f"  {res_dir}")
    print()

    # Generate icons
    for density, size in SIZES.items():

        create_launcher_icon(
            logo_path,
            res_dir,
            density,
            size
        )

    print()
    print("══════════════════════════════════════")
    print(" ✅ All icons generated successfully!")
    print("══════════════════════════════════════")
    print()

    print("Next Steps:")
    print("1. Delete old .webp launcher icons")
    print("2. Clean/Rebuild project")
    print("3. Reinstall app on emulator/device")
    print()


if __name__ == '__main__':

    try:
        main()

    except ImportError:

        print()
        print("❌ Pillow is not installed")
        print()
        print("Install with:")
        print("pip install Pillow")
        print()

    except Exception as e:

        print()
        print("❌ Unexpected Error")
        print(e)
        print()