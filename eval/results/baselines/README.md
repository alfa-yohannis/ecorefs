# Baseline results

Results of `eval/baselines/run.sh` (see `eval/baselines/README.md`). Each run writes four files with its label as the name:

- `<label>.csv`: one row per store, model size, measured iteration and phase (`store,elements,iteration,phase,milliseconds`);
- `<label>-summary.md`: tables that compare write, read, and read with visit across the stores, then every phase per store, as mean, standard deviation and median;
- `<label>-environment.txt`: commit, core plug-in jar, machine, load average at the start and tool versions;
- `<label>.log`: the console output, with the size of each bare Git repository after its model size.

`validation.*` comes from `run.sh validate` on 2026-10-23, while other experiments ran on the machine (load average 4.9 at the start). Its times only show that the harness works.

`final.*` is the run of the paper (`tab:baselines`). It ran on 2026-10-23 from 20:25 to 20:49 on the HP Envy of the timed benchmarks (Intel Core i9-11900H, 32 GB), right after the RQ3 benchmark and with the other services of the machine stopped. The load average at the start (1.79) comes from the build and the RQ3 run before it. The one changed path that `final-environment.txt` counts is that file itself, which the script opens before it asks git.
