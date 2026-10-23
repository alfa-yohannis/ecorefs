package org.ecorefs.eval.baselines;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings of one benchmark run, read from command-line options of the form
 * {@code --name value}. The defaults follow the RQ3 benchmark of the paper: the
 * five generated models, three warm-up iterations and thirty measured
 * iterations.
 */
public final class BenchmarkSettings {

    /** The options as given on the command line, without the leading dashes. */
    private final Map<String, String> options;

    /**
     * Creates the settings from options.
     *
     * @param options option names mapped to values
     */
    private BenchmarkSettings(Map<String, String> options) {
        this.options = options;
    }

    /**
     * Reads the settings from the command line.
     *
     * @param arguments pairs of option name and value, such as {@code --sizes 100,1000}
     * @return the settings
     * @throws IllegalArgumentException when an option has no value
     */
    public static BenchmarkSettings parse(String[] arguments) {
        Map<String, String> options = new HashMap<>();
        for (int index = 0; index < arguments.length; index += 2) {
            if (!arguments[index].startsWith("--") || index + 1 >= arguments.length) {
                throw new IllegalArgumentException("Expected --name value pairs, got " + arguments[index]);
            }
            options.put(arguments[index].substring(2), arguments[index + 1]);
        }
        return new BenchmarkSettings(options);
    }

    /**
     * Returns the element counts of the measured models.
     *
     * @return the sizes in the order of the run
     */
    public List<Integer> sizes() {
        List<Integer> sizes = new ArrayList<>();
        for (String size : option("sizes", "100,1000,10000,100000,500000").split(",")) {
            sizes.add(Integer.parseInt(size.trim()));
        }
        return sizes;
    }

    /**
     * Returns the names of the measured stores.
     *
     * @return store names such as {@code xmi} or {@code git}
     */
    public List<String> stores() {
        return List.of(option("stores", "xmi,ipfs,git,share,neoemf").split(","));
    }

    /**
     * Returns the number of unrecorded warm-up iterations per model size.
     *
     * @return the warm-up iterations
     */
    public int warmupIterations() {
        return Integer.parseInt(option("warmup", "3"));
    }

    /**
     * Returns the number of recorded iterations per model size.
     *
     * @return the measured iterations
     */
    public int measuredIterations() {
        return Integer.parseInt(option("iterations", "30"));
    }

    /**
     * Returns the folder with the generated models.
     *
     * @return the folder with {@code model_<elements>.xmi}
     */
    public Path modelDirectory() {
        return Paths.get(option("models", "/data2/projects/ecorefs/eval/custom_generator/output"));
    }

    /**
     * Returns the work folder for repositories, mounts and databases. The folder
     * should lie on the same local file system as the temporary files of the XMI
     * path.
     *
     * @return the work folder
     */
    public Path workDirectory() {
        return Paths.get(option("work", System.getProperty("java.io.tmpdir") + "/ecorefs-baselines"));
    }

    /**
     * Returns the folder that receives the result files.
     *
     * @return the result folder
     */
    public Path resultDirectory() {
        return Paths.get(option("results", "/data2/projects/ecorefs/eval/results/baselines"));
    }

    /**
     * Returns the label of the run, used in the names of the result files.
     *
     * @return the label, for example {@code validation}
     */
    public String label() {
        return option("label", "run");
    }

    /**
     * Returns the multiaddress of the Kubo API.
     *
     * @return the address, by default the container of this benchmark
     */
    public String ipfsAddress() {
        return option("ipfs", "/ip4/127.0.0.1/tcp/5103");
    }

    /**
     * Returns the port of the {@code git daemon}.
     *
     * @return the port number
     */
    public int gitPort() {
        return Integer.parseInt(option("git-port", "9419"));
    }

    /**
     * Returns the port of the SSH server behind the network share.
     *
     * @return the port number
     */
    public int sshPort() {
        return Integer.parseInt(option("ssh-port", "2222"));
    }

    /**
     * Returns how the NeoEMF MapDB back end stores multi-valued features.
     *
     * @return {@code indices}, {@code arrays} or {@code lists}
     */
    public String neoEmfMapping() {
        return option("neoemf-mapping", "indices");
    }

    /**
     * Returns an option or a default value.
     *
     * @param name         the option name without dashes
     * @param defaultValue the value when the option is missing
     * @return the value
     */
    private String option(String name, String defaultValue) {
        return options.getOrDefault(name, defaultValue);
    }
}
