"""Analyses the resource graph of the partitioned MoDisco model for a cascade save.

The script reads every XMI resource of the partitioned model of the MoDisco case
study and collects the serialized cross-references: every href that names
another resource. An edge from r to q means that r stores the location of q, so
in CID mode r would store the CID of q and need a new save after every change of
q. The script reports the strongly connected components of the graph, the
affected set A(t) of three kinds of change (a compilation unit, the root model
and a shared resource), and what a change costs in the layered publication of
the case study, where references inside the published directories are relative
paths and no cascade is needed.

    python3 -I resource_graph.py <partition-folder> <results-folder>
"""

import collections
import csv
import json
import os
import re
import sys
import urllib.parse

HREF = re.compile(rb'href="([^"#]*)#')


def list_resources(root):
    """Returns the XMI resources of the partition, relative to its folder."""
    resources = []
    for folder, _, files in os.walk(root):
        for name in files:
            if name.endswith(".xmi"):
                relative = os.path.relpath(os.path.join(folder, name), root)
                if relative.startswith("component-compare"):
                    continue
                resources.append(relative)
    return sorted(resources)


def read_edges(root, resources):
    """Returns the referenced resources of every resource and the href counts."""
    known = set(resources)
    edges = {}
    href_counts = collections.Counter()
    for resource in resources:
        targets = set()
        base = os.path.dirname(resource)
        with open(os.path.join(root, resource), "rb") as handle:
            for match in HREF.finditer(handle.read()):
                location = urllib.parse.unquote(match.group(1).decode("utf-8"))
                if not location:
                    continue
                target = os.path.normpath(os.path.join(base, location))
                href_counts[resource] += 1
                if target != resource and target in known:
                    targets.add(target)
        edges[resource] = targets
    return edges, href_counts


def strongly_connected_components(nodes, edges):
    """Tarjan's algorithm without recursion; returns the components as lists."""
    index = {}
    low = {}
    on_stack = set()
    stack = []
    components = []
    counter = 0
    for start in nodes:
        if start in index:
            continue
        work = [(start, iter(sorted(edges[start])))]
        index[start] = low[start] = counter
        counter += 1
        stack.append(start)
        on_stack.add(start)
        while work:
            node, children = work[-1]
            advanced = False
            for child in children:
                if child not in index:
                    index[child] = low[child] = counter
                    counter += 1
                    stack.append(child)
                    on_stack.add(child)
                    work.append((child, iter(sorted(edges[child]))))
                    advanced = True
                    break
                if child in on_stack:
                    low[node] = min(low[node], index[child])
            if advanced:
                continue
            work.pop()
            if work:
                parent = work[-1][0]
                low[parent] = min(low[parent], low[node])
            if low[node] == index[node]:
                component = []
                while True:
                    member = stack.pop()
                    on_stack.discard(member)
                    component.append(member)
                    if member == node:
                        break
                components.append(component)
    return components


def affected_set(target, reverse):
    """Returns the target and every resource with a path to it."""
    affected = {target}
    pending = [target]
    while pending:
        current = pending.pop()
        for source in reverse[current]:
            if source not in affected:
                affected.add(source)
                pending.append(source)
    return affected


def component_of(resource):
    """Returns the component id of a compilation unit, such as eclipse.jdt.core/org.eclipse.jdt.core."""
    parts = resource.split(os.sep)
    if parts[0] == "compilation-units" and len(parts) > 3:
        return parts[1] + "/" + parts[2]
    return None


def layered_cost(resource):
    """Counts the new IPFS objects after a change to one compilation unit in the
    layered publication: the file, every directory from the folder of the file up
    to the component root, the component manifest and the project version manifest.
    The IPNS record is counted separately."""
    parts = resource.split(os.sep)
    directories = len(parts) - 3
    return 1 + directories + 1 + 1


