#!/usr/bin/env python3
"""Merge MoDisco Java XMI files into one XMI resource.

The merger assumes each source model is a standalone MoDisco Java resource
with root-anchored EMF path references such as:
  //@compilationUnits.0/@bodyDeclarations.1

When top-level children from multiple roots are appended into one root, only
the first path segment index needs to be shifted by the cumulative number of
previous top-level children for that feature.
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

XREF_RE = re.compile(r"//@([A-Za-z_][A-Za-z0-9_.-]*)\.(\d+)")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input_root", help="Directory containing repo subdirectories with .xmi files")
    parser.add_argument("output_file", help="Merged .xmi file to write")
    parser.add_argument(
        "--model-name",
        default="all-repos-merged",
        help="Value for the merged java:Model name attribute",
    )
    return parser.parse_args()


def discover_xmi_files(input_root: Path) -> list[Path]:
    files: list[Path] = []
    for repo_dir in sorted(path for path in input_root.iterdir() if path.is_dir()):
        for xmi_file in sorted(repo_dir.glob("*.xmi")):
            files.append(xmi_file)
    if not files:
        raise ValueError(f"No .xmi files found under {input_root}")
    return files


def read_header(first_file: Path, merged_name: str) -> tuple[str, str]:
    with first_file.open("r", encoding="utf-8", errors="strict") as handle:
        handle.readline()
        root_line = handle.readline().rstrip("\n")

    if not root_line.startswith("<java:Model "):
        raise ValueError(f"Unexpected root line in {first_file}: {root_line[:80]}")

    if ' name="' in root_line:
        root_line = re.sub(r' name="[^"]*"', f' name="{merged_name}"', root_line, count=1)
    else:
        root_line = root_line[:-1] + f' name="{merged_name}">'

    return '<?xml version="1.0" encoding="UTF-8"?>', root_line


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def count_top_level_features(xmi_file: Path) -> Counter[str]:
    counts: Counter[str] = Counter()
    depth = 0
    for event, elem in ET.iterparse(xmi_file, events=("start", "end")):
        if event == "start":
            if depth == 1:
                counts[local_name(elem.tag)] += 1
            depth += 1
        else:
            depth -= 1
            if depth == 0:
                elem.clear()
    return counts


def shift_root_refs(value: str, offsets: Counter[str]) -> str:
    def replace(match: re.Match[str]) -> str:
        feature = match.group(1)
        index = int(match.group(2))
        return f"//@{feature}.{index + offsets.get(feature, 0)}"

    return XREF_RE.sub(replace, value)


def rewrite_attributes(elem: ET.Element, offsets: Counter[str]) -> None:
    for key, value in list(elem.attrib.items()):
        if "//@" in value:
            elem.attrib[key] = shift_root_refs(value, offsets)


def merge_models(input_root: Path, output_file: Path, merged_name: str) -> list[Path]:
    xmi_files = discover_xmi_files(input_root)
    counts_by_file = {xmi_file: count_top_level_features(xmi_file) for xmi_file in xmi_files}
    xml_decl, root_line = read_header(xmi_files[0], merged_name)

    output_file.parent.mkdir(parents=True, exist_ok=True)
    cumulative_offsets: Counter[str] = Counter()

    with output_file.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(xml_decl + "\n")
        handle.write(root_line + "\n")

        for xmi_file in xmi_files:
            current_offsets = Counter(cumulative_offsets)
            depth = 0
            root: ET.Element | None = None

            for event, elem in ET.iterparse(xmi_file, events=("start", "end")):
                if event == "start":
                    if depth == 0:
                        root = elem
                    else:
                        rewrite_attributes(elem, current_offsets)
                    depth += 1
                    continue

                depth -= 1
                if depth == 1:
                    serialized = ET.tostring(elem, encoding="unicode", short_empty_elements=True)
                    handle.write(serialized)
                    if not serialized.endswith("\n"):
                        handle.write("\n")
                    if root is not None:
                        root.remove(elem)
                elif depth == 0:
                    elem.clear()

            cumulative_offsets.update(counts_by_file[xmi_file])

        handle.write("</java:Model>\n")

    return xmi_files


def main() -> int:
    args = parse_args()
    input_root = Path(args.input_root).resolve()
    output_file = Path(args.output_file).resolve()

    merged_files = merge_models(input_root, output_file, args.model_name)
    print(f"merged_files={len(merged_files)}")
    print(f"output={output_file}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
