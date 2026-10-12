"""Package existing generated artwork into Android launcher sizes. Requires Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageOps

ARTWORK = Path(__file__).resolve().parent
RES = ARTWORK.parent / "app/src/main/res"
source = Image.open(ARTWORK / "gallery-icon-source.png").convert("RGBA")
assert source.getchannel("A").getextrema()[0] == 0, "Foreground needs real transparency"
bounds = source.getchannel("A").point(lambda value: 255 if value > 5 else 0).getbbox()
assert bounds is not None
mark = ImageOps.contain(source.crop(bounds), (488, 488), Image.Resampling.LANCZOS)
foreground = Image.new("RGBA", (512, 512))
foreground.alpha_composite(mark, ((512 - mark.width) // 2, (512 - mark.height) // 2))
foreground_path = RES / "drawable-nodpi/gallery_icon_foreground.png"
foreground_path.parent.mkdir(parents=True, exist_ok=True)
foreground.save(foreground_path, optimize=True)

def background(size):
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    start, end = (33, 27, 54), (79, 55, 139)
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * max(1, size - 1))
            pixels[x, y] = tuple(round(a + (b - a) * t) for a, b in zip(start, end)) + (255,)
    return image

def launcher(size, circular=False):
    image = background(size)
    emblem_size = round(size * 52 / 72)
    emblem = foreground.resize((emblem_size, emblem_size), Image.Resampling.LANCZOS)
    image.alpha_composite(emblem, ((size - emblem_size) // 2, (size - emblem_size) // 2))
    mask = Image.new("L", (size, size))
    draw = ImageDraw.Draw(mask)
    if circular:
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    else:
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=round(size * 0.22), fill=255)
    image.putalpha(mask)
    return image

for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    for circular, name in [(False, "ic_launcher"), (True, "ic_launcher_round")]:
        path = RES / f"mipmap-{density}/{name}.webp"
        path.parent.mkdir(parents=True, exist_ok=True)
        launcher(size, circular).save(path, "WEBP", lossless=True)
        print(f"Exported {path.relative_to(RES.parent.parent.parent)} ({size}x{size})")

# Companion preview of the vector's geometric mark, using the same 108 dp coordinates.
themed = Image.new("RGBA", (432, 432), (238, 231, 250, 255))
draw = ImageDraw.Draw(themed)
ink = (79, 55, 139, 255)
points = [(32, 63), (29, 39), (29, 34), (33, 33), (65, 29), (70, 29), (71, 33), (72, 37)]
draw.line([(x * 4, y * 4) for x, y in points], fill=ink, width=12, joint="curve")
draw.rounded_rectangle((140, 148, 312, 304), radius=24, outline=ink, width=12)
draw.polygon([(x * 4, y * 4) for x, y in [(40, 68), (51, 52), (60, 63), (67, 57), (73, 68)]], fill=ink)
draw.ellipse((252, 168, 284, 200), fill=ink)
themed = themed.crop((72, 72, 360, 360))
mask = Image.new("L", (288, 288))
ImageDraw.Draw(mask).ellipse((0, 0, 287, 287), fill=255)
themed.putalpha(mask)
preview = Image.new("RGB", (1020, 420), "#F7F5FC")
draw = ImageDraw.Draw(preview)
font_path = Path("C:/Windows/Fonts/segoeui.ttf")
font = ImageFont.truetype(str(font_path), 24) if font_path.exists() else ImageFont.load_default()
for x, image, label in [(30, launcher(288, False), "Rounded square"),
                        (366, launcher(288, True), "Circular"),
                        (702, themed, "Themed monochrome")]:
    preview.paste(image, (x, 36), image)
    draw.text((x + 8, 345), label, fill="#332D41", font=font)
preview.save(ARTWORK / "gallery-icon-preview.png", optimize=True)
