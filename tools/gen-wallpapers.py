#!/usr/bin/env python3
"""Genera el pack de fondos de pantalla propios de UltimateLauncher (WebP) por código, sin imágenes de terceros.

Cada fondo es un cuadrado de SIDE x SIDE px con la composición pensada para recortarse en el centro: el selector hace un
«cover» centrado, así que sirve para móvil vertical (recorte alto y estrecho) y tablet apaisada (recorte ancho y bajo).
Todo sale de fórmulas (degradados de malla, ruido procedural, siluetas vectoriales, círculos con bokeh, facetas...) con
semillas fijas: ejecutar de nuevo da los mismos píxeles. Licencia del resultado: la del proyecto (obra propia, generada).

Uso:  python3 tools/gen-wallpapers.py [--out app/src/main/res/drawable-nodpi] [--preview carpeta]
Requiere: Python 3, numpy y Pillow con WebP.
Salida: wp_01_aurora.webp ... (nombres de recurso: minúsculas, guion bajo) y, si se pide, una hoja de contactos.
"""
import argparse
import pathlib

import numpy as np
from PIL import Image

SIDE = 2304  # múltiplo de 16; cubre 1080x2400 (retrato) y 2800x1840 (tablet) con recorte centrado y poco escalado
QUALITY = 88


# ---------------------------------------------------------------- utilidades
def grid(n=SIDE):
    y, x = np.mgrid[0:n, 0:n].astype(np.float32)
    return x / (n - 1), y / (n - 1)


def hexc(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], np.float32) / 255.0


