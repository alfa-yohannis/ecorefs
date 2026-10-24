"""Summarize the save and load benchmark and compare the two persistence paths.

The script reads eval/results/benchmark_metrics.csv, which holds one row per
measured iteration, and writes eval/results/benchmark_summary.md. For every model
size the script reports mean, median and standard deviation, the difference
between the two paths, and a Wilcoxon signed-rank test on the paired iterations.
The test is paired, because both paths run inside the same iteration on the same
model copy.

Next to each test, the summary gives the matched-pairs rank-biserial correlation
as the effect size, and paired bootstrap confidence intervals for the mean
difference and for the ratio of the means.
"""

import pathlib

import numpy
import pandas
from scipy import stats

RESULTS = pathlib.Path(__file__).resolve().parent / "results"
METRICS_FILE = RESULTS / "benchmark_metrics.csv"
SUMMARY_FILE = RESULTS / "benchmark_summary.md"

OPERATIONS = [
    ("save", "XmiSaveMs", "IpfsSaveMs"),
    ("load", "XmiLoadMs", "IpfsLoadMs"),
]

# Number of bootstrap resamples, level of the intervals, and a fixed seed, so a
# new run of the script reproduces the intervals exactly.
BOOTSTRAP_RESAMPLES = 10_000
CONFIDENCE_LEVEL = 0.95
BOOTSTRAP_SEED = 20261023


def describe(series):
    """Return mean, median and standard deviation of one column."""
    return series.mean(), series.median(), series.std(ddof=1)


def rank_biserial(ipfs, xmi):
    """Return the matched-pairs rank-biserial correlation of the paired values.

    The correlation is the difference between the rank sums of the positive and
    the negative differences, divided by the total rank sum. Zero differences are
    left out, as in the default Wilcoxon test. A value of 1 means that the IPFS
    path took longer in every pair.
    """
    differences = numpy.asarray(ipfs) - numpy.asarray(xmi)
    differences = differences[differences != 0]
    if len(differences) == 0:
        return 0.0
    ranks = stats.rankdata(numpy.abs(differences))
    positive = ranks[differences > 0].sum()
    negative = ranks[differences < 0].sum()
    return (positive - negative) / ranks.sum()


def bootstrap_intervals(ipfs, xmi):
    """Return paired bootstrap intervals for the mean difference and the ratio of means.

    Each resample draws whole iterations with replacement, so the pairing of the
    two paths stays intact.
    """
    ipfs_values = numpy.asarray(ipfs)
    xmi_values = numpy.asarray(xmi)
    generator = numpy.random.default_rng(BOOTSTRAP_SEED)
    indexes = generator.integers(0, len(ipfs_values), size=(BOOTSTRAP_RESAMPLES, len(ipfs_values)))
    ipfs_means = ipfs_values[indexes].mean(axis=1)
    xmi_means = xmi_values[indexes].mean(axis=1)
    tail = (1.0 - CONFIDENCE_LEVEL) / 2.0 * 100.0
    bounds = [tail, 100.0 - tail]
    return numpy.percentile(ipfs_means - xmi_means, bounds), numpy.percentile(ipfs_means / xmi_means, bounds)


def compare(rows, xmi_column, ipfs_column):
    """Return the paired comparison of one operation for one model size."""
    xmi = rows[xmi_column]
    ipfs = rows[ipfs_column]
    statistic, p_value = stats.wilcoxon(ipfs, xmi)
    difference_interval, ratio_interval = bootstrap_intervals(ipfs, xmi)
    return {
        "xmi": describe(xmi),
        "ipfs": describe(ipfs),
        "difference": ipfs.mean() - xmi.mean(),
        "difference_interval": difference_interval,
        "ratio": ipfs.mean() / xmi.mean(),
        "ratio_interval": ratio_interval,
        "statistic": statistic,
        "p_value": p_value,
        "effect_size": rank_biserial(ipfs, xmi),
    }


def format_statistics(values):
    """Format mean, median and standard deviation as text."""
    mean, median, deviation = values
    return f"{mean:.2f} / {median:.2f} / {deviation:.2f}"


def format_interval(interval):
    """Format a confidence interval as text."""
    return f"[{interval[0]:.2f}, {interval[1]:.2f}]"


def operation_lines(measurements, operation, xmi_column, ipfs_column):
    """Return the summary lines of one operation."""
    lines = [f"## {operation.capitalize()}", ""]
    lines.append("| Elements | XMI file | IPFS | Difference (ms) | Difference 95% CI | Ratio | Ratio 95% CI "
                 "| Wilcoxon p | Rank-biserial r |")
    lines.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
    for element_count in sorted(measurements["Elements"].unique()):
        rows = measurements[measurements["Elements"] == element_count]
        result = compare(rows, xmi_column, ipfs_column)
        lines.append(
            f"| {element_count:,} | {format_statistics(result['xmi'])} | "
            f"{format_statistics(result['ipfs'])} | {result['difference']:.2f} | "
            f"{format_interval(result['difference_interval'])} | {result['ratio']:.2f} | "
            f"{format_interval(result['ratio_interval'])} | {result['p_value']:.2e} | "
            f"{result['effect_size']:.2f} |"
        )
    lines.append("")
    return lines


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
    lines.append("Values are mean / median / standard deviation in milliseconds. The confidence intervals "
                 f"come from {BOOTSTRAP_RESAMPLES:,} paired bootstrap resamples (seed {BOOTSTRAP_SEED}).")
    lines.append("")

    for operation, xmi_column, ipfs_column in OPERATIONS:
        lines.extend(operation_lines(measurements, operation, xmi_column, ipfs_column))

    SUMMARY_FILE.write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
