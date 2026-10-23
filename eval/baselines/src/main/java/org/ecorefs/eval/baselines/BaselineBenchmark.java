package org.ecorefs.eval.baselines;

/**
 * Entry point of the baseline benchmark. Example:
 *
 * <pre>
 * java ... org.ecorefs.eval.baselines.BaselineBenchmark --sizes 100,1000 --warmup 1 --iterations 3 --label validation
 * </pre>
 *
 * Options: {@code --sizes}, {@code --stores}, {@code --warmup},
 * {@code --iterations}, {@code --models}, {@code --work}, {@code --results},
 * {@code --label}, {@code --ipfs}, {@code --git-port}, {@code --ssh-port} and
 * {@code --neoemf-mapping} (see {@link BenchmarkSettings}).
 */
public final class BaselineBenchmark {

    /** No instances; the class only offers the entry point. */
    private BaselineBenchmark() {
    }

    /**
     * Runs the benchmark with the given options.
     *
     * @param arguments pairs of option name and value
     * @throws Exception when the run fails
     */
    public static void main(String[] arguments) throws Exception {
        new BenchmarkRunner(BenchmarkSettings.parse(arguments)).run();
    }
}
