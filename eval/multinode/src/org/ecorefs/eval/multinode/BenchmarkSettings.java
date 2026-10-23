package org.ecorefs.eval.multinode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Settings of one start of the harness, read from options of the form
 * --name value. run.sh passes the options; every option has a default that
 * matches the protocol of the paper's benchmark (three warm-up and thirty
 * measured iterations).
 */
public final class BenchmarkSettings {

    /** Option values by name, without the leading dashes. */
    private final Map<String, String> values;

    /**
     * Creates the settings from parsed option values.
     *
     * @param values option values by name
     */
    private BenchmarkSettings(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Parses the command line.
     *
     * @param args options as pairs of --name and value
     * @return the settings
     * @throws IllegalArgumentException when an option has no value or no leading dashes
     */
    public static BenchmarkSettings parse(String[] args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Options come in pairs of --name and value");
        }
        Map<String, String> values = new HashMap<>();
        for (int index = 0; index < args.length; index += 2) {
            if (!args[index].startsWith("--")) {
                throw new IllegalArgumentException("Not an option: " + args[index]);
            }
            values.put(args[index].substring(2), args[index + 1]);
        }
        return new BenchmarkSettings(values);
    }

    /**
     * Returns the identifier of the run, which every row of the result file carries.
     *
     * @return the run identifier
     */
    public String run() {
        return text("run", "manual");
    }

    /**
     * Returns the topology of the network: peered or dht.
     *
     * @return the topology
     */
    public String topology() {
        return text("topology", "peered");
    }

    /**
     * Returns the delay profile of the network, which netem.sh applied before the start.
     *
     * @return the profile, such as wan50
     */
    public String profile() {
        return text("profile", "none");
    }

    /**
     * Returns the number of unrecorded warm-up iterations.
     *
     * @return the warm-up iterations
     */
    public int warmup() {
        return Integer.parseInt(text("warmup", "3"));
    }

    /**
     * Returns the number of measured iterations per model size and of measured
     * IPNS publications.
     *
     * @return the measured iterations
     */
    public int iterations() {
        return Integer.parseInt(text("iterations", "30"));
    }

    /**
     * Returns the number of measured BPMN files; the warm-up files come before them.
     *
     * @return the measured BPMN files
     */
    public int bpmnFiles() {
        return Integer.parseInt(text("bpmn-files", "30"));
    }

    /**
     * Returns the element counts of the generated models.
     *
     * @return the element counts
     */
    public int[] sizes() {
        String[] parts = text("sizes", "100,1000,10000,100000,500000").split(",");
        int[] sizes = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            sizes[index] = Integer.parseInt(parts[index].trim());
        }
        return sizes;
    }

    /**
     * Tells whether a workload belongs to the run.
     *
     * @param workload models, bpmn or ipns
     * @return true when the run includes the workload
     */
    public boolean includes(String workload) {
        Set<String> workloads = new HashSet<>(Arrays.asList(text("workloads", "models,bpmn,ipns").split(",")));
        return workloads.contains(workload);
    }

    /**
     * Returns the folder of the generated models.
     *
     * @return the folder eval/custom_generator/output
     */
    public Path modelsDirectory() {
        return Paths.get(text("models-dir", "eval/custom_generator/output"));
    }

    /**
     * Returns the sample list of the BPMN files.
     *
     * @return the file bpmn-sample.tsv
     */
    public Path bpmnSample() {
        return Paths.get(text("bpmn-sample", "eval/multinode/bpmn-sample.tsv"));
    }

    /**
     * Returns the folder of the cleaned BPMN collections.
     *
     * @return the folder eval/datasets/bpmn
     */
    public Path bpmnRoot() {
        return Paths.get(text("bpmn-root", "eval/datasets/bpmn"));
    }

    /**
     * Returns the CSV file that receives the measurements.
     *
     * @return the result file
     */
    public Path output() {
        return Paths.get(text("out", "eval/results/multinode/metrics.csv"));
    }

    /**
     * Returns the host of the RPC APIs of the nodes.
     *
     * @return the host, 127.0.0.1 by default
     */
    public String host() {
        return text("host", "127.0.0.1");
    }

    /**
     * Returns the port of the RPC API of one node.
     *
     * @param node a, b or c
     * @return the port
     */
    public int port(String node) {
        String defaultPort;
        if ("a".equals(node)) {
            defaultPort = "5201";
        } else if ("b".equals(node)) {
            defaultPort = "5202";
        } else {
            defaultPort = "5203";
        }
        return Integer.parseInt(text("port-" + node, defaultPort));
    }

    /**
     * Returns an option value or a default.
     *
     * @param name         the option name without dashes
     * @param defaultValue the value when the option is missing
     * @return the value
     */
    private String text(String name, String defaultValue) {
        String value = values.get(name);
        return value == null ? defaultValue : value;
    }
}
