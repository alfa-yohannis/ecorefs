"""Summarizes the RQ2 runs on real BPMN models.

Reads raw/split.tsv and raw/calls.tsv of run.sh and writes into the results folder:

- summary.md: the tables that the paper and the README cite,
- split.tsv.gz and calls.tsv.gz: the per-row results of the collections whose
  terms allow redistribution.

Camunda bpmn-for-research appears in the aggregates only, because its terms do
not cover redistribution.

    python3 -I summarize.py <raw-folder> <results-folder>
"""

import collections
import csv
import gzip
import os
import statistics
import sys

NOT_SHARED = {"camunda-bpmn-for-research"}


def read(path):
    if not os.path.exists(path):
        return []
    with open(path, newline="", encoding="utf-8") as table:
        return list(csv.DictReader(table, delimiter="\t"))


def count(n):
    return "{:,}".format(n)


def percent(part, whole):
    return "%.1f%%" % (100.0 * part / whole) if whole else "-"


def table(header, lines):
    out = ["| " + " | ".join(header) + " |", "|" + "|".join("---" if i == 0 else "---:" for i in range(len(header))) + "|"]
    for line in lines:
        out.append("| " + " | ".join(str(cell) for cell in line) + " |")
    return "\n".join(out)


def ran(row, change):
    return row.get(change + "_new_cids_match", "-") in ("yes", "no")


def consistent(row, prefix):
    return row[prefix + "stale_refs"] == "0" and row[prefix + "unknown_refs"] == "0"


def loaded_fully(row, prefix):
    return (row[prefix + "load_resources"] == row[prefix + "load_expected"] and row[prefix + "load_stale"] == "0"
            and row[prefix + "load_unresolved"] == "0" and row[prefix + "load_errors"] == "0")


def median(values):
    return statistics.median(values) if values else float("nan")


def split_sections(rows):
    if not rows:
        return ["## Split of multi-pool models\n\nNot run yet."]
    published = [r for r in rows if r["publish"] == "OK"]
    failures = collections.Counter(r["publish"] if r["publish"].startswith("FAIL") else r["processes"]
                                   for r in rows if r["publish"] != "OK")
    by_collection = []
    for collection in sorted({r["collection"] for r in rows}):
        subset = [r for r in rows if r["collection"] == collection]
        ok = [r for r in subset if r["publish"] == "OK"]
        cyclic = sum(1 for r in ok if r["cyclic"] == "yes")
        by_collection.append([collection, count(len(subset)), count(len(ok)), count(cyclic),
                              "%g" % median([int(r["resources"]) for r in ok]) if ok else "-",
                              max((int(r["resources"]) for r in ok), default="-")])
    sections = ["## Split of multi-pool models\n\n" + table(
        ["collection", "models", "split and published", "with a reference cycle", "resources median", "resources max"],
        by_collection)]
    if failures:
        sections.append("Models that did not reach the first publication:\n\n" + table(
            ["first error", "models"], [[k.replace("|", "/"), count(v)] for k, v in failures.most_common()]))
    lines = []
    for cyclic in ("no", "yes"):
        group = [r for r in published if r["cyclic"] == cyclic]
        initial = sum(1 for r in group if r["initial_consistent"] == "yes")
        lines.append(["first publication", cyclic, count(len(group)), "-", count(initial) + " (" + percent(initial, len(group)) + ")", "-", "-"])
        for change in ("process", "shared"):
            prefix = change + "_"
            done = [r for r in group if ran(r, change)]
            match = sum(1 for r in done if r[prefix + "new_cids_match"] == "yes")
            good = sum(1 for r in done if consistent(r, prefix))
            full = sum(1 for r in done if loaded_fully(r, prefix))
            reached = [r for r in done if r[prefix + "change_visible"] in ("yes", "no")]
            visible = sum(1 for r in reached if r[prefix + "change_visible"] == "yes")
            lines.append(["rename in the " + change + " resource", cyclic, count(len(done)),
                          count(match) + " (" + percent(match, len(done)) + ")",
                          count(good) + " (" + percent(good, len(done)) + ")",
                          count(full) + " (" + percent(full, len(done)) + ")",
                          count(visible) + " of " + count(len(reached)) + " (" + percent(visible, len(reached)) + ")"])
    sections.append("## Cascade checks on the split models\n\n"
                    "New CIDs match: the resources with a new CID are exactly the affected set of the independent reference graph. "
                    "Consistent: every stored reference names the current CID. Loads fully: a load by CID from the collaboration "
                    "resource fetched every reachable resource under its current CID, with no proxy left and no load error. "
                    "Change visible: the loaded copy of the changed object carries the new name, counted where the load reaches it.\n\n" + table(
                        ["step", "cycle", "models", "new CIDs match", "consistent", "loads fully", "change visible after the load"], lines))
    unresolved = [r for r in published if r["cyclic"] == "no" and ran(r, "process") and r["process_load_unresolved"] != "0"]
    sections.append("Acyclic split models whose load by CID left proxies: " + count(len(unresolved)) + ".")
    timing = []
    for change in ("process", "shared"):
        prefix = change + "_"
        done = [r for r in published if ran(r, change) and r["cyclic"] == "no"]
        by_size = collections.defaultdict(list)
        for r in done:
            by_size[int(r[prefix + "new_cids"])].append(float(r[prefix + "cascade_ms"]))
        for size in sorted(by_size):
            values = by_size[size]
            timing.append([change, size, count(len(values)), "%.1f" % median(values), "%.1f" % max(values)])
    sections.append("## Cascade time on the split models (acyclic)\n\n" + table(
        ["changed resource", "resources with a new CID", "changes", "median (ms)", "max (ms)"], timing))
    return sections


