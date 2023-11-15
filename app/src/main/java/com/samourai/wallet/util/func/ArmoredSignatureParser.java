package com.samourai.wallet.util.func;

import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.strip;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringsBetween;

import org.apache.commons.lang3.StringUtils;

/**
 * Parsed armored signature like the below example into ArmoredSignature instance.
 * Returns null, if the parsing is not possible (not respecting armored signature format);
 * This does not verify the signature.
 *
 * armored signature e.g.:
 * -----BEGIN BITCOIN SIGNED MESSAGE-----
 * le contenu du message
 * -----BEGIN BITCOIN SIGNATURE-----
 * Version: Bitcoin-qt (1.0)
 * Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR
 *
 * HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=
 * -----END BITCOIN SIGNATURE-----
 */
public class ArmoredSignatureParser {

    private static final String MARKER_BEGIN_BITCOIN_SIGNED_MESSAGE = "-----BEGIN BITCOIN SIGNED MESSAGE-----";
    private static final String MARKER_BEGIN_BITCOIN_SIGNATURE = "-----BEGIN BITCOIN SIGNATURE-----";
    private static final String MARKER_END_BITCOIN_SIGNATURE = "-----END BITCOIN SIGNATURE-----";
    private static final String MARKER_ADDRESS = "Address:";
    private static final String MARKER_VERSION = "Version:";


    private final String address;
    private final String message;
    private final String signature;

    private ArmoredSignatureParser(final String address,
                                   final String message,
                                   final String signature) {
        this.address = address;
        this.message = message;
        this.signature = signature;
    }

    public static ArmoredSignatureParser parse(final String content) {

        final String strippedContent = strip(content);

        if (! startsWith(
                strippedContent,
                MARKER_BEGIN_BITCOIN_SIGNED_MESSAGE + System.lineSeparator())) return null;
        if (! endsWith(strippedContent, MARKER_END_BITCOIN_SIGNATURE)) return null;
        if (! StringUtils.contains(
                strippedContent,
                MARKER_BEGIN_BITCOIN_SIGNATURE + System.lineSeparator())) return null;

        if (countMatches(content, MARKER_BEGIN_BITCOIN_SIGNED_MESSAGE) != 1) return null;
        if (countMatches(content, MARKER_BEGIN_BITCOIN_SIGNATURE) != 1) return null;
        if (countMatches(content, MARKER_END_BITCOIN_SIGNATURE) != 1) return null;

        final String message = StringUtils.substringBetween(
                content,
                MARKER_BEGIN_BITCOIN_SIGNED_MESSAGE + System.lineSeparator(),
                System.lineSeparator() + MARKER_BEGIN_BITCOIN_SIGNATURE);
        if (StringUtils.length(message) == 0) return null;

        final String signaturePart = substringAfter(
                content,
                MARKER_BEGIN_BITCOIN_SIGNATURE + System.lineSeparator());

        if (countMatches(signaturePart, MARKER_ADDRESS) != 1) return null;
        if (countMatches(signaturePart, MARKER_VERSION) != 1) return null;

        final String[] addresses = substringsBetween(
                signaturePart,
                MARKER_ADDRESS,
                System.lineSeparator());
        if (addresses.length != 1) return null;
        final String address = strip(addresses[0]);

        final String[] signatures = substringsBetween(
                signaturePart,
                MARKER_ADDRESS + addresses[0],
                System.lineSeparator() + MARKER_END_BITCOIN_SIGNATURE);
        if (signatures.length != 1) return null;
        if (! startsWith(
                signatures[0],
                System.lineSeparator() + System.lineSeparator())) return null;
        final String signature = strip(signatures[0]);

        final String shouldHaveOnlyVersionInfo = StringUtils.substringBefore(
                signaturePart,
                System.lineSeparator() + MARKER_ADDRESS);
        if (! startsWith(shouldHaveOnlyVersionInfo, MARKER_VERSION)) return null;
        if (contains(shouldHaveOnlyVersionInfo, System.lineSeparator())) return null;

        return new ArmoredSignatureParser(address, message, signature);
    }

    public String getAddress() {
        return address;
    }

    public String getMessage() {
        return message;
    }

    public String getSignature() {
        return signature;
    }
}
