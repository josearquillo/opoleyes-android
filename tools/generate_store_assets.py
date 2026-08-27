"""
Genera los assets graficos para Play Store:
- icon_512.png         (512x512, icono cuadrado)
- feature_graphic.png  (1024x500, banner horizontal)

Replica el diseno del icono vectorial ic_logo_ol_v3.xml sobre fondo bg_dark (#0f172a).
"""
from PIL import Image, ImageDraw
import math

# Colores del disenio
BG_DARK = (15, 23, 42)        # #0f172a
TEAL = (20, 184, 166)         # #14b8a6
GOLD = (251, 191, 36)         # #fbbf24
GOLD_DARK = (245, 158, 11)    # #f59e0b
GOLD_LIGHT = (253, 230, 138)  # #fde68a
TEXT_LIGHT = (226, 232, 240)  # #e2e8f0


def draw_icon(draw, cx, cy, scale):
    """Dibuja el icono OpoLeyes (O con balanza + L) centrado en (cx, cy).
    scale = tamano del icono en pixels (ancho total ~ 56 unidades vectoriales)."""
    s = scale / 108.0  # factor de escala (viewport original = 108)

    def px(v):
        return v * s

    # --- Letra O: anillo turquesa ---
    # Centro del O en (42, 54), radio exterior 22, interior 15
    ocx, ocy = cx + px(42 - 54), cy + px(54 - 54)  # centrar O a la izquierda
    r_out = px(22)
    r_in = px(15)
    # Anillo: circulo exterior relleno, luego recorta interior con bg
    draw.ellipse([ocx - r_out, ocy - r_out, ocx + r_out, ocy + r_out], fill=TEAL)
    draw.ellipse([ocx - r_in, ocy - r_in, ocx + r_in, ocy + r_in], fill=BG_DARK)

    # --- Balanza dorada dentro de O ---
    # Poste vertical central
    post_x = ocx - px(0.5)
    draw.rectangle([post_x - px(1), ocy - px(10), post_x + px(1), ocy + px(8)], fill=GOLD)
    # Remate superior (circulo pequeno)
    draw.ellipse([post_x - px(1.5), ocy - px(11.5), post_x + px(1.5), ocy - px(8.5)], fill=GOLD)
    # Viga horizontal
    beam_y = ocy - px(7)
    draw.rectangle([post_x - px(11), beam_y, post_x + px(11), beam_y + px(2)], fill=GOLD)
    # Cadenas y platos (izq y der)
    for side in (-1, 1):
        chain_x = post_x + side * px(9)
        draw.rectangle([chain_x - px(0.3), beam_y + px(2), chain_x + px(0.3), beam_y + px(6)], fill=GOLD)
        # Plato (semicirculo)
        pan_cx = chain_x
        pan_cy = beam_y + px(6)
        draw.chord([pan_cx - px(4), pan_cy - px(0), pan_cx + px(4), pan_cy + px(5.5)],
                   start=0, end=180, fill=GOLD)
    # Base (trapecio)
    base_cx = post_x
    base_top = ocy + px(8)
    draw.polygon([
        (base_cx - px(4), base_top),
        (base_cx + px(4), base_top),
        (base_cx + px(2.5), base_top + px(3)),
        (base_cx - px(2.5), base_top + px(3)),
    ], fill=GOLD)

    # --- Letra L: a la derecha del O ---
    # En el vector: vertical (64,32)-(76,76), horizontal (64,68)-(88,76)
    l_x = cx + px(64 - 54)
    l_y_top = cy + px(32 - 54)
    l_w_vert = px(12)
    l_h_vert = px(44)
    l_h_horiz = px(8)
    l_w_horiz = px(24)
    # Vertical (dorado oscuro)
    draw.rectangle([l_x, l_y_top, l_x + l_w_vert, l_y_top + l_h_vert], fill=GOLD_DARK)
    # Highlight vertical
    draw.rectangle([l_x, l_y_top, l_x + px(1.5), l_y_top + l_h_vert], fill=GOLD_LIGHT)
    # Horizontal (dorado claro)
    draw.rectangle([l_x, l_y_top + l_h_vert - l_h_horiz, l_x + l_w_horiz, l_y_top + l_h_vert], fill=GOLD)
    # Highlight horizontal
    draw.rectangle([l_x, l_y_top + l_h_vert - l_h_horiz, l_x + l_w_horiz, l_y_top + l_h_vert - l_h_horiz + px(1.2)], fill=GOLD_LIGHT)


def make_icon_512(path):
    img = Image.new("RGB", (512, 512), BG_DARK)
    draw = ImageDraw.Draw(img)
    # Icono centrado, escala ~ 380px (deja margen)
    draw_icon(draw, 256, 256, 380)
    img.save(path, "PNG")
    print(f"Guardado: {path}")


def make_feature_graphic(path):
    img = Image.new("RGB", (1024, 500), BG_DARK)
    draw = ImageDraw.Draw(img)
    # Icono a la izquierda, escalado ~ 360px
    draw_icon(draw, 250, 250, 360)
    # Texto "OpoLeyes" a la derecha del icono
    # Pillow: usar fuente default grande
    try:
        from PIL import ImageFont
        font = ImageFont.truetype("C:\\Windows\\Fonts\\arialbd.ttf", 96)
    except Exception:
        font = ImageFont.load_default()
    text = "OpoLeyes"
    # Calcular posicion para centrar verticalmente
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    tx = 500
    ty = 250 - th // 2
    # Sombra sutil
    draw.text((tx + 3, ty + 3), text, font=font, fill=(0, 0, 0))
    draw.text((tx, ty), text, font=font, fill=TEXT_LIGHT)
    # Subtitulo
    try:
        font_sub = ImageFont.truetype("C:\\Windows\\Fonts\\arial.ttf", 36)
    except Exception:
        font_sub = ImageFont.load_default()
    sub = "Preparate para tu examen de oposiciones"
    bbox2 = draw.textbbox((0, 0), sub, font=font_sub)
    sw = bbox2[2] - bbox2[0]
    sx = 500
    sy = ty + th + 20
    draw.text((sx, sy), sub, font=font_sub, fill=GOLD)
    img.save(path, "PNG")
    print(f"Guardado: {path}")


if __name__ == "__main__":
    import os
    out = r"D:\ESCRITORIO\APPS\opotest-android\store-assets"
    os.makedirs(out, exist_ok=True)
    make_icon_512(os.path.join(out, "icon_512.png"))
    make_feature_graphic(os.path.join(out, "feature_graphic.png"))
    print("Done.")
