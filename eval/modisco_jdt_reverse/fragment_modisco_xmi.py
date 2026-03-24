#!/usr/bin/env python3

from __future__ import annotations

import argparse
import posixpath
import re
import shutil
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
import xml.etree.ElementTree as ET

XMI_NS = "http://www.omg.org/XMI"
XSI_NS = "http://www.w3.org/2001/XMLSchema-instance"
JAVA_NS = "http://www.eclipse.org/MoDisco/Java/0.2.incubation/java"

ET.register_namespace("xmi", XMI_NS)
ET.register_namespace("xsi", XSI_NS)
ET.register_namespace("java", JAVA_NS)

REF_RE = re.compile(r"^//@([^/.]+)\.(\d+)(.*)$")
DEFAULT_ROOT_CLASSES = {
    "ownedElements": "Package",
    "unresolvedItems": "UnresolvedItem",
    "compilationUnits": "CompilationUnit",
    "classFiles": "ClassFile",
    "archives": "Archive",
}


@dataclass(frozen=True)
class FragmentMeta:
    feature: str
    index: int
    rel_path: Path
    xsi_type: str | None


def qname(namespace: str, local_name: str) -> str:
    return f"{{{namespace}}}{local_name}"


def local_name(tag: str) -> str:
    if tag.startswith("{"):
        return tag.rsplit("}", 1)[1]
    return tag


def ensure_clean_dir(path: Path) -> None:
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True, exist_ok=True)


def collect_fragments(input_path: Path) -> tuple[str, list[FragmentMeta], dict[str, int]]:
    counters: dict[str, int] = defaultdict(int)
    fragments: list[FragmentMeta] = []

    context = ET.iterparse(input_path, events=("start", "end"))
    stack: list[ET.Element] = []
    root: ET.Element | None = None
    model_name: str | None = None

    for event, elem in context:
        if event == "start":
            stack.append(elem)
            if root is None:
                root = elem
                model_name = elem.get("name", input_path.stem)
            continue

        if len(stack) == 2:
            feature = local_name(elem.tag)
            index = counters[feature]
            counters[feature] += 1
            rel_path = Path("fragments") / feature / f"{feature}-{index:05d}.xmi"
            fragments.append(
                FragmentMeta(
                    feature=feature,
                    index=index,
                    rel_path=rel_path,
                    xsi_type=elem.get(qname(XSI_NS, "type")),
                )
            )
            assert root is not None
            root.remove(elem)

        stack.pop()

    if model_name is None:
        raise ValueError(f"No root model found in {input_path}")

    return model_name, fragments, counters


def rewrite_reference_token(token: str, current: FragmentMeta, lookup: dict[tuple[str, int], FragmentMeta]) -> str:
    if not token.startswith("//@"):
        return token

    match = REF_RE.match(token)
    if match is None:
        return token

    target_feature = match.group(1)
    target_index = int(match.group(2))
    remainder = match.group(3)
    target = lookup.get((target_feature, target_index))
    if target is None:
        return token

    target_fragment = "#/" + remainder
    if target.feature == current.feature and target.index == current.index:
        return target_fragment

    relative_target = posixpath.relpath(target.rel_path.as_posix(), current.rel_path.parent.as_posix())
    return f"{relative_target}{target_fragment}"


def rewrite_attribute_value(value: str, current: FragmentMeta, lookup: dict[tuple[str, int], FragmentMeta]) -> str:
    if "//@" not in value:
        return value

    parts = re.split(r"(\s+)", value)
    rewritten = [rewrite_reference_token(part, current, lookup) if part and not part.isspace() else part for part in parts]
    return "".join(rewritten)


def rewrite_subtree_references(elem: ET.Element, current: FragmentMeta, lookup: dict[tuple[str, int], FragmentMeta]) -> None:
    for candidate in elem.iter():
        if not candidate.attrib:
            continue
        for attr_name, attr_value in list(candidate.attrib.items()):
            candidate.set(attr_name, rewrite_attribute_value(attr_value, current, lookup))


def build_model_root(model_name: str) -> ET.Element:
    return ET.Element(qname(JAVA_NS, "Model"), {qname(XMI_NS, "version"): "2.0", "name": model_name})


def root_class_local_name(meta: FragmentMeta, child: ET.Element) -> str:
    xsi_type = child.get(qname(XSI_NS, "type")) or meta.xsi_type
    if xsi_type:
        return xsi_type.split(":", 1)[1] if ":" in xsi_type else xsi_type

    default = DEFAULT_ROOT_CLASSES.get(meta.feature)
    if default:
        return default

    raise ValueError(f"Cannot determine concrete root class for feature '{meta.feature}'")


