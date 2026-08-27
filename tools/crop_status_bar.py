"""Recorta la barra de estado superior de todas las capturas.
La barra de estado en un HONOR CRT-NX1 (1080x2388) ocupa ~90px arriba.
Recortamos esos px y guardamos sobre el mismo archivo.
"""
from PIL import Image
import os
import glob

# Altura de la barra de estado en pixels (HONOR MagicOS ~90px a 1080 de ancho)
STATUS_BAR_HEIGHT = 90

screenshots_dir = r"D:\ESCRITORIO\APPS\opotest-android\store-assets\screenshots"
files = sorted(glob.glob(os.path.join(screenshots_dir, "*.png")))

print(f"Procesando {len(files)} capturas...")
for f in files:
    img = Image.open(f)
    w, h = img.size
    print(f"  {os.path.basename(f)}: {w}x{h}", end=" -> ")
    # Recortar la franja superior
    cropped = img.crop((0, STATUS_BAR_HEIGHT, w, h))
    cropped.save(f, "PNG")
    print(f"{cropped.size[0]}x{cropped.size[1]}")

print("Done.")
