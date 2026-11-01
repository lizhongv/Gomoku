"""生成应用图标：木色棋盘 + 黑白棋子，输出 PNG 与 Windows ICO。

用法：python packaging/make-icon.py
"""

from PIL import Image, ImageDraw, ImageFilter

SIZE = 512
image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)

# 圆角木色底板
draw.rounded_rectangle((16, 16, SIZE - 16, SIZE - 16), radius=96, fill=(214, 175, 112, 255))
draw.rounded_rectangle((36, 36, SIZE - 36, SIZE - 36), radius=80, outline=(140, 104, 56, 255), width=6)

# 棋盘格与星位
margin, cell = 110, 42
for i in range(9):
    pos = margin + i * cell
    draw.line((margin, pos, margin + 8 * cell, pos), fill=(108, 75, 38, 255), width=3)
    draw.line((pos, margin, pos, margin + 8 * cell), fill=(108, 75, 38, 255), width=3)
for row in (2, 6):
    for col in (2, 6):
        x, y = margin + col * cell, margin + row * cell
        draw.ellipse((x - 5, y - 5, x + 5, y + 5), fill=(70, 48, 22, 255))

def stone(cx, cy, radius, black):
    shadow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.ellipse((cx - radius + 4, cy - radius + 8, cx + radius + 4, cy + radius + 8), fill=(0, 0, 0, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(6))
    image.alpha_composite(shadow)
    d = ImageDraw.Draw(image)
    if black:
        d.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=(28, 28, 28, 255))
        d.ellipse((cx - radius * 0.45, cy - radius * 0.55, cx - radius * 0.05, cy - radius * 0.15),
                  fill=(105, 105, 105, 255))
    else:
        d.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=(246, 246, 246, 255))
        d.ellipse((cx - radius * 0.45, cy - radius * 0.55, cx - radius * 0.05, cy - radius * 0.15),
                  fill=(255, 255, 255, 255))

stone(margin + 3 * cell, margin + 3 * cell, 46, True)
stone(margin + 5 * cell, margin + 4 * cell, 46, False)

image.save("packaging/icon.png")
image.save("packaging/icon.ico", sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
print("生成 packaging/icon.png 与 packaging/icon.ico")
