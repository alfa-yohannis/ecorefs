"""Summarize the save and load benchmark and compare the two persistence paths.

The script reads eval/results/benchmark_metrics.csv, which holds one row per
measured iteration, and writes eval/results/benchmark_summary.md. For every model
size the script reports mean, median and standard deviation, the difference
between the two paths, and a Wilcoxon signed-rank test on the paired iterations.
The test is paired, because both paths run inside the same iteration on the same
model copy.
"""

import pathlib

import pandas
from scipy import stats

RESULTS = pathlib.Path(__file__).resolve().parent / "results"
METRICS_FILE = RESULTS / "benchmark_metrics.csv"
SUMMARY_FILE = RESULTS / "benchmark_summary.md"

OPERATIONS = [
    ("save", "XmiSaveMs", "IpfsSaveMs"),
    ("load", "XmiLoadMs", "IpfsLoadMs"),
]


def describe(series):
    """Return mean, median and standard deviation of one column."""
    return series.mean(), series.median(), series.std(ddof=1)


def compare(rows, xmi_column, ipfs_column):
    """Return the paired comparison of one operation for one model size."""
    xmi = rows[xmi_column]
    ipfs = rows[ipfs_column]
    statistic, p_value = stats.wilcoxon(ipfs, xmi)
    return {
        "xmi": describe(xmi),
        "ipfs": describe(ipfs),
        "difference": ipfs.mean() - xmi.mean(),
        "ratio": ipfs.mean() / xmi.mean(),
        "statistic": statistic,
        "p_value": p_value,
    }


def format_statistics(values):
    """Format mean, median and standard deviation as text."""
    mean, median, deviation = values
    return f"{mean:.2f} / {median:.2f} / {deviation:.2f}"


def main():
    """Read the measurements and write the summary file."""
    measurements = pandas.read_csv(METRICS_FILE)
    iterations = measurements.groupby("Elements").size().unique()
    lines = ["# Benchmark summary", ""]
    lines.append(
        f"Measurements: {len(measurements)} rows, {iterations[0]} iterations per model size, "
        "three warm-up iterations before each series."
    )
    lines.append("")
    lines.append("Values are mean / median / standard deviation in milliseconds.")
    lines.append("")

    for operation, xmi_column, ipfs_column in OPERATIONS:
        lines.append(f"## {operation.capitalize()}")
        lines.append("")
        lines.append("| Elements | XMI file | IPFS | Difference (ms) | Ratio | Wilcoxon p |")
        lines.append("| --- | --- | --- | --- | --- | --- |")
        for element_count in sorted(measurements["Elements"].unique()):
            rows = measurements[measurements["Elements"] == element_count]
            result = compare(rows, xmi_column, ipfs_column)
            lines.append(
                f"| {element_count:,} | {format_statistics(result['xmi'])} | "
                f"{format_statistics(result['ipfs'])} | {result['difference']:.2f} | "
                f"{result['ratio']:.2f} | {result['p_value']:.2e} |"
            )
        lines.append("")

    SUMMARY_FILE.write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
