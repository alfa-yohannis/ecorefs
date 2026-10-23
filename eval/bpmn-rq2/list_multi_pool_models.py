#!/usr/bin/env python3
"""Lists the BPMN models with two or more pools that run a process.

A pool counts when its participant names a process through processRef. The
output has one line per model with the number of such participants and the
number of processes of the file, and the Java driver splits these models into
one resource per process.

Usage: list_multi_pool_models.py <dataset-dir> <output-dir>
"""

import csv
import os
import sys
import xml.etree.ElementTree as ElementTree

BPMN_NAMESPACE = '{http://www.omg.org/spec/BPMN/20100524/MODEL}'
COLLECTIONS = ['camunda-bpmn-for-research', 'omg-miwg-test-suite', 'jbpm-tests',
               'flowable-tests', 'camunda7-tests']


def list_files(dataset_dir, collection):
    """Returns the files of a collection that still exist after the cleaning."""
    listing = os.path.join(dataset_dir, '_load-check', collection + '.lst')
    with open(listing, encoding='utf-8') as handle:
        names = [line.strip() for line in handle if line.strip()]
    return [name for name in names if os.path.exists(name)]


def count_pools(path):
    """Returns (participants with a process, processes) of one file, or None if it does not parse."""
    try:
        root = ElementTree.parse(path).getroot()
    except ElementTree.ParseError:
        return None
    participants = 0
    for collaboration in root.iter(BPMN_NAMESPACE + 'collaboration'):
        for participant in collaboration.iter(BPMN_NAMESPACE + 'participant'):
            if participant.get('processRef'):
                participants += 1
    processes = sum(1 for _ in root.iter(BPMN_NAMESPACE + 'process'))
    return participants, processes


def main():
    """Writes multi-pool-models.tsv into the output folder."""
    dataset_dir, output_dir = sys.argv[1], sys.argv[2]
    os.makedirs(output_dir, exist_ok=True)
    with open(os.path.join(output_dir, 'multi-pool-models.tsv'), 'w', newline='', encoding='utf-8') as out:
        writer = csv.writer(out, delimiter='\t', lineterminator='\n')
        writer.writerow(['collection', 'file', 'pools', 'processes'])
        for collection in COLLECTIONS:
            for path in list_files(dataset_dir, collection):
                counts = count_pools(path)
                if counts is not None and counts[0] >= 2:
                    writer.writerow([collection, os.path.relpath(path, dataset_dir), counts[0], counts[1]])


if __name__ == '__main__':
    main()
