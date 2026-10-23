"""Summarizes the result file of the multi-node run as Markdown.

Every cell gives the median of a group of measured iterations in milliseconds,
with the interquartile range in brackets. The groups are the topology, the
delay profile, the workload, the model size or file, and the operation.

    python3 -I eval/multinode/summarize.py eval/results/multinode/metrics.csv > summary.md
"""

import argparse
import csv
import statistics
from collections import defaultdict

PROFILE_ORDER = ("none", "lan", "wan50", "wan100")
TOPOLOGY_ORDER = ("peered", "dht")
MODEL_OPERATIONS = ("save_a", "cat_b", "load_c")
IPNS_OPERATIONS = ("publish_a", "resolve_b", "visible_b")


def ordered(values, order):
    """Sorts values by a preferred order, unknown values last in name order."""
    known = [value for value in order if value in values]
    unknown = sorted(value for value in values if value not in order)
    return known + unknown


def number(value):
    """Formats milliseconds with fewer decimals as the value grows."""
    if value < 10:
        return "%.2f" % value
    if value < 1000:
        return "%.1f" % value
    return "{:,.0f}".format(value)


def cell(values):
    """Median with the interquartile range, or a dash for an empty group."""
    if not values:
        return "-"
    if len(values) == 1:
        return number(values[0])
    first, middle, third = statistics.quantiles(values, n=4, method="inclusive")
    return "%s [%s, %s]" % (number(middle), number(first), number(third))


def read(path):
    """Reads the result file into a list of rows."""
    with open(path, newline="", encoding="utf-8") as source:
        rows = list(csv.DictReader(source))
    for row in rows:
        row["milliseconds"] = float(row["milliseconds"])
        row["bytes"] = int(row["bytes"])
    return rows


def model_tables(rows, topologies, profiles):
    """One table per topology: model size and operation against the profiles."""
    lines = ["## Generated models", ""]
    times = defaultdict(list)
    sizes = defaultdict(list)
    for row in rows:
        if row["workload"] == "models":
            key = (row["topology"], int(row["label"]), row["operation"])
            times[key + (row["profile"],)].append(row["milliseconds"])
            sizes[int(row["label"])].append(row["bytes"])
    if not sizes:
        return []
    for topology in topologies:
        lines += ["Topology `%s`:" % topology, ""]
        lines.append("| elements | bytes | operation | " + " | ".join(profiles) + " |")
        lines.append("|---:|---:|---|" + "---:|" * len(profiles))
        for elements in sorted(sizes):
            for operation in MODEL_OPERATIONS:
                cells = [cell(times[(topology, elements, operation, profile)]) for profile in profiles]
                lines.append("| {:,} | {:,} | {} | {} |".format(
                    elements, int(statistics.median(sizes[elements])), operation, " | ".join(cells)))
        lines.append("")
    return lines


def bpmn_table(rows, topologies, profiles):
    """One row per topology and profile over the sampled BPMN files."""
    times = defaultdict(list)
    sizes = defaultdict(set)
    for row in rows:
        if row["workload"] == "bpmn":
            times[(row["topology"], row["profile"], row["operation"])].append(row["milliseconds"])
            sizes[(row["topology"], row["profile"])].add((row["label"], row["bytes"]))
    if not sizes:
        return []
    lines = ["## Real BPMN files", ""]
    lines.append("| topology | profile | files | bytes min / median / max | add_a | cat_b | cat_b max |")
    lines.append("|---|---|---:|---|---:|---:|---:|")
    for topology in topologies:
        for profile in profiles:
            if (topology, profile) not in sizes:
                continue
            byte_counts = [count for _, count in sizes[(topology, profile)]]
            cats = times[(topology, profile, "cat_b")]
            lines.append("| {} | {} | {} | {:,} / {:,} / {:,} | {} | {} | {} |".format(
                topology, profile, len(byte_counts), min(byte_counts), int(statistics.median(byte_counts)),
                max(byte_counts), cell(times[(topology, profile, "add_a")]), cell(cats),
                number(max(cats)) if cats else "-"))
    lines.append("")
    return lines


def ipns_table(rows, topologies, profiles):
    """One row per topology and profile for the IPNS publications."""
    times = defaultdict(list)
    attempts = defaultdict(list)
    for row in rows:
        if row["workload"] == "ipns":
            times[(row["topology"], row["profile"], row["operation"])].append(row["milliseconds"])
            if row["operation"].startswith("visible_b"):
                attempts[(row["topology"], row["profile"])].append(int(row["label"].split("=")[1]))
    if not attempts:
        return []
    lines = ["## IPNS", ""]
    lines.append("| topology | profile | publications | " + " | ".join(IPNS_OPERATIONS)
                 + " | timeouts | resolutions until visible, max |")
    lines.append("|---|---|---:|" + "---:|" * len(IPNS_OPERATIONS) + "---:|---:|")
    for topology in topologies:
        for profile in profiles:
            if (topology, profile) not in attempts:
                continue
            cells = [cell(times[(topology, profile, operation)]) for operation in IPNS_OPERATIONS]
            lines.append("| {} | {} | {} | {} | {} | {} |".format(
                topology, profile, len(attempts[(topology, profile)]), " | ".join(cells),
                len(times[(topology, profile, "visible_b_timeout")]), max(attempts[(topology, profile)])))
    lines.append("")
    return lines


def connection_table(rows, topologies, profiles):
    """How often a fetcher held a connection to the publisher when it started."""
    states = defaultdict(list)
    for row in rows:
        if row["connected_to_publisher"] in ("true", "false"):
            key = (row["topology"], row["profile"], row["workload"], row["operation"])
            states[key].append(row["connected_to_publisher"] == "true")
    lines = ["## Connection to the publisher at the start of each fetch", ""]
    lines.append("| topology | profile | workload | operation | connected / all |")
    lines.append("|---|---|---|---|---:|")
    for topology in topologies:
        for profile in profiles:
            for key in sorted(key for key in states if key[:2] == (topology, profile)):
                lines.append("| %s | %s | %s | %s | %d / %d |" % (key + (sum(states[key]), len(states[key]))))
    lines.append("")
    return lines


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("metrics", help="the CSV file that the harness wrote")
    arguments = parser.parse_args()

    rows = read(arguments.metrics)
    topologies = ordered({row["topology"] for row in rows}, TOPOLOGY_ORDER)
    profiles = ordered({row["profile"] for row in rows}, PROFILE_ORDER)
    runs = sorted({row["run"] for row in rows})
    lines = ["# Multi-node measurements", ""]
    lines.append("%d rows from run %s. Each cell gives the median time in milliseconds with the "
                 "interquartile range in brackets." % (len(rows), ", ".join(runs)))
    lines.append("")
    lines += model_tables(rows, topologies, profiles)
    lines += bpmn_table(rows, topologies, profiles)
    lines += ipns_table(rows, topologies, profiles)
    lines += connection_table(rows, topologies, profiles)
    print("\n".join(lines).rstrip())


if __name__ == "__main__":
    main()
