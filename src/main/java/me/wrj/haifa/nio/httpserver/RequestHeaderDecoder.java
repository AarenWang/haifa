package me.wrj.haifa.nio.httpserver;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RequestHeaderDecoder {

    public static enum Verb {
        CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE
    }

    public static enum Version {
        HTTP10, HTTP11
    }

    private static CharsetDecoder decoder = Charset.forName("ISO-8859-1")
            .newDecoder();
    private static final byte[] END = new byte[] { 13, 10, 13, 10 };
    private static final byte[] GET = new byte[] { 71, 69, 84, 32 };
    private static final byte[] HEAD = new byte[] { 72, 69, 65, 68 };

    // private Version version;
    private boolean begin = false;

    private CharBuffer charBuffer = ByteBuffer.allocate(2048).asCharBuffer();

    private Map<String, String> headerMap = new TreeMap<String, String>();

    private String resouce;
    private Verb verb;

    public boolean appendSegment(byte[] segment) {
        int beginIndex = 0;

        if (begin == false) {

            if ((beginIndex = Util.subArray(segment, GET, 0)) != segment.length) {
                begin = true;
                headerMap.clear();
                verb = Verb.GET;

            } else if ((beginIndex = Util.subArray(segment, HEAD, 0)) != segment.length) {
                begin = true;
                headerMap.clear();
                verb = Verb.HEAD;

            } else {
                // not begin yet, and find no begin, just return false;
                return false;

            }
        }

        int endIndex = Util.subArrayFromEnd(segment, END, 0);
        ByteBuffer b = ByteBuffer.wrap(segment, beginIndex, endIndex);
        decoder.decode(b, charBuffer, endIndex != segment.length);
        if (endIndex != segment.length) {
            extractValueAndReset();
            return true;
        }
        return false;
    }

    private void extractValueAndReset() {
        charBuffer.flip();
        String head = charBuffer.toString();
        String[] lines = head.split("\r\n");
        String[] split = lines[0].split(" ");

        resouce = split[1];

        for (int i = 1; i < lines.length; ++i) {
            String[] temp = lines[i].split(":");
            headerMap.put(temp[0].trim(), temp[1].trim());
        }

        charBuffer.clear();
        decoder.reset();
        begin = false;
    }

    public String getHeader(String key) {
        return headerMap.get(key);
    }

    public Set<String> getHeaders() {
        return headerMap.keySet();
    }

    public String getResouce() {
        return resouce;
    }

    /**
     *
     * @return currently, only GET [71,69,84,32],and HEAD [72, 69, 65, 68] is
     *         supported
     */
    public Verb getVerb() {
        return verb;
    }

    public Version getVersion() {
        throw new RuntimeException("not implement yet");
        // return version;
    }
}
