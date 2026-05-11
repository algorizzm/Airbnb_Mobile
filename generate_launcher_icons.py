#!/usr/bin/env python3
"""
Generate Android launcher icons from verdant_logo.png
This script creates properly sized launcher icons for all density buckets.
Requires: Pillow (pip install Pillow)
"""

from PIL import Image
import os

# Icon sizes for each density
SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

def create_launcher_icon(logo_path, output_dir, density, size):
    """Create a launcher icon with dark background and centered logo"""
    
    # Create dark background
    icon = Image.new('RGBA', (size, size), (18, 18, 18, 255))  # #121212
    
    # Open and resize logo
    logo = Image.open(logo_path)
    
    # Calculate logo size (70% of icon size to leave padding)
    logo_size = int(size * 0.7)
    logo = logo.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
    
    # Center the logo
    offset = ((size - logo_size) // 2, (size - logo_size) // 2)
    icon.paste(logo, offset, logo if logo.mode == 'RGBA' else None)
    
    # Create output directory if it doesn't exist
    mipmap_dir = os.path.join(output_dir, f'mipmap-{density}')
    os.makedirs(mipmap_dir, exist_ok=True)
    
    # Save as PNG (replacing .webp)
    icon.save(os.path.join(mipmap_dir, 'ic_launcher.png'), 'PNG')
    icon.save(os.path.join(mipmap_dir, 'ic_launcher_round.png'), 'PNG')
    
    print(f"✓ Generated {density} icons ({size}x{size})")

def main():
    # Paths
    script_dir = os.path.dirname(os.path.abspath(__file__))
    logo_path = os.path.join(script_dir, 'app', 'src', 'main', 'res', 'drawable', 'verdant_logo.png')
    res_dir = os.path.join(script_dir, 'app', 'src', 'main', 'res')
    
    if not os.path.exists(logo_path):
        print(f"❌ Error: verdant_logo.png not found at {logo_path}")
        return
    
    print("Generating launcher icons from verdant_logo.png...")
    print(f"Source: {logo_path}")
    print(f"Output: {res_dir}/mipmap-*/")
    print()
    
    # Generate icons for each density
    for density, size in SIZES.items():
        create_launcher_icon(logo_path, res_dir, density, size)
    
    print()
    print("✅ All launcher icons generated successfully!")
    print()
    print("Next steps:")
    print("1. Delete the old .webp files from each mipmap-* folder")
    print("2. Rebuild the project in Android Studio")
    print("3. Reinstall the app to see the new icon")

if __name__ == '__main__':
    try:
        main()
    except ImportError:
        print("❌ Error: Pillow library not found")
        print("Install it with: pip install Pillow")
    except Exception as e:
        print(f"❌ Error: {e}")