def retag_root_object(meta: FragmentMeta, child: ET.Element) -> None:
    child.tag = qname(JAVA_NS, root_class_local_name(meta, child))
    child.attrib[qname(XMI_NS, "version")] = "2.0"
    child.attrib.pop(qname(XSI_NS, "type"), None)


def add_package_model_href(meta: FragmentMeta, child: ET.Element, root_filename: str) -> None:
    if meta.feature != "ownedElements" or local_name(child.tag) != "Package":
        return

    if any(local_name(existing.tag) == "model" for existing in child):
        return

    relative_root = posixpath.relpath(root_filename, meta.rel_path.parent.as_posix())
    child.insert(0, ET.Element("model", {"href": f"{relative_root}#/"}))


def write_fragment(fragment_dir: Path, model_name: str, meta: FragmentMeta, child: ET.Element, root_filename: str) -> None:
    output_path = fragment_dir / meta.rel_path
    output_path.parent.mkdir(parents=True, exist_ok=True)

    retag_root_object(meta, child)
    add_package_model_href(meta, child, root_filename)
    ET.ElementTree(child).write(output_path, encoding="UTF-8", xml_declaration=True)


def write_root_model(input_path: Path, output_dir: Path, root_filename: str) -> Path:
    root_path = output_dir / root_filename
    shutil.copyfile(input_path, root_path)
    return root_path


def write_metadata(output_dir: Path, root_filename: str, fragments: list[FragmentMeta], feature_counts: dict[str, int]) -> None:
    (output_dir / "ROOT_MODEL.txt").write_text(f"{root_filename}\n", encoding="utf-8")

    manifest_lines = ["feature,index,relative_path,xsi_type"]
    for fragment in fragments:
        xsi_type = fragment.xsi_type or ""
        manifest_lines.append(f"{fragment.feature},{fragment.index},{fragment.rel_path.as_posix()},{xsi_type}")
    (output_dir / "fragments-manifest.csv").write_text("\n".join(manifest_lines) + "\n", encoding="utf-8")

    summary_lines = [
        "# Fragmented MoDisco Model",
        "",
        f"- Root model: `{root_filename}`",
        f"- Total fragments: `{len(fragments)}`",
        "",
        "## Feature Counts",
        "",
    ]
    for feature in sorted(feature_counts):
        summary_lines.append(f"- `{feature}`: `{feature_counts[feature]}`")
    (output_dir / "README.md").write_text("\n".join(summary_lines) + "\n", encoding="utf-8")


def fragment_model(input_path: Path, output_dir: Path, root_filename: str, model_name_override: str | None) -> Path:
    model_name, fragments, feature_counts = collect_fragments(input_path)
    model_name = model_name_override or model_name
    lookup = {(fragment.feature, fragment.index): fragment for fragment in fragments}

    ensure_clean_dir(output_dir)

    context = ET.iterparse(input_path, events=("start", "end"))
    stack: list[ET.Element] = []
    root: ET.Element | None = None
    position = 0

    for event, elem in context:
        if event == "start":
            stack.append(elem)
            if root is None:
                root = elem
            continue

        if len(stack) == 2:
            meta = fragments[position]
            position += 1
            rewrite_subtree_references(elem, meta, lookup)
            write_fragment(output_dir, model_name, meta, elem, root_filename)
            assert root is not None
            root.remove(elem)

        stack.pop()

    if position != len(fragments):
        raise ValueError(f"Fragment count mismatch: wrote {position}, expected {len(fragments)}")

    root_path = write_root_model(input_path, output_dir, root_filename)
    write_metadata(output_dir, root_filename, fragments, feature_counts)
    return root_path


def main() -> int:
    parser = argparse.ArgumentParser(description="Split a merged MoDisco Java XMI into top-level fragments.")
    parser.add_argument("input_xmi", type=Path, help="Merged input XMI file")
    parser.add_argument("output_dir", type=Path, help="Directory where the fragmented model set will be written")
    parser.add_argument("--root-filename", default="all-repos-merged.root.xmi", help="Filename for the root model")
    parser.add_argument("--model-name", default=None, help="Optional model name override for the generated root model")
    args = parser.parse_args()

    root_path = fragment_model(args.input_xmi, args.output_dir, args.root_filename, args.model_name)
    print(f"root_model={root_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
