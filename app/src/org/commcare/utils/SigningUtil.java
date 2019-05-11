package org.commcare.utils;

import android.util.Pair;

import org.commcare.util.Base64;
import org.commcare.util.Base64DecoderException;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Pattern;

/**
 * A set of helper methods for verifying whether a message was genuinely sent from HQ. Currently we
 * expcect the SMS in the format [commcare app - do not delete] link where the link resolves to
 * some string such as
 * <p>
 * Y2NhcHA6IGh0dHA6Ly9iaXQubHkvMU5JMUl6MyBzaWduYXR1cmU6IEvECygFUhiUH
 * 3TRjC0lClQrpLR7lG//IpDYpRH7ComtZRjTirteXmPyM9fRgbPZ9K6jG9zEms9WQj55Uo7jTujKNYThjU8rJJmWLouJBr/Yn
 * WobEupwzn6DP2FavPF1YLPbp0ZctOfymW3m4j3VZ0lR2dMOjmInMSBiInqICKid
 * <p>
 * Which is base 64 decoded decoded into:
 * <p>
 * ccapp: <profile link> signature: <binary signature>
 * <p>
 * And we can then verify that the profile link was in fact signed (using SHA256withRSA) by
 * the CommCareHQ private key
 *
 * @author Will Pride (wpride@dimagi.com)
 */
public class SigningUtil {

    private final static Pattern WHITELISTED_URL_HOSTS_REGEX =
            Pattern.compile("\\.commcarehq\\.org$");

    /**
     * Given a trimmed byte[] payload, return the parsed out download link and signature
     *
     * @return Pair of <Download Link, Signature>
     * @throws Exception Throw a generic exception if we fail during signature parse/verification
     */
    public static Pair<String, byte[]> getUrlAndSignatureFromPayload(byte[] payload) throws Exception {
        byte[] signatureBytes = getSignatureBytes(payload);
        byte[] messageBytes = getMessageBytes(payload);
        String downloadLink = getDownloadLink(messageBytes);
        return new Pair<>(downloadLink, signatureBytes);
    }

    /**
     * Given a base64 encoded URL, decode the URL, and return first line read
     * from accessing that URL
     */
    public static String convertEncodedUrlToPayload(String baseEncodedUrl)
            throws IOException, Base64DecoderException {
        return readURL(decodeUrl(baseEncodedUrl));
    }

    protected static String decodeUrl(String baseEncodedUrl)
            throws Base64DecoderException, UnsupportedEncodingException {
        String decodedUrl;
        if (baseEncodedUrl.startsWith("http://") || baseEncodedUrl.startsWith("https://")) {
            // for backwards compatibility, accept non-base64 encoded URLS
            // once all users have migrated to the new format
            // (info available on HQ?) we can remove this branch
            decodedUrl = baseEncodedUrl;
        } else {
            decodedUrl = new String(Base64.decode(baseEncodedUrl), "UTF-8");
        }
        assertWhitelistedUrlHost(decodedUrl);
        return decodedUrl;
    }

    /**
     * Very basic method to prevent spoofed SMSs from making CommCare hit malicious URLs.
     */
    private static void assertWhitelistedUrlHost(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(urlString + " is not a valid URL.");
        }

