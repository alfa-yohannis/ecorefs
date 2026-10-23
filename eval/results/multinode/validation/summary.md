# Multi-node measurements

132 rows from run 20261023-095500. Each cell gives the median time in milliseconds with the interquartile range in brackets.

## Generated models

Topology `peered`:

| elements | bytes | operation | none | wan50 |
|---:|---:|---|---:|---:|
| 100 | 4,026 | save_a | 12.5 [11.8, 14.0] | 11.8 [11.0, 12.7] |
| 100 | 4,026 | cat_b | 23.4 [23.4, 23.6] | 221.2 [220.6, 222.0] |
| 100 | 4,026 | load_c | 29.5 [29.1, 30.4] | 252.6 [240.2, 267.3] |
| 1,000 | 49,500 | save_a | 19.7 [18.5, 20.7] | 15.3 [14.9, 16.8] |
| 1,000 | 49,500 | cat_b | 24.9 [24.8, 25.4] | 334.4 [323.9, 339.3] |
| 1,000 | 49,500 | load_c | 38.2 [37.7, 39.5] | 347.3 [342.4, 348.2] |

Topology `dht`:

| elements | bytes | operation | none | wan50 |
|---:|---:|---|---:|---:|
| 100 | 4,026 | save_a | 11.0 [10.6, 14.2] | 12.6 [11.3, 13.1] |
| 100 | 4,026 | cat_b | 1,008 [1,007, 1,019] | 1,710 [1,693, 1,717] |
| 100 | 4,026 | load_c | 1,034 [1,028, 1,034] | 1,688 [1,665, 1,692] |
| 1,000 | 49,500 | save_a | 15.1 [13.7, 16.6] | 13.6 [13.1, 17.6] |
| 1,000 | 49,500 | cat_b | 1,029 [1,018, 1,030] | 1,818 [1,805, 1,824] |
| 1,000 | 49,500 | load_c | 1,043 [1,041, 1,044] | 1,793 [1,606, 1,807] |

## Real BPMN files

| topology | profile | files | bytes min / median / max | add_a | cat_b | cat_b max |
|---|---|---:|---|---:|---:|---:|
| peered | none | 3 | 244 / 1,929 / 1,930 | 7.42 [7.18, 8.20] | 28.1 [18.1, 29.3] | 30.6 |
| peered | wan50 | 3 | 244 / 1,929 / 1,930 | 6.21 [6.18, 6.24] | 231.7 [175.2, 240.0] | 248.3 |
| dht | none | 3 | 241 / 1,926 / 1,927 | 6.63 [6.50, 7.65] | 1,030 [1,019, 1,035] | 1,041 |
| dht | wan50 | 3 | 242 / 1,927 / 1,928 | 7.13 [7.05, 8.39] | 1,673 [1,660, 1,678] | 1,683 |

## IPNS

| topology | profile | publications | publish_a | resolve_b | visible_b | timeouts | resolutions until visible, max |
|---|---|---:|---:|---:|---:|---:|---:|
| peered | none | 3 | 6.23 [6.18, 6.24] | 1.87 [1.75, 1.93] | 8.00 [7.93, 8.12] | 0 | 1 |
| peered | wan50 | 3 | 512.8 [508.7, 531.9] | 103.2 [99.1, 108.8] | 618.9 [617.4, 632.4] | 0 | 1 |
| dht | none | 3 | 6.82 [6.22, 8.16] | 4.69 [4.68, 5.50] | 11.5 [10.9, 13.7] | 0 | 1 |
| dht | wan50 | 3 | 229.9 [228.2, 236.2] | 606.3 [603.1, 608.1] | 839.8 [833.1, 844.3] | 0 | 1 |

## Connection to the publisher at the start of each fetch

| topology | profile | workload | operation | connected / all |
|---|---|---|---|---:|
| peered | none | bpmn | cat_b | 3 / 3 |
| peered | none | ipns | resolve_b | 3 / 3 |
| peered | none | models | cat_b | 6 / 6 |
| peered | none | models | load_c | 6 / 6 |
| peered | wan50 | bpmn | cat_b | 3 / 3 |
| peered | wan50 | ipns | resolve_b | 3 / 3 |
| peered | wan50 | models | cat_b | 6 / 6 |
| peered | wan50 | models | load_c | 6 / 6 |
| dht | none | bpmn | cat_b | 0 / 3 |
| dht | none | ipns | resolve_b | 0 / 3 |
| dht | none | models | cat_b | 0 / 6 |
| dht | none | models | load_c | 0 / 6 |
| dht | wan50 | bpmn | cat_b | 0 / 3 |
| dht | wan50 | ipns | resolve_b | 0 / 3 |
| dht | wan50 | models | cat_b | 0 / 6 |
| dht | wan50 | models | load_c | 0 / 6 |
