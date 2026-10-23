# Baseline results

Results of `eval/baselines/run.sh` (see `eval/baselines/README.md`). Each run writes four files with its label as the name:

- `<label>.csv`: one row per store, model size, measured iteration and phase (`store,elements,iteration,phase,milliseconds`);
- `<label>-summary.md`: tables that compare write, read, and read with visit across the stores, then every phase per store, as mean, standard deviation and median;
- `<label>-environment.txt`: commit, core plug-in jar, machine, load average at the start and tool versions;
- `<label>.log`: the console output, with the size of each bare Git repository after its model size.

`validation.*` comes from `run.sh validate` on 2026-10-23, while other experiments ran on the machine (load average 4.9 at the start). Its times only show that the harness works. `final.*` will hold the run for the paper.