        String host = url.getHost();
        if (!WHITELISTED_URL_HOSTS_REGEX.matcher(host).find()) {
            throw new DisallowedSMSInstallURLException(url + " is not an approved URL.");
        }
    }

    // given the raw trimmed byte paylaod, return the message (everything before the signature)
    private static byte[] getMessageBytes(byte[] payload) {
        byte[] messageBytes = new byte[getSignatureStartIndex(payload)];
        for (int i = 0; i < getSignatureStartIndex(payload); i++) {
            messageBytes[i] = payload[i];
        }
        return messageBytes;
    }

    /**
     * Verify a string input against a provided signature using the default public key
     *
     * @param message   the string to validate
     * @param signature the signature bytes
     * @return The input string if the signature is valid, null if not verified
     * @throws SignatureException if we have an internal error during verification
     */
    public static String verifyMessageAndBytes(String message, byte[] signature) throws Exception {
        String keyString = GlobalConstants.TRUSTED_SOURCE_PUBLIC_KEY;
        return verifyMessageAndBytes(keyString, message, signature);
    }


    /**
     * Verify a string input against a provided signature using a specific public key
     *
     * @param keyString the RSA Public Key which the signature should be checked against.
     *                  base64 encoded raw DER file
     * @param message   the string to validate
     * @param signature the signature bytes
     * @return The input string if the signature is valid, null if not verified
     * @throws SignatureException if we have an internal error during verification
     */
    public static String verifyMessageAndBytes(String keyString, String message, byte[] signature)
            throws Exception {
        boolean success = verifyMessageSignatureHelper(keyString, message, signature);

        if (success) {
            return message;
        }
        return null;
    }

    /**
     * Given the raw message bytes not including the signature, convert to UTF-8 and parse out
     * the download link
     *
     * @param messageBytes the raw bytes of the message payload (not the signature)
     * @return the parsed out profile link
     */
    private static String getDownloadLink(byte[] messageBytes) throws Exception {
        String textMessage = new String(messageBytes, "UTF-8");
        return textMessage.substring(textMessage.indexOf("ccapp: ") + "ccapp: ".length(),
                textMessage.indexOf("signature") - 1);
    }

    /**
     * Get the byte representation of the signature from the plaintext. We have to pull this out
     * directly because the conversion from Base64 can have a non-1:1 correspondence with the actual
     * bytes
     *
     * @return the binary representation of the signtature
     */
    private static byte[] getSignatureBytes(byte[] messageBytes) {
        int lastSpaceIndex = getSignatureStartIndex(messageBytes);
        int signatureByteLength = messageBytes.length - lastSpaceIndex;
        byte[] signatureBytes = new byte[signatureByteLength];
        for (int i = 0; i < signatureByteLength; i++) {
            signatureBytes[i] = messageBytes[i + lastSpaceIndex];
        }
        return signatureBytes;
    }

    /**
     * Iterate through the byte array until we find the third "space" character (represented
     * by integer 32) and then return its index
     *
     * @param messageBytes the raw bytes of the Base64 message
     * @return index of the third "space" byte, -1 if none encountered
     */
    private static int getSignatureStartIndex(byte[] messageBytes) {
        int index = 0;
        int spaceCount = 0;
        int spaceByte = 32;
        for (byte b : messageBytes) {
            if (b == spaceByte) {
                if (spaceCount == 2) {
                    return index + 1;
                } else {
                    spaceCount++;
                }
            }
            index++;
        }
        return -1;
    }

    // given a text message, return the raw Base64 bytes
    public static byte[] getBytesFromString(String stringMessage) throws Exception {
        return Base64.decode(stringMessage);
    }

    // given a text message, trim out the [commcare app - do not delete] and return
    public static String trimMessagePayload(String newMessage) {
        return newMessage.substring(newMessage.indexOf(GlobalConstants.SMS_INSTALL_KEY_STRING) +
                GlobalConstants.SMS_INSTALL_KEY_STRING.length() + 1);
    }

    /**
     * @param publicKeyString  the known public key of CCHQ
     * @param message          the message content
     * @param messageSignature the signature generated by HQ with its private key and the message content
     * @return whether or not the message was verified to be sent with HQ's private key
     */
    private static boolean verifyMessageSignatureHelper(String publicKeyString, String message, byte[] messageSignature) throws Base64DecoderException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        PublicKey publicKey = getPublicKey(publicKeyString);
        return verifyMessageSignature(publicKey, message, messageSignature);
    }

    // convert from a key string to a PublicKey object
    private static PublicKey getPublicKey(String key)
            throws Base64DecoderException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] derPublicKey = Base64.decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(derPublicKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    private static boolean verifyMessageSignature(PublicKey publicKey,
                                                  String messageString, byte[] signature)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        Signature sign = Signature.getInstance("SHA256withRSA/PSS", new BouncyCastleProvider());
        byte[] message = messageString.getBytes();
        sign.initVerify(publicKey);
        sign.update(message);
        return sign.verify(signature);
    }

    /**
     * Read the data from the URL arg and return as a string (only return first line)
     */
    private static String readURL(String url) throws IOException {
        String acc = "";
        URL oracle = new URL(url);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(oracle.openStream()));
        String inputLine;
        // only return the first line
        if ((inputLine = in.readLine()) != null)
            acc = inputLine;
        in.close();
        return acc;
    }

    public static class DisallowedSMSInstallURLException extends RuntimeException {
        public DisallowedSMSInstallURLException(String message) {
            super(message);
        }
    }
}
