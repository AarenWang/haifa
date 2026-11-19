#!/usr/bin/env python3
"""Render a CJK-safe comparison dashboard from a small JSON specification."""

import argparse
import json
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


CJK_FONT_CANDIDATES = (
    "C:/Windows/Fonts/msyh.ttc",
    "C:/Windows/Fonts/msyhbd.ttc",
    "C:/Windows/Fonts/simhei.ttf",
    "C:/Windows/Fonts/simsun.ttc",
    "C:/Windows/Fonts/Deng.ttf",
    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
    "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
    "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
    "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
    "/System/Library/Fonts/PingFang.ttc",
)

PALETTE = ((139, 69, 19), (166, 91, 52), (205, 133, 63), (222, 184, 135), (153, 112, 82))


def glyph_signature(font, character):
    mask = font.getmask(character)
    return mask.size, bytes(mask)


def supports_cjk(font):
    missing = glyph_signature(font, "\U0010ffff")
    return all(glyph_signature(font, character) != missing for character in "中文图表")


def resolve_font(explicit_path=None):
    candidates = []
    if explicit_path:
        candidates.append(explicit_path)
    if os.getenv("DEERFLOW_CJK_FONT"):
        candidates.append(os.environ["DEERFLOW_CJK_FONT"])
    candidates.extend(CJK_FONT_CANDIDATES)

    checked = []
    for candidate in candidates:
        path = Path(candidate).expanduser()
        checked.append(str(path))
        if not path.is_file():
            continue
        try:
            font = ImageFont.truetype(str(path), 24)
            if supports_cjk(font):
                return path
        except OSError:
            continue
    raise RuntimeError(
        "No usable CJK font was found. Set DEERFLOW_CJK_FONT or --font-file to a font "
        "such as C:/Windows/Fonts/msyh.ttc or NotoSansCJK-Regular.ttc. Checked: "
        + ", ".join(checked)
    )


def load_font(font_path, size):
    font = ImageFont.truetype(str(font_path), size)
    if not supports_cjk(font):
        raise RuntimeError(f"Font does not provide required CJK glyphs: {font_path}")
    return font


def centered_x(draw, text, font, left, width):
    box = draw.textbbox((0, 0), text, font=font)
    return left + (width - (box[2] - box[0])) // 2


def read_spec(path):
    if path == "-":
        return json.load(sys.stdin)
    with open(path, "r", encoding="utf-8") as stream:
        return json.load(stream)


def validate_spec(spec):
    if not isinstance(spec, dict) or not str(spec.get("title", "")).strip():
        raise ValueError("spec.title is required")
    panels = spec.get("panels")
    if not isinstance(panels, list) or not panels:
        raise ValueError("spec.panels must be a non-empty array")
    for index, panel in enumerate(panels):
        labels = panel.get("labels")
        values = panel.get("values")
        if not isinstance(labels, list) or not isinstance(values, list) or not labels:
            raise ValueError(f"panels[{index}] requires non-empty labels and values arrays")
        if len(labels) != len(values):
            raise ValueError(f"panels[{index}] labels and values must have equal length")
        if any(not isinstance(value, (int, float)) for value in values):
            raise ValueError(f"panels[{index}] values must be numeric")


def render(spec, output_path, font_path):
    validate_spec(spec)
    panels = spec["panels"]
    width = max(900, int(spec.get("width", 1400)))
    panel_height = 210
    top = 125
    footer_height = 80
    height = max(500, top + len(panels) * panel_height + footer_height)

    title_font = load_font(font_path, 34)
    panel_font = load_font(font_path, 23)
    label_font = load_font(font_path, 19)
    value_font = load_font(font_path, 17)
    footer_font = load_font(font_path, 15)

    image = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(image)
    ink = (69, 64, 55)
    muted = (105, 84, 68)
    grid = (231, 224, 214)
    margin = 70

    title = str(spec["title"])
    draw.text((centered_x(draw, title, title_font, 0, width), 35), title, fill=ink, font=title_font)

    for panel_index, panel in enumerate(panels):
        panel_top = top + panel_index * panel_height
        panel_title = str(panel.get("title", ""))
        draw.text((margin, panel_top), panel_title, fill=ink, font=panel_font)

        labels = [str(label) for label in panel["labels"]]
        values = [float(value) for value in panel["values"]]
        max_value = float(panel.get("max_value") or max(values) or 1)
        if max_value <= 0:
            raise ValueError(f"panels[{panel_index}].max_value must be positive")
        unit = str(panel.get("unit", "%"))
        chart_top = panel_top + 45
        baseline = chart_top + 115
        available = width - 2 * margin
        slot_width = available / len(labels)
        bar_width = max(24, int(slot_width * 0.62))

        draw.line((margin, baseline, width - margin, baseline), fill=grid, width=2)
        for item_index, (label, value) in enumerate(zip(labels, values)):
            slot_left = int(margin + item_index * slot_width)
            bar_left = slot_left + (int(slot_width) - bar_width) // 2
            bar_height = max(4, int(min(value / max_value, 1.0) * 100))
            bar_top = baseline - bar_height
            color = PALETTE[item_index % len(PALETTE)]
            draw.rounded_rectangle(
                (bar_left, bar_top, bar_left + bar_width, baseline), radius=5, fill=color
            )
            value_text = f"{value:.1f}{unit}"
            draw.text(
                (centered_x(draw, value_text, value_font, slot_left, int(slot_width)), bar_top - 27),
                value_text,
                fill=ink,
                font=value_font,
            )
            draw.text(
                (centered_x(draw, label, label_font, slot_left, int(slot_width)), baseline + 12),
                label,
                fill=muted,
                font=label_font,
            )

    footer = str(spec.get("footer", "")).strip()
    if footer:
        draw.text((margin, height - 45), footer, fill=(130, 110, 90), font=footer_font)

    destination = Path(output_path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination, "PNG")
    if not destination.is_file() or destination.stat().st_size == 0:
        raise RuntimeError(f"Renderer did not create a non-empty output: {destination}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--spec-file", required=True, help="UTF-8 JSON specification, or - for stdin")
    parser.add_argument("--output-file", required=True, help="Destination PNG")
    parser.add_argument("--font-file", help="Optional explicit CJK font path")
    args = parser.parse_args()

    try:
        spec = read_spec(args.spec_file)
        font_path = resolve_font(args.font_file)
        render(spec, args.output_file, font_path)
        print(f"Chart generated: {args.output_file}")
        print(f"CJK font: {font_path}")
    except Exception as error:
        print(f"Chart generation failed: {error}", file=sys.stderr)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