def smooth(a, b, v):
    t = np.clip((v - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def mix(c0, c1, t):
    return c0 * (1 - t[..., None]) + c1 * t[..., None]


def fbm(rng, octaves=5, base=3, n=SIDE):
    """Ruido de valor suave multiescala en [0,1], generado en baja resolución y ampliado (bicúbico)."""
    out = np.zeros((n, n), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        k = base * (2 ** o)
        small = rng.random((k + 3, k + 3)).astype(np.float32)
        img = Image.fromarray(small).resize((n + n // k * 2, n + n // k * 2), Image.BICUBIC)
        pad = n // k
        out += amp * np.asarray(img)[pad:pad + n, pad:pad + n]
        tot += amp
        amp *= 0.5
    out /= tot
    lo, hi = np.percentile(out, 1), np.percentile(out, 99)
    return np.clip((out - lo) / (hi - lo), 0, 1)


def mesh(points, x, y, power=2.2):
    """Degradado de malla: mezcla de colores anclados en puntos (pesos por distancia inversa)."""
    num = 0.0
    den = 0.0
    for (px, py, col) in points:
        d2 = (x - px) ** 2 + (y - py) ** 2 + 1e-4
        w = d2 ** (-power / 2)
        num = num + w[..., None] * hexc(col)
        den = den + w
    return num / den[..., None]


def finish(rgb, rng, grain=0.012):
    """Granulado fino para evitar bandas en los degradados y conversión a 8 bits."""
    rgb = rgb + (rng.random(rgb.shape[:2], dtype=np.float32) - 0.5)[..., None] * grain
    return Image.fromarray((np.clip(rgb, 0, 1) * 255 + 0.5).astype(np.uint8))


def disk(x, y, cx, cy, r, soft):
    d = np.sqrt((x - cx) ** 2 + (y - cy) ** 2)
    return 1.0 - smooth(r - soft, r + soft, d)


# ---------------------------------------------------------------- fondos
def wp_aurora(rng):
    """Malla suave en verdes, azules y violetas sobre fondo oscuro."""
    x, y = grid()
    warp = fbm(rng, 3, 2)
    xx, yy = x + (warp - 0.5) * 0.18, y + (fbm(rng, 3, 2) - 0.5) * 0.18
    pts = [(0.15, 0.1, "#0b1d3a"), (0.85, 0.05, "#1b2a6b"), (0.5, 0.35, "#2fb6a3"), (0.1, 0.6, "#5a3fb0"),
           (0.9, 0.7, "#1f6fd1"), (0.45, 0.95, "#0a1a2e"), (0.75, 0.45, "#7be0b8")]
    return mesh(pts, xx, yy, 2.6)


def wp_atardecer(rng):
    """Malla cálida: coral, naranja, rosa y violeta profundo."""
    x, y = grid()
    warp = fbm(rng, 3, 2)
    xx, yy = x + (warp - 0.5) * 0.2, y + (fbm(rng, 3, 2) - 0.5) * 0.2
    pts = [(0.5, 0.0, "#2b1a5c"), (0.1, 0.3, "#8a2f8c"), (0.9, 0.35, "#d94a73"), (0.5, 0.6, "#ff8a4a"),
           (0.15, 0.85, "#ffb36b"), (0.85, 0.9, "#f0667a"), (0.5, 1.0, "#ffd08a")]
    return mesh(pts, xx, yy, 2.4)


def wp_oceano(rng):
    """Malla fría clara: azules, turquesa y arena."""
    x, y = grid()
    warp = fbm(rng, 3, 2)
    xx, yy = x + (warp - 0.5) * 0.16, y + (fbm(rng, 3, 2) - 0.5) * 0.16
    pts = [(0.2, 0.05, "#e8f6ff"), (0.85, 0.15, "#9fd8f2"), (0.5, 0.45, "#3a9ad9"), (0.1, 0.65, "#1d6fb8"),
           (0.9, 0.7, "#2bb6c6"), (0.4, 1.0, "#0c3f7a"), (0.8, 0.98, "#bfe9e1")]
    return mesh(pts, xx, yy, 2.5)


def wp_seda(rng):
    """Seda: pliegues suaves de un campo deformado, en vino, rosa y oro (paleta de tres tintas)."""
    x, y = grid()
    w1 = fbm(rng, 3, 2)
    w2 = fbm(rng, 3, 2)
    f = 1.3 * x + 0.9 * y + 0.9 * w1 + 0.35 * np.sin(2.6 * y + 2.0 * w2)
    t = 0.5 + 0.5 * np.sin(2 * np.pi * f * 1.15)
    c0, c1, c2 = hexc("#2a0f2e"), hexc("#b4476b"), hexc("#f2b880")
    shape = (SIDE, SIDE, 3)
    img = mix(mix(np.broadcast_to(c0, shape), np.broadcast_to(c1, shape), smooth(0.0, 0.75, t)),
              np.broadcast_to(c2, shape), smooth(0.7, 1.0, t) * 0.85)
    shade = 0.9 + 0.1 * np.cos(2 * np.pi * f * 1.15)  # brillo de tela en las crestas
    return img * shade[..., None]


def ridge_layers(rng, sky_top, sky_bot, layers, sun=None, fog=0.55):
    """Siluetas de montañas/dunas apiladas, cada capa más oscura y cercana, con niebla entre capas."""
    x, y = grid()
    img = mix(hexc(sky_top), hexc(sky_bot), smooth(0.0, 0.75, y))
    if sun:
        sx, sy, sr, sc = sun
        glow = np.exp(-(((x - sx) ** 2 + (y - sy) ** 2) / (2 * (sr * 2.6) ** 2)))
        img = img + glow[..., None] * hexc(sc) * 0.35
        img = mix(img, np.broadcast_to(hexc(sc), img.shape), disk(x, y, sx, sy, sr, 0.0025))
    n = len(layers)
    for i, (base, amp, color, rough) in enumerate(layers):
        h = np.zeros(SIDE, np.float32)
        xs = np.linspace(0, 1, SIDE, dtype=np.float32)
        for o in range(5):
            f = rough * (2 ** o)
            h += (amp / (1.9 ** o)) * np.sin(2 * np.pi * (f * xs) + rng.random() * 6.28)
        edge = base + h  # altura (fracción de la imagen) de la cresta en cada columna
        mask = smooth(-0.0012, 0.0012, y - edge[None, :])
        col = hexc(color)
        depth = i / max(1, n - 1)
        fogmix = fog * (1 - depth) * smooth(edge[None, :] - 0.02, edge[None, :] + 0.45, y)
        layer = np.broadcast_to(col, img.shape).copy()
        layer = mix(layer, np.broadcast_to(hexc(sky_bot), img.shape), np.clip(fogmix, 0, 1))
        img = mix(img, layer, mask)
    return img


def wp_dunas(rng):
    return ridge_layers(rng, "#3b2a63", "#f4a261",
                        [(0.52, 0.035, "#e07a5f", 0.9), (0.62, 0.045, "#b5483f", 1.3),
                         (0.74, 0.05, "#7a2e3a", 1.1), (0.88, 0.04, "#3d1d36", 1.6)],
                        sun=(0.5, 0.36, 0.07, "#ffe3b0"), fog=0.4)


def wp_montanas(rng):
    return ridge_layers(rng, "#dfe9f3", "#f6efe6",
                        [(0.42, 0.07, "#a7b9cf", 1.1), (0.55, 0.08, "#7f95b2", 1.4),
                         (0.68, 0.07, "#566f94", 1.2), (0.82, 0.06, "#2f4670", 1.7)],
                        sun=(0.72, 0.22, 0.05, "#fffaf0"), fog=0.7)


def wp_luna(rng):
    """Noche minimalista: cielo degradado con estrellas, luna con halo y colinas oscuras."""
    x, y = grid()
    img = ridge_layers(rng, "#050b1f", "#27407a",
                       [(0.70, 0.03, "#16254d", 1.0), (0.80, 0.04, "#0d1836", 1.4), (0.91, 0.03, "#070d22", 1.9)],
                       sun=(0.62, 0.28, 0.06, "#eef2ff"), fog=0.45)
    stars = rng.random((SIDE, SIDE), dtype=np.float32)
    pts = np.argwhere(stars > 0.99985)
    star = np.zeros((SIDE, SIDE), np.float32)
    for (py, px) in pts:
        if py > SIDE * 0.62:
            continue
        r = rng.integers(1, 4)
        y0, y1, x0, x1 = max(0, py - 5), min(SIDE, py + 6), max(0, px - 5), min(SIDE, px + 6)
        yy, xx = np.mgrid[y0:y1, x0:x1].astype(np.float32)
        star[y0:y1, x0:x1] = np.maximum(star[y0:y1, x0:x1], np.exp(-((xx - px) ** 2 + (yy - py) ** 2) / (0.6 * r * r)) * rng.uniform(0.5, 1))
    sky = smooth(0.66, 0.5, y)  # solo en el cielo
    return img + (star * sky)[..., None] * 0.9


def wp_bokeh(rng):
    """Bokeh abstracto: círculos desenfocados con borde más claro sobre un degradado profundo."""
    x, y = grid()
    img = mesh([(0.2, 0.1, "#1a0f3d"), (0.9, 0.3, "#3a1a6e"), (0.4, 0.8, "#0d2a5c"), (0.9, 0.95, "#5a1f6f")], x, y, 2.0)
    palette = ["#ff7ab6", "#7ad7ff", "#ffd27a", "#a98bff", "#7affc9"]
    for _ in range(46):
        cx, cy = rng.uniform(-0.05, 1.05, 2)
        r = rng.uniform(0.025, 0.11)
        col = hexc(palette[rng.integers(len(palette))])
        a = rng.uniform(0.10, 0.34)
        d = np.sqrt((x - cx) ** 2 + (y - cy) ** 2) / r
        body = 1.0 - smooth(0.88, 1.0, d)
        ring = np.exp(-((d - 0.93) ** 2) / 0.006) * 0.55
        k = (body * 0.55 + ring) * a
        img = 1.0 - (1.0 - img) * (1.0 - k[..., None] * col)  # mezcla «pantalla»
    return img


def wp_geometrico(rng):
    """Composición geométrica: círculos y arcos translúcidos solapados (estilo Bauhaus suave)."""
    x, y = grid()
    img = mesh([(0.0, 0.0, "#f4ece1"), (1.0, 0.2, "#efe3d2"), (0.3, 1.0, "#e9dcc8")], x, y, 2.0)
    shapes = [(0.25, 0.28, 0.30, "#e76f51", 0.85), (0.72, 0.38, 0.24, "#2a9d8f", 0.80),
              (0.45, 0.70, 0.34, "#264653", 0.88), (0.82, 0.82, 0.20, "#e9c46a", 0.90),
              (0.12, 0.88, 0.13, "#f4a261", 0.90)]
    for (cx, cy, r, col, a) in shapes:
        m = disk(x, y, cx, cy, r, 0.0012) * a
        img = mix(img, np.broadcast_to(hexc(col), img.shape), m)
    # un anillo y un semicírculo para dar ritmo
    d = np.sqrt((x - 0.55) ** 2 + (y - 0.18) ** 2)
    ring = smooth(0.118, 0.12, d) * (1 - smooth(0.14, 0.142, d))
    img = mix(img, np.broadcast_to(hexc("#264653"), img.shape), ring * 0.9)
    half = disk(x, y, 0.2, 0.55, 0.11, 0.0012) * (y > 0.55)
    img = mix(img, np.broadcast_to(hexc("#2a9d8f"), img.shape), half.astype(np.float32))
    return img


def wp_facetas(rng):
    """Cristal facetado: celdas de Voronoi con sombreado suave según una luz diagonal."""
    n = 1152
    x, y = grid(n)
    pts = rng.random((90, 2)).astype(np.float32)
    # Se añaden puntos fuera del cuadro para que los bordes no queden con celdas gigantes.
    pts = np.vstack([pts, rng.uniform(-0.2, 1.2, (40, 2)).astype(np.float32)])
    tone = rng.random(len(pts)).astype(np.float32)
    best = np.full((n, n), 9.0, np.float32)
    idx = np.zeros((n, n), np.int32)
    for i, (px, py) in enumerate(pts):
        d = (x - px) ** 2 + (y - py) ** 2
        m = d < best
        best = np.where(m, d, best)
        idx = np.where(m, i, idx)
    base = mesh([(0.0, 0.0, "#12324f"), (1.0, 0.1, "#2c6e8f"), (0.5, 0.6, "#4aa3a8"), (0.1, 1.0, "#0e2238"),
                 (1.0, 1.0, "#1b4d6e")], x, y, 2.2)
    shade = (0.78 + 0.36 * tone[idx])[..., None]
    img = base * shade
    # aristas finas (diferencia de índice entre vecinos)
    edge = (idx != np.roll(idx, 1, 0)) | (idx != np.roll(idx, 1, 1))
    img = np.where(edge[..., None], img * 0.82 + 0.06, img)
    im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)).resize((SIDE, SIDE), Image.BICUBIC)
    return np.asarray(im, np.float32) / 255.0


def wp_tinta(rng):
    """Tinta/mármol: ruido fractal remapeado a tres tintas, con curvas de nivel tenues."""
    f = fbm(rng, 6, 2)
    g = fbm(rng, 5, 3)
    v = np.clip(0.65 * f + 0.35 * g, 0, 1)
    c0, c1, c2 = hexc("#0e1a2b"), hexc("#27607a"), hexc("#e8d5b0")
    img = mix(mix(np.broadcast_to(c0, (SIDE, SIDE, 3)), np.broadcast_to(c1, (SIDE, SIDE, 3)), smooth(0.2, 0.6, v)),
              np.broadcast_to(c2, (SIDE, SIDE, 3)), smooth(0.62, 0.9, v))
    contour = 0.5 + 0.5 * np.cos(v * 2 * np.pi * 14)
    return img * (0.94 + 0.06 * contour[..., None])


def wp_olas(rng):
    """Olas de papel: bandas sinusoidales apiladas con sombra suave, en tonos pastel."""
    x, y = grid()
    cols = ["#fbe3d6", "#f7c6c0", "#e7a3b5", "#b68fc4", "#7d8fd6", "#5db0d6"]
    img = np.broadcast_to(hexc(cols[0]), (SIDE, SIDE, 3)).copy()
    for i in range(1, len(cols)):
        edge = 0.08 + i * 0.15 + 0.05 * np.sin(2 * np.pi * (1.2 * x + 0.17 * i) + rng.random()) \
            + 0.025 * np.sin(2 * np.pi * (2.7 * x + 0.31 * i) + rng.random())
        mask = smooth(-0.0012, 0.0012, y - edge)
        shadow = np.exp(-np.clip(y - edge, 0, None) * 22.0)  # sombra proyectada por el borde superior de la capa
        layer = np.broadcast_to(hexc(cols[i]), img.shape) * (1.0 - 0.14 * shadow)[..., None]
        img = mix(img, layer, mask)
    return img


FONDOS = [
    ("wp_01_aurora", wp_aurora, 11), ("wp_02_atardecer", wp_atardecer, 12), ("wp_03_oceano", wp_oceano, 13),
    ("wp_04_seda", wp_seda, 14), ("wp_05_dunas", wp_dunas, 15), ("wp_06_montanas", wp_montanas, 16),
    ("wp_07_luna", wp_luna, 17), ("wp_08_bokeh", wp_bokeh, 18), ("wp_09_geometrico", wp_geometrico, 19),
    ("wp_10_facetas", wp_facetas, 20), ("wp_11_tinta", wp_tinta, 21), ("wp_12_olas", wp_olas, 22),
]


def main():
    ap = argparse.ArgumentParser()
    root = pathlib.Path(__file__).resolve().parent.parent
    ap.add_argument("--out", default=str(root / "app/src/main/res/drawable-nodpi"))
    ap.add_argument("--preview", default=None, help="carpeta donde guardar una hoja de contactos PNG")
    a = ap.parse_args()
    out = pathlib.Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    thumbs = []
    total = 0
    for name, fn, seed in FONDOS:
        rng = np.random.default_rng(seed)
        img = finish(np.asarray(fn(rng), np.float32), rng)
        p = out / f"{name}.webp"
        img.save(p, "WEBP", quality=QUALITY, method=6)
        total += p.stat().st_size
        thumbs.append(img.resize((288, 288), Image.LANCZOS))
        print(f"{p.name}: {p.stat().st_size / 1024:.0f} KB")
    print(f"Total: {total / 1048576:.2f} MB")
    if a.preview:
        pv = pathlib.Path(a.preview)
        pv.mkdir(parents=True, exist_ok=True)
        cols = 6
        rows = (len(thumbs) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * 296 + 8, rows * 296 + 8), (30, 30, 30))
        for i, t in enumerate(thumbs):
            sheet.paste(t, (8 + (i % cols) * 296, 8 + (i // cols) * 296))
        sheet.save(pv / "contact-sheet.png")


if __name__ == "__main__":
    main()
