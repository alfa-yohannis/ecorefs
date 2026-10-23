"""Picks the real BPMN files of the multi-node run and writes bpmn-sample.tsv.

The files come from the four collections whose terms allow redistribution: the
MIWG test suite and the test resources of jBPM, Flowable and Camunda 7. The
Camunda research collection stays out, because its terms keep it on an offline
node. The measured files sit at evenly spaced quantiles of the file size, from
the smallest file to the largest, and the warm-up files come first, picked next
to the median. The choice depends only on the dataset, so a second call writes
the same list.

    python3 -I eval/multinode/select_bpmn_sample.py [--measured 30] [--warmup 3]
"""

import argparse
import os

COLLECTIONS = ("omg-miwg-test-suite", "jbpm-tests", "flowable-tests", "camunda7-tests")
SUFFIXES = (".bpmn", ".bpmn2", ".xml")
HERE = os.path.dirname(os.path.abspath(__file__))
REPOSITORY = os.path.dirname(os.path.dirname(HERE))


def list_files(root):
    """Returns (bytes, path relative to root) for every BPMN file, smallest first."""
    files = []
    for collection in COLLECTIONS:
        for directory, subdirectories, names in os.walk(os.path.join(root, collection)):
            subdirectories[:] = sorted(name for name in subdirectories if name != ".git")
            for name in sorted(names):
                if name.endswith(SUFFIXES):
                    path = os.path.join(directory, name)
                    files.append((os.path.getsize(path), os.path.relpath(path, root)))
    files.sort()
    return files


def measured_positions(count, measured):
    """Positions at evenly spaced quantiles, from the first file to the last."""
    last = count - 1
    return [round(step * last / (measured - 1)) for step in range(measured)]


def warmup_positions(count, warmup, taken):
    """Positions next to the median that the measured files do not use."""
    positions = []
    candidate = count // 2
    while len(positions) < warmup:
        if candidate not in taken:
            positions.append(candidate)
        candidate += 1
    return positions


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=os.path.join(REPOSITORY, "eval", "datasets", "bpmn"))
    parser.add_argument("--out", default=os.path.join(HERE, "bpmn-sample.tsv"))
    parser.add_argument("--measured", type=int, default=30)
    parser.add_argument("--warmup", type=int, default=3)
    arguments = parser.parse_args()

    files = list_files(arguments.root)
    measured = measured_positions(len(files), arguments.measured)
    warmup = warmup_positions(len(files), arguments.warmup, set(measured))
    with open(arguments.out, "w", encoding="utf-8") as output:
        output.write("# BPMN files of the multi-node run, written by select_bpmn_sample.py.\n")
        output.write("# Collections: %s (%d files).\n" % (", ".join(COLLECTIONS), len(files)))
        output.write("# The first %d files are warm-up files next to the median size; the other %d\n"
                     % (len(warmup), len(measured)))
        output.write("# sit at evenly spaced size quantiles. Columns: path below eval/datasets/bpmn, bytes.\n")
        for position in warmup + measured:
            size, path = files[position]
            output.write("%s\t%d\n" % (path, size))
    print("%d of %d files, %d to %d bytes, written to %s"
          % (len(warmup) + len(measured), len(files), files[measured[0]][0], files[measured[-1]][0],
             arguments.out))


if __name__ == "__main__":
    main()
