#!/usr/bin/env bash
# Rebuilds every figure PDF of the manuscript from its source in this folder.
# All text in the figures is Times New Roman, which has to be installed as a
# system font.
# PlantUML diagrams (*.puml) take the font from style.iuml. PlantUML writes SVG,
# and rsvg-convert turns the SVG into a PDF with the font embedded. PlantUML's
# own EPS output would place the letters of Times New Roman on whole pixels and
# space them unevenly. rsvg-convert runs at 72 dpi, so one pixel of a diagram
# becomes one point.
# PlantUML measures the text with the fonts that Java finds, and rsvg-convert
# draws the text with the fonts that fontconfig finds. Both fall back to another
# font without an error, so the script stops first if either one lacks Times
# New Roman.
# TikZ and pgfplots figures (*.tex) load the font with fontspec, so they compile
# with LuaLaTeX. Their math uses TeX Gyre Termes Math, except for the digits,
# which come from Times New Roman.
# Requires plantuml, rsvg-convert, fc-list, lualatex and latexmk on the PATH.
set -euo pipefail
cd "$(dirname "$0")"

if ! plantuml -printfonts 2>/dev/null | grep "family=Times New Roman" > /dev/null; then
  echo "Java does not find Times New Roman, so PlantUML would use another font." >&2
  exit 1
fi
if [ -z "$(fc-list "Times New Roman")" ]; then
  echo "fontconfig does not find Times New Roman, so rsvg-convert would use another font." >&2
  exit 1
fi

for source in *.puml; do
  name="${source%.puml}"
  plantuml -tsvg "$source"
  rsvg-convert --format=pdf --dpi-x=72 --dpi-y=72 --output="$name.pdf" "$name.svg"
  rm -f "$name.svg"
done

for source in *.tex; do
  latexmk -lualatex -interaction=nonstopmode -halt-on-error "$source" > /dev/null
  latexmk -c "$source" > /dev/null
done

ls -1 *.pdf
