package org.ecorefs.eval.bpmn;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes the identifier of a local file. A local path has no CID, so the
 * determinism check compares SHA-256 digests of the bytes instead.
 */
public final class Digests {

    /**
     * Prevents instances; the class offers one static operation.
     */
    private Digests() {
    }

    /**
     * Returns the SHA-256 digest of bytes in hexadecimal notation.
     *
     * @param bytes the content
     * @return the digest
     */
    public static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime supports SHA-256", e);
        }
    }
}