def call_sections(rows):
    if not rows:
        return ["## Call graphs between files\n\nNot run yet."]
    components = {}
    for r in rows:
        components.setdefault(r["component"], r)
    sizes = collections.Counter(int(r["files"]) for r in components.values())
    links = sum(int(r["links"]) for r in components.values() if r["links"].isdigit())
    applied = sum(int(r["links_applied"]) for r in components.values() if r["links_applied"].isdigit())
    cyclic = sum(1 for r in components.values() if r["cyclic"] == "yes")
    changes = [r for r in rows if r["new_cids_match"] in ("yes", "no")]
    failed = [r for r in rows if r["new_cids_match"] not in ("yes", "no")]
    sections = ["## Call graphs between files\n\n" + table(["measure", "value"], [
        ["components", count(len(components))],
        ["files", count(sum(int(r["files"]) for r in components.values()))],
        ["call links", count(links)],
        ["links set as references", count(applied)],
        ["components with a reference cycle", count(cyclic)],
        ["changes", count(len(changes))],
        ["changes that did not run", count(len(failed))],
        ["largest component (files)", count(max(sizes))],
        ["largest fan-in", max(int(r["fan_in"]) for r in changes)],
        ["deepest chain (edges)", max(int(r["depth"]) for r in changes)],
    ])]
    sections.append("Files per component:\n\n" + table(["files", "components"], [[k, v] for k, v in sorted(sizes.items())]))
    lines = []
    for cyclic_value in ("no", "yes"):
        group = [r for r in changes if r["cyclic"] == cyclic_value]
        match = sum(1 for r in group if r["new_cids_match"] == "yes")
        good = sum(1 for r in group if consistent(r, ""))
        full = sum(1 for r in group if loaded_fully(r, ""))
        reached = [r for r in group if r["change_visible"] in ("yes", "no")]
        visible = sum(1 for r in reached if r["change_visible"] == "yes")
        lines.append([cyclic_value, count(len(group)), count(match) + " (" + percent(match, len(group)) + ")",
                      count(good) + " (" + percent(good, len(group)) + ")", count(full) + " (" + percent(full, len(group)) + ")",
                      count(visible) + " of " + count(len(reached)) + " (" + percent(visible, len(reached)) + ")"])
    sections.append("## Cascade checks on the call graphs\n\n" + table(
        ["cycle", "changes", "new CIDs match", "consistent", "loads fully", "change visible after the load"], lines))
    by_size = collections.defaultdict(list)
    by_depth = collections.defaultdict(list)
    for r in changes:
        if r["cyclic"] == "no":
            by_size[int(r["new_cids"])].append(float(r["cascade_ms"]))
            by_depth[int(r["depth"])].append(int(r["new_cids"]))
    sections.append("## Cascade time against the number of new CIDs (acyclic)\n\n" + table(
        ["resources with a new CID", "changes", "median (ms)", "max (ms)", "median per new CID (ms)"],
        [[k, count(len(v)), "%.1f" % median(v), "%.1f" % max(v), "%.1f" % (median(v) / k)] for k, v in sorted(by_size.items())]))
    sections.append("## Chain depth of the changed file (acyclic)\n\n" + table(
        ["depth (edges above the file)", "changes", "median resources with a new CID", "max"],
        [[k, count(len(v)), "%g" % median(v), max(v)] for k, v in sorted(by_depth.items())]))
    return sections


def write_shared(rows, path):
    if not rows:
        return
    with gzip.open(path, "wt", encoding="utf-8", newline="") as out:
        writer = csv.DictWriter(out, fieldnames=list(rows[0].keys()), delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for r in rows:
            if r.get("collection", "") not in NOT_SHARED and not r.get("file", r.get("target", "")).startswith("camunda-bpmn-for-research"):
                writer.writerow(r)


def main():
    raw, results = sys.argv[1], sys.argv[2]
    os.makedirs(results, exist_ok=True)
    split_rows = read(os.path.join(raw, "split.tsv"))
    call_rows = read(os.path.join(raw, "calls.tsv"))
    sections = ["# RQ2 on real BPMN models: summary tables",
                "Generated by `eval/bpmn-rq2/summarize.py` from the raw tables of `eval/bpmn-rq2/run.sh`. README.md explains the setup."]
    sections += split_sections(split_rows)
    sections += call_sections(call_rows)
    with open(os.path.join(results, "summary.md"), "w", encoding="utf-8") as out:
        out.write("\n\n".join(sections) + "\n")
    write_shared(split_rows, os.path.join(results, "split.tsv.gz"))
    write_shared(call_rows, os.path.join(results, "calls.tsv.gz"))
    print("\n\n".join(sections))


if __name__ == "__main__":
    main()
