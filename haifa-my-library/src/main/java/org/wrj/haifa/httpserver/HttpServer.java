package org.wrj.haifa.httpserver;

import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangrenjun on 2017/9/26.
 */
public class HttpServer {

    public static void main(String[] args) {
        HttpRequestHandler requestHandler = new HttpRequestHandler() {

            public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
                                                                                                IOException {
                response.setStatusCode(HttpStatus.SC_OK);
                response.setEntity(new StringEntity("some important message", ContentType.TEXT_PLAIN));
            }

        };
        HttpProcessor httpProcessor = new HttpProcessor() {

            @Override
            public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
                response.addHeader("Server-Name","Customer Apache Http Compenent");
                String jsessionId = (String) context.getAttribute("jsession-id");
                response.addHeader("jsessionid",jsessionId);


            }

            @Override
            public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                System.out.printf("request line %s \r\n",request.getRequestLine());
                Header[] headers = request.getAllHeaders();
                for(Header header : headers) {
                    System.out.printf("header name %s,header value %s \r\n",header.getName(),header.getValue());
                }

                context.setAttribute("jsession-id", UUID.randomUUID().toString());

            }
        };

        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(15000).setTcpNoDelay(true).build();

        final org.apache.http.impl.bootstrap.HttpServer server = ServerBootstrap.bootstrap().setListenerPort(8080).setHttpProcessor(httpProcessor).setSocketConfig(socketConfig).setExceptionLogger(new StdErrorExceptionLogger()).registerHandler("*",
                                                                                                                                                                                                                                                   requestHandler).create();

        try {
            server.start();
            server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException | IOException e) {

            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                System.out.println("Http Server is SHUTDOWN now bye bye!");
                server.shutdown(5, TimeUnit.SECONDS);
            }
        });
    }
}
