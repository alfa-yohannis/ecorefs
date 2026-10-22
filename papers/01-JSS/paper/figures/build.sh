#!/usr/bin/env bash
# Rebuilds every figure PDF of the manuscript from its source in this folder.
# PlantUML diagrams (*.puml) go through EPS, so the PDFs hold vector outlines
# and need no embedded fonts. TikZ and pgfplots figures (*.tex) compile with
# pdfLaTeX. Requires plantuml, epstopdf and latexmk on the PATH.
set -euo pipefail
cd "$(dirname "$0")"

for source in *.puml; do
  name="${source%.puml}"
  plantuml -teps "$source"
  epstopdf "$name.eps"
  rm -f "$name.eps"
done

for source in *.tex; do
  latexmk -pdf -interaction=nonstopmode -halt-on-error "$source" > /dev/null
  latexmk -c "$source" > /dev/null
done

ls -1 *.pdf
