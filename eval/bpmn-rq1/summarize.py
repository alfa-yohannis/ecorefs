"""Summarizes the RQ1 runs on real BPMN models.

Reads the raw tables of run.sh (raw/roundtrip/*.tsv, raw/dedup.tsv with its .repo
file, raw/directory.tsv) and writes into the results folder:

- summary.md: the tables that the paper and the README cite,
- roundtrip.tsv.gz: the per-file results of the collections whose terms allow
  redistribution, with the main columns of each path.

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

csv.field_size_limit(10**8)

COLLECTIONS = [
    "camunda-bpmn-for-research",
    "omg-miwg-test-suite",
    "jbpm-tests",
    "flowable-tests",
    "camunda7-tests",
]
NOT_SHARED = {"camunda-bpmn-for-research"}
PATHS = [
    ("xmi_local", "Plain XMI, local file"),
    ("xmi_ipfs", "Plain XMI, IPFS (IPFSResourceImpl)"),
    ("emd_local", "XMI with extended metadata, local file"),
    ("emd_ipfs", "XMI with extended metadata, IPFS"),
    ("bpmn_local", "BPMN 2.0 XML, local file"),
    ("bpmn_ipfs", "BPMN 2.0 XML, IPFS"),
    ("bpmn_ipfs_numbered", "BPMN 2.0 XML, IPFS, numbered ids"),
]
PAIRS = [("xmi_local", "xmi_ipfs"), ("emd_local", "emd_ipfs"), ("bpmn_local", "bpmn_ipfs")]


STATISTICS_COLUMNS = ["objects", "flow_elements", "missing_ids", "extension_entries", "external_proxies"]


def read_roundtrip(raw):
    """Reads the round trip rows and takes the model counts from raw/statistics
    when that folder exists, since the statistics run applies the final counting
    rules."""
    rows = []
    for collection in COLLECTIONS:
        path = os.path.join(raw, "roundtrip", collection + ".tsv")
        if os.path.exists(path):
            with open(path, newline="", encoding="utf-8") as table:
                rows.extend(csv.DictReader(table, delimiter="\t"))
    recount = {}
    for collection in COLLECTIONS:
        path = os.path.join(raw, "statistics", collection + ".tsv")
        if os.path.exists(path):
            with open(path, newline="", encoding="utf-8") as table:
                for row in csv.DictReader(table, delimiter="\t"):
                    recount[(row["collection"], row["path"])] = row
    for row in rows:
        counts = recount.get((row["collection"], row["path"]))
        if counts is not None and counts["source_load"] == row["source_load"]:
            for column in STATISTICS_COLUMNS:
                row[column] = counts[column]
    return rows


def loaded(row):
    return row["source_load"] in ("OK",) or row["source_load"].startswith("ERRORS")


def outcome(row, path):
    """Returns the outcome class of one path for one row."""
    save = row[path + "_save"]
    if save == "-":
        return "not run"
    if not save.startswith("OK"):
        return "save fails"
    load = row[path + "_load"]
    if load.startswith("FAIL"):
        return "load fails"
    if row[path + "_equal"] == "yes":
        return "equal" if load == "OK" else "equal, load errors"
    category = row[path + "_difference"].split(" | ")[0]
    return "differs: " + category


def failure_kind(text):
    text = text.replace("FAIL ", "", 1)
    return text.split(":")[0]


def percent(part, whole):
    return "%.1f%%" % (100.0 * part / whole) if whole else "-"


def count(n):
    return "{:,}".format(n)


def median(values):
    return statistics.median(values) if values else float("nan")


def table(header, lines):
    out = ["| " + " | ".join(header) + " |", "|" + "|".join("---" if i == 0 else "---:" for i in range(len(header))) + "|"]
    for line in lines:
        out.append("| " + " | ".join(str(cell) for cell in line) + " |")
    return "\n".join(out)


def source_table(rows):
    lines = []
    totals = collections.Counter()
    for collection in COLLECTIONS:
        subset = [r for r in rows if r["collection"] == collection]
        ok = sum(1 for r in subset if r["source_load"] == "OK")
        errors = sum(1 for r in subset if r["source_load"].startswith("ERRORS"))
        failed = len(subset) - ok - errors
        totals.update(files=len(subset), ok=ok, errors=errors, failed=failed)
        lines.append([collection, count(len(subset)), count(ok), count(errors), count(failed)])
    lines.append(["total", count(totals["files"]), count(totals["ok"]), count(totals["errors"]), count(totals["failed"])])
    return table(["collection", "files", "load without errors", "load with errors", "no model"], lines)


def path_table(rows):
    base = [r for r in rows if loaded(r)]
    lines = []
    for path, label in PATHS:
        outcomes = collections.Counter(outcome(r, path) for r in base)
        saved = [r for r in base if r[path + "_save"].startswith("OK")]
        same = sum(1 for r in saved if r[path + "_repeat"] == "same")
        equal = outcomes["equal"] + outcomes["equal, load errors"]
        lines.append([label, count(len(base)), count(len(saved)) + " (" + percent(len(saved), len(base)) + ")",
                      count(equal) + " (" + percent(equal, len(base)) + ")",
                      count(same) + " of " + count(len(saved)) + " (" + percent(same, len(saved)) + ")"])
    return table(["path", "models", "save succeeds", "loaded model equals saved model", "second save gives the same identifier"], lines)


def path_by_collection_table(rows):
    lines = []
    for collection in COLLECTIONS:
        subset = [r for r in rows if r["collection"] == collection and loaded(r)]
        cells = [collection, count(len(subset))]
        for path, _ in PATHS:
            equal = sum(1 for r in subset if outcome(r, path).startswith("equal"))
            cells.append(percent(equal, len(subset)))
        lines.append(cells)
    return table(["collection", "models"] + [p for p, _ in PATHS], lines)


def outcome_table(rows):
    base = [r for r in rows if loaded(r)]
    lines = []
    for path, label in PATHS:
        outcomes = collections.Counter(outcome(r, path) for r in base)
        for name, n in sorted(outcomes.items(), key=lambda item: -item[1]):
            lines.append([label, name, count(n), percent(n, len(base))])
    return table(["path", "outcome", "models", "share"], lines)


def failure_table(rows):
    base = [r for r in rows if loaded(r)]
    lines = []
    for path, label in PATHS:
        kinds = collections.Counter(failure_kind(r[path + "_save"]) for r in base if r[path + "_save"].startswith("FAIL"))
        for kind, n in kinds.most_common():
            lines.append([label, "save", kind, count(n)])
        loads = collections.Counter(r[path + "_load"].split(" (")[0][:80] for r in base
                                    if r[path + "_load"].startswith("ERRORS") or r[path + "_load"].startswith("FAIL"))
        for kind, n in loads.most_common(3):
            lines.append([label, "load", kind.replace("|", "/"), count(n)])
    return table(["path", "step", "first error", "models"], lines)


def pairing_table(rows):
    base = [r for r in rows if loaded(r)]
    lines = []
    for local, ipfs in PAIRS:
        agree = sum(1 for r in base if outcome(r, local) == outcome(r, ipfs))
        same_difference = sum(1 for r in base if r[local + "_difference"].split(" | ", 1)[-1] == r[ipfs + "_difference"].split(" | ", 1)[-1])
        lines.append([local + " / " + ipfs, count(len(base)), count(agree) + " (" + percent(agree, len(base)) + ")",
                      count(same_difference) + " (" + percent(same_difference, len(base)) + ")"])
    return table(["local / IPFS", "models", "same outcome", "same first difference"], lines)


def determinism_table(rows):
    base = [r for r in rows if loaded(r)]
    lines = []
    for path in ("xmi_ipfs", "bpmn_ipfs", "bpmn_ipfs_numbered"):
        saved = [r for r in base if r[path + "_save"].startswith("OK") and r[path + "_repeat"] in ("same", "different")]
        for has_missing in (True, False):
            group = [r for r in saved if (int(r["missing_ids"]) > 0) == has_missing]
            different = sum(1 for r in group if r[path + "_repeat"] == "different")
            lines.append([path, "yes" if has_missing else "no", count(len(group)), count(different), percent(different, len(group))])
    return table(["path", "objects without id", "models", "new CID on the second save", "share"], lines)


def size_table(rows):
    base = [r for r in rows if loaded(r)]
    lines = []
    for collection in COLLECTIONS:
        subset = [r for r in base if r["collection"] == collection]
        if not subset:
            continue
        sizes = [int(r["bytes"]) for r in subset]
        flows = [int(r["flow_elements"]) for r in subset]
        objects = [int(r["objects"]) for r in subset]
        missing = sum(1 for r in subset if int(r["missing_ids"]) > 0)
        extensions = sum(1 for r in subset if int(r["extension_entries"]) > 0)
        lines.append([collection, count(len(subset)), count(int(median(sizes))), count(max(sizes)),
                      count(int(median(objects))), count(max(objects)), count(int(median(flows))), count(max(flows)),
                      percent(missing, len(subset)), percent(extensions, len(subset))])
    return table(["collection", "models", "bytes median", "bytes max", "objects median", "objects max",
                  "flow elements median", "flow elements max", "with objects without id", "with vendor extensions"], lines)


def timing_table(rows):
    lines = []
    base = [r for r in rows if loaded(r)]
    for local, ipfs in (("xmi_local", "xmi_ipfs"), ("bpmn_local", "bpmn_ipfs")):
        both = [r for r in base if r[local + "_save"].startswith("OK") and r[ipfs + "_save"].startswith("OK")
                and r[local + "_load_ms"] and r[ipfs + "_load_ms"]]
        if not both:
            continue
        for step in ("save", "load"):
            local_ms = [float(r[local + "_" + step + "_ms"]) for r in both]
            ipfs_ms = [float(r[ipfs + "_" + step + "_ms"]) for r in both]
            lines.append([local + " / " + ipfs, step, count(len(both)), "%.2f" % median(local_ms), "%.2f" % median(ipfs_ms),
                          "%.2f" % median([b - a for a, b in zip(local_ms, ipfs_ms)])])
    return table(["paths", "step", "models", "local median (ms)", "IPFS median (ms)", "median of paired differences (ms)"], lines)


def dedup_section(raw):
    path = os.path.join(raw, "dedup.tsv")
    if not os.path.exists(path):
        return "Not run yet."
    with open(path, newline="", encoding="utf-8") as table_file:
        rows = list(csv.DictReader(table_file, delimiter="\t"))
    repo = {}
    with open(path + ".repo", encoding="utf-8") as repo_file:
        for line in repo_file:
            key, value = line.rstrip("\n").split("\t")
            repo[key] = value
    total_bytes = sum(int(r["bytes"]) for r in rows)
    by_cid = {}
    for r in rows:
        by_cid.setdefault(r["cid"], int(r["bytes"]))
    unique_bytes = sum(by_cid.values())
    allocated = sum(-(-int(r["bytes"]) // 4096) * 4096 for r in rows)
    growth = int(repo["repo_bytes_after"]) - int(repo["repo_bytes_before"])
    adds = [float(r["add_ms"]) for r in rows]
    lines = [
        ["files added one by one", count(len(rows))],
        ["distinct CIDs", count(len(by_cid))],
        ["files with the CID of an earlier file", count(len(rows) - len(by_cid))],
        ["bytes of all files", count(total_bytes)],
        ["bytes of the distinct contents", count(unique_bytes) + " (" + percent(unique_bytes, total_bytes) + ")"],
        ["plain copy on 4 KiB blocks (computed)", count(allocated)],
        ["growth of RepoSize reported by Kubo", count(growth)],
        ["blocks folder, apparent bytes (du -sb)", count(int(repo["blocks_apparent_bytes"]))],
        ["blocks folder, allocated (du -sk, in bytes)", count(int(repo["blocks_allocated_kib"]) * 1024)],
        ["whole repository, allocated (du -sk, in bytes)", count(int(repo["datastore_allocated_kib"]) * 1024)],
        ["repository objects before / after", count(int(repo["repo_objects_before"])) + " / " + count(int(repo["repo_objects_after"]))],
        ["time of all adds (s)", repo["add_seconds"]],
        ["models per second, one add per model", "%.1f" % (len(rows) / float(repo["add_seconds"]))],
        ["median time of one add (ms)", "%.2f" % median(adds)],
    ]
    return table(["measure", "value"], lines)


def directory_section(raw):
    path = os.path.join(raw, "directory.tsv")
    if not os.path.exists(path):
        return "Not run yet."
    with open(path, newline="", encoding="utf-8") as table_file:
        rows = list(csv.DictReader(table_file, delimiter="\t"))
    lines = []
    for r in rows:
        seconds = float(r["add_ms"]) / 1000.0
        cid = r["cid"] if r["collection"] not in NOT_SHARED else "(not listed)"
        lines.append([r["collection"], count(int(r["files"])), count(int(r["bytes"])), "%.2f" % seconds,
                      "%.0f" % (int(r["files"]) / seconds), cid])
    files = sum(int(r["files"]) for r in rows)
    seconds = sum(float(r["add_ms"]) for r in rows) / 1000.0
    lines.append(["total", count(files), count(sum(int(r["bytes"]) for r in rows)), "%.2f" % seconds, "%.0f" % (files / seconds), ""])
    return table(["collection", "files", "bytes", "time of the add (s)", "models per second", "directory CID"], lines)


def write_per_file(rows, results):
    columns = ["collection", "path", "bytes", "source_load", "objects", "flow_elements", "missing_ids",
               "extension_entries", "external_proxies"]
    for path, _ in PATHS:
        columns += [path + "_save", path + "_load", path + "_equal", path + "_difference", path + "_repeat", path + "_id"]
    with gzip.open(os.path.join(results, "roundtrip.tsv.gz"), "wt", encoding="utf-8", newline="") as out:
        writer = csv.writer(out, delimiter="\t", lineterminator="\n")
        writer.writerow(columns)
        for r in rows:
            if r["collection"] not in NOT_SHARED:
                writer.writerow([r[c] for c in columns])


def main():
    raw, results = sys.argv[1], sys.argv[2]
    rows = read_roundtrip(raw)
    os.makedirs(results, exist_ok=True)
    sections = [
        "# RQ1 on real BPMN models: summary tables",
        "Generated by `eval/bpmn-rq1/summarize.py` from the raw tables of `eval/bpmn-rq1/run.sh`. README.md explains the setup.",
        "## Source models\n\n" + source_table(rows),
        "## Size of the models\n\nCounted on the models that loaded. Objects are EMF objects, flow elements are BPMN flow elements, and an object without id is an object whose class has an identifier attribute that the file leaves empty.\n\n" + size_table(rows),
        "## Round trip per path\n\n" + path_table(rows),
        "## Equal round trips per collection\n\n" + path_by_collection_table(rows),
        "## Outcomes per path\n\n" + outcome_table(rows),
        "## Errors\n\n" + failure_table(rows),
        "## Local and IPFS paths, model by model\n\n" + pairing_table(rows),
        "## Determinism\n\n" + determinism_table(rows),
        "## Times inside the round trip (indicative)\n\nFive collections ran in parallel, so the times include contention between the five virtual machines.\n\n" + timing_table(rows),
        "## Deduplication\n\n" + dedup_section(raw),
        "## One directory per collection\n\n" + directory_section(raw),
    ]
    with open(os.path.join(results, "summary.md"), "w", encoding="utf-8") as out:
        out.write("\n\n".join(sections) + "\n")
    write_per_file(rows, results)
    print("\n\n".join(sections))


if __name__ == "__main__":
    main()
