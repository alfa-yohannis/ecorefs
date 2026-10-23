package org.ecorefs.eval.multinode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Talks to the RPC API of one Kubo node of the experiment network. The harness
 * needs a few commands: add and cat for content, id and swarm for the topology,
 * and key and name for IPNS. Kubo accepts only POST requests on its RPC API, and
 * every response except the bytes of cat is JSON.
 */
public final class KuboClient {

    /** Time allowed to open a connection to the API. */
    private static final int CONNECT_TIMEOUT_MILLIS = 30_000;

    /** Time allowed for one response; a fetch over the emulated WAN can take minutes. */
    private static final int READ_TIMEOUT_MILLIS = 900_000;

    /** Boundary of the multipart body that add expects. */
    private static final String BOUNDARY = "ecorefs-multinode-boundary";

    /** Line break of the multipart format. */
    private static final String CRLF = "\r\n";

    /** Short name of the node, such as mn-a, for logs and result files. */
    private final String name;

    /** Host of the RPC API as seen from the harness. */
    private final String host;

    /** Port of the RPC API as seen from the harness. */
    private final int port;

    /**
     * Creates a client for one node.
     *
     * @param name short name of the node
     * @param host host of the RPC API
     * @param port port of the RPC API
     */
    public KuboClient(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    /**
     * Returns the short name of the node.
     *
     * @return the name, such as mn-a
     */
    public String name() {
        return name;
    }

    /**
     * Returns the multiaddress of the RPC API in the form that the Java IPFS
     * client of ecorefs expects.
     *
     * @return the multiaddress, such as /ip4/127.0.0.1/tcp/5201
     */
    public String apiMultiaddress() {
        return "/ip4/" + host + "/tcp/" + port;
    }

    /**
     * Adds content to the node as one file and pins it.
     *
     * @param content  the bytes of the file
     * @param fileName the name in the multipart body, which does not change the CID
     * @return the CID of the content
     * @throws IOException when the node rejects the request
     */
    public String add(byte[] content, String fileName) throws IOException {
        byte[] body = postMultipart("add", "pin=true", content, fileName);
        String[] lines = new String(body, StandardCharsets.UTF_8).trim().split("\n");
        JsonObject last = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        return last.get("Hash").getAsString();
    }

    /**
     * Reads the content of a CID through this node. When the node does not hold
     * the content, the node fetches it from the network first.
     *
     * @param cid the CID to read
     * @return the bytes of the content
     * @throws IOException when the node fails to deliver the content
     */
    public byte[] cat(String cid) throws IOException {
        return post("cat", "arg=" + encode(cid));
    }

    /**
     * Returns the peer ID of the node.
     *
     * @return the peer ID
     * @throws IOException when the node rejects the request
     */
    public String peerId() throws IOException {
        return postJson("id", "").get("ID").getAsString();
    }

    /**
     * Lists the peers that the node holds a connection to, as address with the
     * peer ID appended.
     *
     * @return one entry per connection, such as /ip4/172.30.0.3/tcp/4001/p2p/12D3...
     * @throws IOException when the node rejects the request
     */
    public List<String> connections() throws IOException {
        JsonObject response = postJson("swarm/peers", "");
        List<String> connections = new ArrayList<>();
        JsonElement peers = response.get("Peers");
        if (peers == null || !peers.isJsonArray()) {
            return connections;
        }
        for (JsonElement peer : peers.getAsJsonArray()) {
            JsonObject entry = peer.getAsJsonObject();
            connections.add(entry.get("Addr").getAsString() + "/p2p/" + entry.get("Peer").getAsString());
        }
        return connections;
    }

    /**
     * Tells whether the node holds a connection to a peer.
     *
     * @param peerId the peer ID to look for
     * @return true when a connection to the peer exists
     * @throws IOException when the node rejects the request
     */
    public boolean isConnectedTo(String peerId) throws IOException {
        for (String connection : connections()) {
            if (connection.endsWith("/p2p/" + peerId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Closes every connection of the node to a peer.
     *
     * @param peerId the peer to disconnect from
     * @throws IOException when the node rejects the request
     */
    public void disconnect(String peerId) throws IOException {
        for (String connection : connections()) {
            if (connection.endsWith("/p2p/" + peerId)) {
                closeConnection(connection);
            }
        }
    }

    /**
     * Removes every block that the node does not pin. A fetcher pins nothing, so
     * afterwards it holds no block of earlier fetches.
     *
     * @throws IOException when the node rejects the request
     */
    public void collectGarbage() throws IOException {
        post("repo/gc", "quiet=true");
    }

    /**
     * Closes one connection. Kubo answers with an error when the connection
     * closed between the listing and this call; that is no failure here, since
     * the connection policy checks the connections again afterwards.
     *
     * @param connection the address of the connection with the peer ID appended
     */
    private void closeConnection(String connection) {
        try {
            post("swarm/disconnect", "arg=" + encode(connection));
        } catch (IOException closedMeanwhile) {
            System.out.println(name + ": " + closedMeanwhile.getMessage());
        }
    }

    /**
     * Creates an Ed25519 key for IPNS unless the node already holds a key with
     * the name.
     *
     * @param keyName the name of the key
     * @throws IOException when the node rejects the request
     */
    public void ensureKey(String keyName) throws IOException {
        JsonArray keys = postJson("key/list", "").getAsJsonArray("Keys");
        for (JsonElement key : keys) {
            if (keyName.equals(key.getAsJsonObject().get("Name").getAsString())) {
                return;
            }
        }
        post("key/gen", "arg=" + encode(keyName) + "&type=ed25519");
    }

    /**
     * Publishes a CID under the IPNS name of a key.
     *
     * @param cid     the CID to publish
     * @param keyName the key that signs the record
     * @return the IPNS name
     * @throws IOException when the node fails to publish
     */
    public String publish(String cid, String keyName) throws IOException {
        String query = "arg=" + encode("/ipfs/" + cid) + "&key=" + encode(keyName);
        return postJson("name/publish", query).get("Name").getAsString();
    }

    /**
     * Resolves an IPNS name without the cache of the node, so the node asks the
     * network for the current record.
     *
     * @param ipnsName the IPNS name
     * @return the resolved path, such as /ipfs/Qm...
     * @throws IOException when the node fails to resolve the name
     */
    public String resolve(String ipnsName) throws IOException {
        return postJson("name/resolve", "arg=" + encode(ipnsName) + "&nocache=true").get("Path").getAsString();
    }

    /**
     * Sends a command without a body and parses the JSON response.
     *
     * @param command the RPC command, such as swarm/peers
     * @param query   the query string without the question mark
     * @return the parsed response
     * @throws IOException when the node rejects the request
     */
    private JsonObject postJson(String command, String query) throws IOException {
        byte[] body = post(command, query);
        return JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /**
     * Sends a command without a body.
     *
     * @param command the RPC command
     * @param query   the query string without the question mark
     * @return the bytes of the response
     * @throws IOException when the node rejects the request
     */
    private byte[] post(String command, String query) throws IOException {
        HttpURLConnection connection = open(command, query);
        try {
            connection.getOutputStream().close();
            return readResponse(connection);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Sends a command with one file in a multipart body.
     *
     * @param command  the RPC command
     * @param query    the query string without the question mark
     * @param content  the bytes of the file
     * @param fileName the name of the file part
     * @return the bytes of the response
     * @throws IOException when the node rejects the request
     */
    private byte[] postMultipart(String command, String query, byte[] content, String fileName) throws IOException {
        HttpURLConnection connection = open(command, query);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        String head = "--" + BOUNDARY + CRLF
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + CRLF
                + "Content-Type: application/octet-stream" + CRLF + CRLF;
        String tail = CRLF + "--" + BOUNDARY + "--" + CRLF;
        try {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(head.getBytes(StandardCharsets.UTF_8));
                output.write(content);
                output.write(tail.getBytes(StandardCharsets.UTF_8));
            }
            return readResponse(connection);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Opens a POST connection to a command of the RPC API.
     *
     * @param command the RPC command
     * @param query   the query string without the question mark
     * @return the connection, ready for the body
     * @throws IOException when the connection cannot be opened
     */
    private HttpURLConnection open(String command, String query) throws IOException {
        String address = "http://" + host + ":" + port + "/api/v0/" + command + (query.isEmpty() ? "" : "?" + query);
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        return connection;
    }

    /**
     * Reads the response of a request and turns an error status into an
     * exception that carries the message of the node.
     *
     * @param connection the connection after the request body was sent
     * @return the bytes of a successful response
     * @throws IOException when the status is not 2xx
     */
    private byte[] readResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status >= 200 && status < 300) {
            try (InputStream input = connection.getInputStream()) {
                return readAll(input);
            }
        }
        String message = "";
        InputStream error = connection.getErrorStream();
        if (error != null) {
            try (InputStream input = error) {
                message = new String(readAll(input), StandardCharsets.UTF_8);
            }
        }
        throw new IOException(name + " answered HTTP " + status + " to " + connection.getURL() + ": " + message);
    }

    /**
     * Reads a stream to the end.
     *
     * @param input the stream
     * @return every byte of the stream
     * @throws IOException when the stream fails
     */
    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[65_536];
        int read = input.read(chunk);
        while (read >= 0) {
            buffer.write(chunk, 0, read);
            read = input.read(chunk);
        }
        return buffer.toByteArray();
    }

    /**
     * Encodes a query value.
     *
     * @param value the raw value
     * @return the encoded value
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