def main():
    root, results = sys.argv[1], sys.argv[2]
    os.makedirs(results, exist_ok=True)
    resources = list_resources(root)
    edges, href_counts = read_edges(root, resources)
    reverse = {resource: set() for resource in resources}
    for source, targets in edges.items():
        for target in targets:
            reverse[target].add(source)
    components = strongly_connected_components(resources, edges)
    components.sort(key=len, reverse=True)
    largest = set(components[0])
    edge_count = sum(len(targets) for targets in edges.values())

    units = [r for r in resources if r.startswith("compilation-units" + os.sep)]
    referenced_by_units = collections.Counter()
    for unit in units:
        for target in edges[unit]:
            if target.startswith("compilation-units" + os.sep):
                referenced_by_units[target] += 1
    leaf_candidates = [u for u in units if referenced_by_units[u] == 0]
    leaf = sorted(leaf_candidates, key=lambda u: (len(reverse[u]), u))[0]
    most_used_unit = max(units, key=lambda u: (len(reverse[u]), u))
    targets = [
        ("compilation unit that no other compilation unit references", leaf),
        ("compilation unit with the most referencing resources", most_used_unit),
        ("root model", "all-repos-merged.root.xmi"),
        ("shared resource aux/orphan-types.xmi", os.path.join("aux", "orphan-types.xmi")),
        ("shared resource aux/unassigned-package-types.xmi", os.path.join("aux", "unassigned-package-types.xmi")),
    ]
    rows = []
    for label, target in targets:
        affected = affected_set(target, reverse)
        rows.append({
            "change": label,
            "resource": target,
            "referencing_resources": len(reverse[target]),
            "affected_resources": len(affected),
            "in_largest_cycle": target in largest,
            "layered_new_objects": layered_cost(target) if target.startswith("compilation-units") else "",
        })

    manifest_dependencies = collections.Counter()
    with open(os.path.join(root, "component-dependencies.csv"), newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            manifest_dependencies[row["target_kind"]] += 1

    cross_unit_edges = sum(1 for unit in units for target in edges[unit]
                           if target.startswith("compilation-units") and component_of(target) != component_of(unit))
    within_component_edges = sum(1 for unit in units for target in edges[unit]
                                 if target.startswith("compilation-units") and component_of(target) == component_of(unit))
    figures = {
        "resources": len(resources),
        "compilation_units": len(units),
        "resource_edges": edge_count,
        "serialized_cross_resource_hrefs": sum(href_counts.values()),
        "strongly_connected_components": len(components),
        "largest_component_size": len(components[0]),
        "resources_outside_largest_component": len(resources) - len(components[0]),
        "unit_to_unit_edges_within_component": within_component_edges,
        "unit_to_unit_edges_across_components": cross_unit_edges,
        "manifest_dependency_edges": dict(manifest_dependencies),
        "changes": rows,
    }
    with open(os.path.join(results, "resource-graph.json"), "w", encoding="utf-8") as out:
        json.dump(figures, out, indent=2)
    lines = ["# Resource graph of the partitioned MoDisco model", "",
             "Generated by `eval/modisco-cascade/resource_graph.py` from the serialized hrefs of the partition.", "",
             "| measure | value |", "|---|---:|"]
    for key in ("resources", "compilation_units", "resource_edges", "serialized_cross_resource_hrefs",
                "strongly_connected_components", "largest_component_size", "resources_outside_largest_component",
                "unit_to_unit_edges_within_component", "unit_to_unit_edges_across_components"):
        lines.append("| %s | %s |" % (key.replace("_", " "), "{:,}".format(figures[key])))
    lines.append("| component manifest dependency edges | %s |" % ", ".join(
        "%s: %d" % (k, v) for k, v in sorted(manifest_dependencies.items())))
    lines += ["", "| change | resource | referencing resources | affected resources A(t) | in the largest cycle | new objects in the layered publication |",
              "|---|---|---:|---:|---|---:|"]
    for row in rows:
        lines.append("| %s | `%s` | %s | %s | %s | %s |" % (row["change"], row["resource"], "{:,}".format(row["referencing_resources"]),
                                                     "{:,}".format(row["affected_resources"]), "yes" if row["in_largest_cycle"] else "no",
                                                     row["layered_new_objects"]))
    with open(os.path.join(results, "resource-graph.md"), "w", encoding="utf-8") as out:
        out.write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
