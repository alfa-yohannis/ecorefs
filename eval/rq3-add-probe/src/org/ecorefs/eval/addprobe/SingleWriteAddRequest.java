package org.ecorefs.eval.addprobe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Adds a file with a request that carries the whole multipart body in one
 * write, with a fixed length and without "Expect: 100-continue". The request
 * holds the same form field as the request of the Java IPFS client, and it can
 * leave out the pin that the node otherwise records for the added file.
 */
public final class SingleWriteAddRequest implements AddRequest {

    /** Separator of the multipart body. */
    private static final String BOUNDARY = "ecorefsProbeBoundary7MA4YWxkTrZu0gW";

    /** Line end of the multipart format. */
    private static final String LINE_END = "\r\n";

    /** Address of the add command, for example {@code http://127.0.0.1:5001/api/v0/add}. */
    private final URL addUrl;

    /** Whether the node pins the added file, which is the default of Kubo. */
    private final boolean pin;

    /**
     * Prepares requests for the node.
     *
     * @param apiUrl base URL of the API, for example {@code http://127.0.0.1:5001}
     * @param pin    whether the node pins the added file
     * @throws IOException when the URL is malformed
     */
    public SingleWriteAddRequest(String apiUrl, boolean pin) throws IOException {
        this.addUrl = new URL(apiUrl + "/api/v0/add?stream-channels=true&pin=" + pin);
        this.pin = pin;
    }

    /**
     * Sends the file in one write and reads the answer of the node.
     *
     * @param file the file to add
     * @return the answer of the node, which holds the CID
     * @throws IOException when the request fails
     */
    @Override
    public String add(Path file) throws IOException {
        byte[] body = buildBody(file);
        HttpURLConnection connection = (HttpURLConnection) addUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }
        try (InputStream input = connection.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    /**
     * Builds the multipart body with one file part.
     *
     * @param file the file to add
     * @return the complete body
     * @throws IOException when the file cannot be read
     */
    private byte[] buildBody(Path file) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String header = "--" + BOUNDARY + LINE_END
                + "Content-Disposition: file; filename=\"" + file.getFileName() + "\"" + LINE_END
                + "Content-Type: application/octet-stream" + LINE_END + LINE_END;
        body.write(header.getBytes(StandardCharsets.UTF_8));
        body.write(Files.readAllBytes(file));
        body.write((LINE_END + "--" + BOUNDARY + "--" + LINE_END).getBytes(StandardCharsets.UTF_8));
        return body.toByteArray();
    }

    /**
     * Names the implementation in the output of the probe.
     *
     * @return {@code single-write}, or {@code single-write-no-pin} without the pin
     */
    @Override
    public String name() {
        return pin ? "single-write" : "single-write-no-pin";
    }
}
