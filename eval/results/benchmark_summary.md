# Benchmark summary

Measurements: 150 rows, 30 iterations per model size, three warm-up iterations before each series.

Values are mean / median / standard deviation in milliseconds. The confidence intervals come from 10,000 paired bootstrap resamples (seed 20261023).

## Save

| Elements | XMI file | IPFS | Difference (ms) | Difference 95% CI | Ratio | Ratio 95% CI | Wilcoxon p | Rank-biserial r |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 100 | 0.46 / 0.41 / 0.15 | 43.56 / 41.49 / 4.45 | 43.11 | [41.61, 44.76] | 95.19 | [84.17, 107.31] | 1.86e-09 | 1.00 |
| 1,000 | 0.57 / 0.50 / 0.21 | 41.00 / 40.76 / 1.11 | 40.43 | [40.07, 40.85] | 71.70 | [62.40, 79.73] | 1.86e-09 | 1.00 |
| 10,000 | 2.77 / 2.75 / 0.18 | 45.50 / 45.64 / 0.93 | 42.73 | [42.40, 43.05] | 16.43 | [16.03, 16.81] | 1.86e-09 | 1.00 |
| 100,000 | 30.88 / 30.08 / 1.59 | 87.83 / 87.96 / 1.76 | 56.95 | [56.22, 57.65] | 2.84 | [2.79, 2.89] | 1.86e-09 | 1.00 |
| 500,000 | 186.15 / 186.24 / 9.65 | 308.65 / 310.42 / 10.92 | 122.51 | [118.46, 126.65] | 1.66 | [1.63, 1.69] | 1.86e-09 | 1.00 |

## Load

| Elements | XMI file | IPFS | Difference (ms) | Difference 95% CI | Ratio | Ratio 95% CI | Wilcoxon p | Rank-biserial r |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 100 | 1.27 / 1.13 / 0.35 | 1.97 / 1.99 / 0.49 | 0.70 | [0.52, 0.87] | 1.55 | [1.39, 1.71] | 6.34e-06 | 0.94 |
| 1,000 | 1.53 / 1.36 / 0.40 | 2.05 / 1.93 / 0.39 | 0.52 | [0.38, 0.67] | 1.34 | [1.23, 1.46] | 6.15e-08 | 0.96 |
| 10,000 | 7.86 / 7.96 / 0.36 | 9.20 / 8.96 / 0.89 | 1.34 | [1.08, 1.62] | 1.17 | [1.14, 1.21] | 1.86e-09 | 1.00 |
| 100,000 | 75.18 / 73.83 / 2.57 | 92.14 / 91.00 / 3.95 | 16.96 | [15.35, 18.61] | 1.23 | [1.20, 1.25] | 1.86e-09 | 1.00 |
| 500,000 | 399.85 / 396.92 / 18.05 | 503.75 / 506.65 / 21.96 | 103.90 | [95.05, 113.11] | 1.26 | [1.24, 1.29] | 1.86e-09 | 1.00 |

## Practicality threshold

The README calls the IPFS path impractical where the path takes more than 5 times as long as the local XMI path.

- Save: the ratio crosses 5 at about 47,678 elements (interpolated on logarithmic axes between the measured sizes).
- Load: the ratio stays below the threshold at every measured size.
