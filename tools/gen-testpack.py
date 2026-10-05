#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
# SPDX-License-Identifier: GPL-3.0-or-later
"""Genera el paquete de iconos de PRUEBA propio que solo viaja en la build debug (app/src/debug).

Sirve para validar el flujo de paquetes de iconos sin instalar packs de terceros: 4 iconos con componente concreto (dos con
esquinas transparentes = «ya traen su forma», uno cuadrado opaco = «recibe la forma del launcher»), y fondo / máscara / capa
superior / escala para el resto de apps. Todo dibujado aquí; nada de terceros. En release NO existe (src/debug).

Uso: python3 tools/gen-testpack.py
Salida: app/src/debug/res/drawable-nodpi/ulpack_*.png y app/src/debug/res/xml/appfilter.xml
"""
import pathlib

import numpy as np
from PIL import Image, ImageDraw

N = 192
root = pathlib.Path(__file__).resolve().parent.parent / "app/src/debug"
res = root / "res/drawable-nodpi"
res.mkdir(parents=True, exist_ok=True)
(root / "res/xml").mkdir(parents=True, exist_ok=True)


def gradient(c0, c1):
    y = np.linspace(0, 1, N, dtype=np.float32)[:, None, None]
    a = np.array(c0, np.float32)[None, None, :]
    b = np.array(c1, np.float32)[None, None, :]
    img = np.broadcast_to(a * (1 - y) + b * y, (N, N, 3))
    return Image.fromarray(img.astype(np.uint8)).convert("RGBA")


def circle_mask(margin=6):
    m = Image.new("L", (N * 4, N * 4), 0)
    ImageDraw.Draw(m).ellipse([margin * 4, margin * 4, (N - margin) * 4, (N - margin) * 4], fill=255)
    return m.resize((N, N), Image.LANCZOS)


def save(img, name):
    img.save(res / f"{name}.png")


# 1) tres iconos redondos con esquinas transparentes
for name, c0, c1, glyph in (("ulpack_settings", (30, 140, 150), (20, 70, 110), "gear"),
                            ("ulpack_camera", (240, 110, 150), (150, 50, 120), "lens"),
                            ("ulpack_phone", (90, 200, 120), (30, 120, 80), "bar")):
    base = gradient(c0, c1)
    d = ImageDraw.Draw(base)
    if glyph == "gear":
        for k in range(8):
            a = k * np.pi / 4
            cx, cy = N / 2 + 52 * np.cos(a), N / 2 + 52 * np.sin(a)
            d.ellipse([cx - 11, cy - 11, cx + 11, cy + 11], fill=(255, 255, 255, 235))
        d.ellipse([N / 2 - 46, N / 2 - 46, N / 2 + 46, N / 2 + 46], fill=(255, 255, 255, 235))
        d.ellipse([N / 2 - 20, N / 2 - 20, N / 2 + 20, N / 2 + 20], fill=c1 + (255,))
    elif glyph == "lens":
        d.ellipse([N / 2 - 50, N / 2 - 50, N / 2 + 50, N / 2 + 50], fill=(255, 255, 255, 235))
        d.ellipse([N / 2 - 32, N / 2 - 32, N / 2 + 32, N / 2 + 32], fill=(60, 20, 70, 255))
        d.ellipse([N / 2 - 12, N / 2 - 22, N / 2 + 2, N / 2 - 8], fill=(255, 255, 255, 200))
    else:
        d.rounded_rectangle([N / 2 - 14, N / 2 - 52, N / 2 + 14, N / 2 + 52], 14, fill=(255, 255, 255, 235))
        d.ellipse([N / 2 - 10, N / 2 + 30, N / 2 + 10, N / 2 + 50], fill=c1 + (255,))
    base.putalpha(circle_mask())
    save(base, name)

# 2) cuadrado opaco (recibe la forma del launcher)
sq = gradient((255, 170, 60), (220, 70, 60))
d = ImageDraw.Draw(sq)
d.ellipse([N / 2 - 38, N / 2 - 50, N / 2 + 38, N / 2 + 26], fill=(255, 245, 200, 255))
d.polygon([(0, N), (N * 0.35, N * 0.62), (N * 0.6, N * 0.85), (N * 0.8, N * 0.68), (N, N)], fill=(120, 30, 60, 255))
save(sq, "ulpack_gallery")

# 3) fondo, máscara y capa superior para el resto de apps
back = gradient((50, 56, 80), (20, 24, 40))
rr = Image.new("L", (N * 4, N * 4), 0)
ImageDraw.Draw(rr).rounded_rectangle([0, 0, N * 4 - 1, N * 4 - 1], 40 * 4, fill=255)
back.putalpha(rr.resize((N, N), Image.LANCZOS))
save(back, "ulpack_iconback")

# máscara: OPACA fuera de la forma (se resta del icono original con DST_OUT)
mask = Image.new("RGBA", (N, N), (0, 0, 0, 255))
inner = Image.new("L", (N * 4, N * 4), 0)
ImageDraw.Draw(inner).rounded_rectangle([0, 0, N * 4 - 1, N * 4 - 1], 40 * 4, fill=255)
a = np.array(inner.resize((N, N), Image.LANCZOS), np.float32)
mask.putalpha(Image.fromarray((255 - a).astype(np.uint8)))
save(mask, "ulpack_iconmask")

upon = Image.new("RGBA", (N, N), (255, 255, 255, 0))
ov = np.zeros((N, N), np.float32)
yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
ov = np.clip(1.0 - (yy / (N * 0.55)), 0, 1) * 70
ov *= (np.array(inner.resize((N, N), Image.LANCZOS), np.float32) / 255.0)
upon.putalpha(Image.fromarray(ov.astype(np.uint8)))
save(upon, "ulpack_iconupon")

xml = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Paquete de iconos de PRUEBA (solo build debug). Mezcla referencias @drawable/ y nombres a secas para probar el lector del XML compilado. -->
<resources>
    <iconback img1="@drawable/ulpack_iconback" />
    <iconmask img1="@drawable/ulpack_iconmask" />
    <iconupon img1="@drawable/ulpack_iconupon" />
    <scale factor="0.7" />
    <item component="ComponentInfo{com.android.settings/com.android.settings.Settings}" drawable="@drawable/ulpack_settings" />
    <item component="ComponentInfo{com.oplus.camera/com.oplus.camera.Camera}" drawable="ulpack_camera" />
    <item component="ComponentInfo{com.google.android.dialer/com.google.android.dialer.extensions.GoogleDialtactsActivity}" drawable="@drawable/ulpack_phone" />
    <item component="ComponentInfo{com.coloros.gallery3d/com.coloros.gallery3d.app.MainActivity}" drawable="ulpack_gallery" />
</resources>
'''
(root / "res/xml/appfilter.xml").write_text(xml)
print("Paquete de prueba generado en", root)
