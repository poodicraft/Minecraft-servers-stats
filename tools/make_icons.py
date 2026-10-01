"""Generates ServerScope's pixel-art launcher icon (adaptive vectors + legacy PNGs) and
in-app pixel drawables. Pure Python: no imaging libraries required."""
import math
import os
import struct
import zlib

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")

PAL = {
    "g1": "#7CC744", "g2": "#5EA634", "g3": "#3F7F24",
    "d1": "#9A6A45", "d2": "#7A5034", "d3": "#5A3A25",
    "w": "#F4F1EA", "l": "#9BE7F2", "l2": "#6CCFE0", "h": "#4A4644", "h2": "#2E2B29",
    "o": "#0F0D0C",
}

BLOCK = [
    "g1 g2 g1 g1 g2 g1 g2 g1 g1 g2 g1 g2",
    "g2 g1 g3 g2 g1 g2 g1 g3 g2 g1 g2 g1",
    "g2 g3 d2 g2 g2 g3 g2 d2 g3 g2 g3 g2",
    "d2 d1 d2 g3 d2 d1 g3 d2 d1 d2 g3 d2",
    "d1 d2 d3 d2 d1 d2 d2 d3 d2 d1 d2 d1",
    "d2 d3 d2 d1 d2 d3 d1 d2 d2 d3 d2 d2",
    "d2 d1 d2 d2 d3 d2 d2 d1 d3 d2 d1 d3",
    "d3 d2 d1 d2 d2 d1 d3 d2 d2 d2 d2 d2",
    "d2 d2 d3 d1 d2 d2 d2 d3 d1 d2 d3 d1",
    "d1 d2 d2 d2 d3 d1 d2 d2 d2 d1 d2 d2",
    "d2 d3 d1 d2 d2 d2 d3 d1 d2 d3 d2 d3",
    "d3 d2 d2 d3 d1 d2 d2 d2 d3 d2 d2 d2",
]


# Generic placeholder face shown while player heads load.
HEAD_PAL = {"h": "#4A3424", "s": "#B98B68", "w": "#F2F2F2", "e": "#3D4A7A", "n": "#9A6F52", "m": "#6E4433"}
HEAD = [
    "h h h h h h h h", "h h h h h h h h", "h s s s s s s h", "s s s s s s s s",
    "s w e s s e w s", "s s s n n s s s", "s s m m m m s s", "s s s s s s s s",
]


def block_grid():
    return [row.split() for row in BLOCK]


def launcher_grid():
    """18x18: grass block with a magnifying glass, plus a dark outline."""
    n = 18
    g = [[None] * n for _ in range(n)]
    for y, row in enumerate(block_grid()):
        for x, c in enumerate(row):
            g[y + 1][x + 1] = c
    cx, cy = 11.5, 11.5
    for y in range(n):
        for x in range(n):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d < 2.6:
                g[y][x] = "l2" if (x + y) % 5 == 0 or (x - y) == 0 else "l"
            elif d < 4.0:
                g[y][x] = "w"
    for (x, y) in [(14, 14), (15, 14), (14, 15), (15, 15), (16, 15), (15, 16), (16, 16)]:
        g[y][x] = "h" if (x + y) % 2 == 0 else "h2"
    out = [r[:] for r in g]
    for y in range(n):
        for x in range(n):
            if g[y][x] is None and any(
                0 <= x + dx < n and 0 <= y + dy < n and g[y + dy][x + dx] not in (None, "o")
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
            ):
                out[y][x] = "o"
    return out


def vector_xml(grid, size_dp, viewport, offset, scale, colors=None):
    by_color = {}
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            if c is None:
                continue
            fill = colors(c) if colors else PAL[c]
            if fill is None:
                continue
            px, py = offset + x * scale, offset + y * scale
            by_color.setdefault(fill, []).append(
                f"M{px:g},{py:g}h{scale:g}v{scale:g}h{-scale:g}z")
    paths = "\n".join(
        f'    <path android:fillColor="{fill}" android:pathData="{"".join(parts)}" />'
        for fill, parts in by_color.items())
    return (f'<?xml version="1.0" encoding="utf-8"?>\n'
            f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{size_dp}dp"\n    android:height="{size_dp}dp"\n'
            f'    android:viewportWidth="{viewport}"\n    android:viewportHeight="{viewport}">\n'
            f'{paths}\n</vector>\n')


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def write_png(path, w, h, pixels):
    raw = b"".join(b"\x00" + bytes(v for p in pixels[y * w:(y + 1) * w] for v in p) for y in range(h))
    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def legacy_png(path, size, grid, round_shape):
    bg = hex_rgb("#1E1B18")
    edge = hex_rgb("#3A342E")
    cell = size // 24
    art = cell * len(grid)
    off = (size - art) // 2
    radius = size * 0.2
    px = []
    for y in range(size):
        for x in range(size):
            cov = 0
            for sy in range(4):
                for sx in range(4):
                    fx, fy = x + (sx + 0.5) / 4, y + (sy + 0.5) / 4
                    if round_shape:
                        inside = math.hypot(fx - size / 2, fy - size / 2) <= size / 2 - 0.5
                    else:
                        m = 0.5
                        qx = max(m + radius - fx, 0, fx - (size - m - radius))
                        qy = max(m + radius - fy, 0, fy - (size - m - radius))
                        inside = math.hypot(qx, qy) <= radius and m <= fx <= size - m and m <= fy <= size - m
                    cov += inside
            a = int(255 * cov / 16)
            color = bg
            gx, gy = (x - off) // cell, (y - off) // cell
            if 0 <= x - off < art and 0 <= y - off < art and grid[gy][gx] is not None:
                color = hex_rgb(PAL[grid[gy][gx]])
            elif a < 255 and a > 0:
                color = edge
            px.append(color + (a,))
    write_png(path, size, size, px)


def main():
    lg = launcher_grid()
    scale = 2.75
    off = (108 - scale * 18) / 2
    with open(f"{RES}/drawable/ic_launcher_foreground.xml", "w") as f:
        f.write(vector_xml(lg, 108, 108, off, scale))
    mono = [[None if c in (None, "o", "l", "l2") else "x" for c in row] for row in lg]
    with open(f"{RES}/drawable/ic_launcher_monochrome.xml", "w") as f:
        f.write(vector_xml(mono, 108, 108, off, scale, colors=lambda c: "#FF000000"))
    with open(f"{RES}/drawable/ic_server_default.xml", "w") as f:
        f.write(vector_xml(block_grid(), 64, 12, 0, 1))
    with open(f"{RES}/drawable/ic_head_placeholder.xml", "w") as f:
        f.write(vector_xml([row.split() for row in HEAD], 64, 8, 0, 1, colors=HEAD_PAL.get))
    for dpi, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        legacy_png(f"{RES}/mipmap-{dpi}/ic_launcher.png", size, lg, round_shape=False)
        legacy_png(f"{RES}/mipmap-{dpi}/ic_launcher_round.png", size, lg, round_shape=True)
    print("icons written")


if __name__ == "__main__":
    main()
