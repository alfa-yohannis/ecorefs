# Benchmark summary

Measurements: 150 rows, 30 iterations per model size, three warm-up iterations before each series.

Values are mean / median / standard deviation in milliseconds. The confidence intervals come from 10,000 paired bootstrap resamples (seed 20261023).

## Save

| Elements | XMI file | IPFS | Difference (ms) | Difference 95% CI | Ratio | Ratio 95% CI | Wilcoxon p | Rank-biserial r |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 100 | 0.69 / 0.65 / 0.29 | 21.20 / 20.15 / 3.96 | 20.51 | [19.55, 22.11] | 30.62 | [26.20, 36.34] | 1.86e-09 | 1.00 |
| 1,000 | 0.99 / 0.98 / 0.40 | 21.28 / 20.99 / 1.07 | 20.29 | [20.02, 20.61] | 21.50 | [18.96, 24.67] | 1.86e-09 | 1.00 |
| 10,000 | 3.10 / 3.01 / 0.30 | 25.07 / 25.14 / 0.84 | 21.97 | [21.68, 22.26] | 8.08 | [7.82, 8.35] | 1.86e-09 | 1.00 |
| 100,000 | 31.50 / 29.67 / 7.43 | 77.73 / 82.08 / 9.97 | 46.23 | [41.97, 50.36] | 2.47 | [2.24, 2.68] | 1.86e-09 | 1.00 |
| 500,000 | 311.14 / 309.83 / 24.57 | 423.25 / 421.51 / 19.53 | 112.11 | [99.76, 123.66] | 1.36 | [1.31, 1.41] | 3.73e-09 | 1.00 |

## Load

| Elements | XMI file | IPFS | Difference (ms) | Difference 95% CI | Ratio | Ratio 95% CI | Wilcoxon p | Rank-biserial r |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 100 | 2.16 / 2.07 / 0.82 | 2.43 / 2.28 / 0.77 | 0.28 | [-0.10, 0.66] | 1.13 | [0.96, 1.34] | 3.28e-01 | 0.21 |
| 1,000 | 2.51 / 2.34 / 0.68 | 3.31 / 3.14 / 1.01 | 0.80 | [0.47, 1.15] | 1.32 | [1.18, 1.48] | 2.32e-04 | 0.73 |
| 10,000 | 7.81 / 7.58 / 0.95 | 10.23 / 9.97 / 1.12 | 2.42 | [1.94, 2.91] | 1.31 | [1.24, 1.38] | 1.30e-08 | 0.98 |
| 100,000 | 69.41 / 69.35 / 1.38 | 88.86 / 88.78 / 4.67 | 19.44 | [17.85, 21.29] | 1.28 | [1.26, 1.31] | 1.86e-09 | 1.00 |
| 500,000 | 505.03 / 503.58 / 21.26 | 683.87 / 667.48 / 48.97 | 178.84 | [162.74, 197.21] | 1.35 | [1.32, 1.39] | 1.86e-09 | 1.00 |

## Practicality threshold

The README calls the IPFS path impractical where the path takes more than 5 times as long as the local XMI path.

- Save: the ratio crosses 5 at about 25,400 elements (interpolated on logarithmic axes between the measured sizes).
- Load: the ratio stays below the threshold at every measured size.
