#!/usr/bin/env python3
"""Rebuilds the cross-file call links that the cleaning of the BPMN collections removed.

The cleaning removed every calledElement value that named a process in another
file (logged in _load-check/clean/<collection>.changes.tsv). This script finds,
for each removed value, the file of the same collection that defines a process
with that id, and writes one line per resolved link. A value is resolved by the
first rule that leaves exactly one candidate file:

  unique-in-collection  only one other file of the collection defines the id
  unique-in-folder      only one candidate lies in the folder of the caller
  name-matches-id       only one candidate in the folder is named after the id
  same-variant          only one candidate in the folder has the same name suffix,
                        for example "-roundtrip" in the MIWG test suite

Expressions such as ${...}, values without any defining file and values with
several candidates after all rules are written to the skipped file with the reason.

Usage: resolve_called_elements.py <dataset-dir> <output-dir>
"""

import csv
import os
import re
import sys
import xml.etree.ElementTree as ElementTree

BPMN_NAMESPACE = '{http://www.omg.org/spec/BPMN/20100524/MODEL}'
COLLECTIONS = ['camunda-bpmn-for-research', 'omg-miwg-test-suite', 'jbpm-tests',
               'flowable-tests', 'camunda7-tests']
TEST_CASE_STEM = re.compile(r'^[A-Z]\.\d+\.\d+')
BPMN_EXTENSIONS = ('.bpmn20.xml', '.bpmn2', '.bpmn', '.xml')


def list_files(dataset_dir, collection):
    """Returns the files of a collection that still exist after the cleaning."""
    listing = os.path.join(dataset_dir, '_load-check', collection + '.lst')
    with open(listing, encoding='utf-8') as handle:
        names = [line.strip() for line in handle if line.strip()]
    return [name for name in names if os.path.exists(name)]


def index_process_ids(files):
    """Maps each process id to the files that define a process with that id."""
    index = {}
    for path in files:
        for process_id in read_process_ids(path):
            index.setdefault(process_id, []).append(path)
    return index


def read_process_ids(path):
    """Returns the ids of the processes of one BPMN file, or nothing if it does not parse."""
    try:
        root = ElementTree.parse(path).getroot()
    except ElementTree.ParseError:
        return []
    return [element.get('id') for element in root.iter(BPMN_NAMESPACE + 'process') if element.get('id')]


def read_removed_calls(dataset_dir, collection):
    """Returns (caller file, call activity id, called value) for every removed calledElement."""
    log = os.path.join(dataset_dir, '_load-check', 'clean', collection + '.changes.tsv')
    calls = []
    with open(log, newline='', encoding='utf-8') as handle:
        for row in csv.DictReader(handle, delimiter='\t'):
            if row['attribute'] == 'calledElement' and row['action'] == 'REMOVE_ATTRIBUTE':
                activity_id = row['element'].split('#', 1)[1]
                called_value = row['old'].split('=', 1)[1]
                calls.append((row['file'], activity_id, called_value))
    return calls


def base_name(path):
    """Returns the file name without the BPMN extension."""
    name = os.path.basename(path)
    for extension in BPMN_EXTENSIONS:
        if name.endswith(extension):
            return name[:-len(extension)]
    return name


def variant(path):
    """Returns the part of the file name after the test case stem, such as "-roundtrip"."""
    return TEST_CASE_STEM.sub('', base_name(path))


def resolve(caller, called_value, index):
    """Returns (callee file, rule) for one call, or (None, reason) when no rule decides."""
    if '${' in called_value or '#{' in called_value:
        return None, 'expression'
    candidates = [path for path in index.get(called_value, []) if path != caller]
    if not candidates:
        return None, 'no-defining-file'
    if len(candidates) == 1:
        return candidates[0], 'unique-in-collection'
    in_folder = [path for path in candidates if os.path.dirname(path) == os.path.dirname(caller)]
    if len(in_folder) == 1:
        return in_folder[0], 'unique-in-folder'
    return resolve_within_folder(caller, called_value, in_folder)


def resolve_within_folder(caller, called_value, in_folder):
    """Applies the naming rules to several candidates in the folder of the caller."""
    if not in_folder:
        return None, 'several-outside-folder'
    named = [path for path in in_folder if base_name(path).lower() == called_value.lower()]
    if len(named) == 1:
        return named[0], 'name-matches-id'
    same_variant = [path for path in in_folder if variant(path) == variant(caller)]
    if len(same_variant) == 1:
        return same_variant[0], 'same-variant'
    return None, 'several-in-folder'


def main():
    """Resolves every collection and writes the links and the skipped calls."""
    dataset_dir, output_dir = sys.argv[1], sys.argv[2]
    os.makedirs(output_dir, exist_ok=True)
    links_path = os.path.join(output_dir, 'called-element-links.tsv')
    skipped_path = os.path.join(output_dir, 'called-element-skipped.tsv')
    with open(links_path, 'w', newline='', encoding='utf-8') as links, \
            open(skipped_path, 'w', newline='', encoding='utf-8') as skipped:
        link_writer = csv.writer(links, delimiter='\t', lineterminator='\n')
        skip_writer = csv.writer(skipped, delimiter='\t', lineterminator='\n')
        link_writer.writerow(['collection', 'caller', 'activity', 'called', 'callee', 'rule'])
        skip_writer.writerow(['collection', 'caller', 'activity', 'called', 'reason'])
        for collection in COLLECTIONS:
            index = index_process_ids(list_files(dataset_dir, collection))
            for caller, activity, called in read_removed_calls(dataset_dir, collection):
                callee, rule = resolve(caller, called, index)
                caller_name = os.path.relpath(caller, dataset_dir)
                if callee is None:
                    skip_writer.writerow([collection, caller_name, activity, called, rule])
                else:
                    link_writer.writerow([collection, caller_name, activity, called,
                                          os.path.relpath(callee, dataset_dir), rule])


if __name__ == '__main__':
    main()
