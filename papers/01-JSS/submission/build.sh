#!/usr/bin/env bash
# Builds the upload for Editorial Manager, which accepts no subfolders: main.tex,
# the figures that main.tex includes, the class, the bibliography style, the
# bibliography, the highlights and the biography all go into one flat folder.
# The script then compiles the paper there, so the folder proves that it builds
# on its own, and it fails when a \todo note is left in the text.
#
#   papers/01-JSS/submission/build.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
PAPER="$(cd "$HERE/../paper" && pwd)"
JSS="$(cd "$HERE/.." && pwd)"
OUT="$HERE/editorial-manager"

rm -rf "$OUT"
mkdir -p "$OUT"
cp "$PAPER/main.tex" "$PAPER/elsarticle.cls" "$PAPER/elsarticle-num.bst" "$PAPER/references.bib" \
   "$PAPER/highlights.txt" "$JSS/biography.txt" "$OUT/"

# Every figure that main.tex includes, without the figures/ prefix that
# \graphicspath already removes.
grep -o '\\includegraphics\(\[[^]]*\]\)\?{[^}]*}' "$PAPER/main.tex" \
  | sed 's/.*{\(.*\)}/\1/' | sort -u | while read -r figure; do
    cp "$PAPER/figures/$figure" "$OUT/$figure"
  done

if grep -n '\\todo{' "$OUT/main.tex"; then
  echo "main.tex still has \\todo notes; remove them before submission" >&2
  exit 1
fi

(cd "$OUT" && latexmk -pdf -interaction=nonstopmode -halt-on-error main.tex > build.log 2>&1)
# main.spl is an auxiliary file of elsarticle that latexmk -c leaves behind
(cd "$OUT" && latexmk -c > /dev/null 2>&1 && rm -f build.log main.bbl.bak main.spl)
ls -1 "$OUT"
