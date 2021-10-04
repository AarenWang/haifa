package org.wrj.haifa.nio.httpserver;

import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.ACCEPT_ENCODING;
import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.CONNECTION;
import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.CONTENT_ENCODING;
import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.CONTENT_LENGTH;
import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.CONTENT_TYPE;
import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.GZIP;
import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.KEEP_ALIVE;
import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.LAST_MODIFIED;
import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.NOT_FOUND_404;
import static org.wrj.haifa.nio.httpserver.ResponceHeaderBuilder.SERVER_ERROR_500;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.WeakHashMap;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wrj.haifa.nio.httpserver.RequestHeaderDecoder.Verb;



public class RequestHandler implements Runnable {

    private static final DateFormat formater = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    static {
        formater.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private ButterflySoftCache cache;
    private File currentFile;
    private Date lastModified;
    private List<RequestSegmentHeader> pendingRequestSegment = new ArrayList<RequestSegmentHeader>();
    private Map<SocketChannel, RequestHeaderDecoder> requestMap = new WeakHashMap<SocketChannel, RequestHeaderDecoder>();
    private NioHttpServer server;
    private String serverRoot;
    private String acceptEncoding;

    /**
     *
     * @param server
     *            {@link NioHttpServer} the server
     * @param wwwroot
     *            wwwroot
     * @param cache
     *            cache implementation
     */
    public RequestHandler(NioHttpServer server, String wwwroot,
                          ButterflySoftCache cache) {
        this.cache = cache;
        this.serverRoot = wwwroot;
        this.server = server;
    }

    public void processData(SocketChannel client, byte[] data, int count) {

        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);

        synchronized (pendingRequestSegment) {
            // add data
            pendingRequestSegment
                    .add(new RequestSegmentHeader(client, dataCopy));
            pendingRequestSegment.notify();
        }
    }

    @Override
    public void run() {

        RequestSegmentHeader requestData = null;
        RequestHeaderDecoder header = null;
        ResponceHeaderBuilder builder = new ResponceHeaderBuilder();
        byte[] head = null;
        byte[] body = null;
        String file = null;
        String mime = null;
        boolean zip = false;

        // wait for data
        while (true) {

            synchronized (pendingRequestSegment) {
                while (pendingRequestSegment.isEmpty()) {
                    try {
                        pendingRequestSegment.wait();
                    } catch (InterruptedException e) {
                    }
                }
                requestData = pendingRequestSegment.remove(0);
            }

            header = requestMap.get(requestData.client);
            if (header == null) {
                header = new RequestHeaderDecoder();
                requestMap.put(requestData.client, header);
            }
            try {
                if (header.appendSegment(requestData.data)) {
                    file = serverRoot + header.getResouce();
                    currentFile = new File(file);
                    mime = Util.getContentType(currentFile);
                    // logger.info(currentFile + "\t" + mime);
                    acceptEncoding = header.getHeader(ACCEPT_ENCODING);
                    // gzip text
                    zip = mime.contains("text")
                            && acceptEncoding != null
                            && (acceptEncoding.contains("gzip") || acceptEncoding
                            .contains("gzip"));
                    builder.clear(); // get ready for next request;

                    // always keep alive
                    builder.addHeader(CONNECTION, KEEP_ALIVE);
                    builder.addHeader(CONTENT_TYPE, mime);

                    // response body byte, exception throws here
                    body = Util.file2ByteArray(currentFile, zip);
                    builder.addHeader(CONTENT_LENGTH, body.length);
                    if (zip) {
                        // add zip header
                        builder.addHeader(CONTENT_ENCODING, GZIP);
                    }

                    // last modified header
                    lastModified = new Date(currentFile.lastModified());
                    builder.addHeader(LAST_MODIFIED,
                            formater.format(lastModified));

                    // response header byte
                    head = builder.getHeader();

                    // data is prepared, send out to the client
                    server.send(requestData.client, head);
                    if (body != null && header.getVerb() == Verb.GET)
                        server.send(requestData.client, body);

                    logger.info(header.getResouce() + " 200");

                }
            } catch (IOException e) {
                builder.addHeader(CONTENT_LENGTH, 0);
                builder.setStatus(NOT_FOUND_404);
                head = builder.getHeader();
                server.send(requestData.client, head);
                // cache 404 if case client make a mistake again
                cache.put(file, head, body);
                logger.error(header.getResouce() + " 404");

            } catch (Exception e) {
                // any other, it's a 505 error
                builder.addHeader(CONTENT_LENGTH, 0);
                builder.setStatus(SERVER_ERROR_500);
                head = builder.getHeader();
                server.send(requestData.client, head);
                logger.error("505 error", e);
            }
        }
    }
}

class RequestSegmentHeader {
    SocketChannel client;
    byte[] data;

    public RequestSegmentHeader(SocketChannel client, byte[] data) {
        this.client = client;
        this.data = data;
    }
}