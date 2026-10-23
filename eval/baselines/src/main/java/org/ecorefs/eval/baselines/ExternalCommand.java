package org.ecorefs.eval.baselines;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Runs external programs such as {@code git}, {@code ssh-keygen} or
 * {@code sshfs}. A command that ends with a non-zero exit code is an error,
 * because a benchmark must not measure a failed operation.
 */
public final class ExternalCommand {

    /** No instances; the class only offers static helpers. */
    private ExternalCommand() {
    }

    /**
     * Runs a command to the end and returns the combined output.
     *
     * @param directory the working directory of the command
     * @param command   the program and the arguments
     * @return the standard output and the error output of the command
     * @throws IOException          when the command cannot start or ends with an error
     * @throws InterruptedException when the waiting thread is interrupted
     */
    public static String run(Path directory, String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(describe(command) + " ended with exit code " + exitCode + ": " + output.trim());
        }
        return output;
    }

    /**
     * Starts a long-running program, such as a server, and returns at once. The
     * output of the program goes to a log file.
     *
     * @param directory the working directory of the program
     * @param logFile   the file that receives the output
     * @param command   the program and the arguments
     * @return the running process
     * @throws IOException when the program cannot start
     */
    public static Process start(Path directory, Path logFile, String... command) throws IOException {
        return new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();
    }

    /**
     * Joins a command line for error messages.
     *
     * @param command the program and the arguments
     * @return the command as one line
     */
    private static String describe(String... command) {
        List<String> parts = Arrays.asList(command);
        return String.join(" ", parts);
    }
}
