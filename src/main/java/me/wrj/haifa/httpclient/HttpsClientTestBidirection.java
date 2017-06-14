package me.wrj.haifa.httpclient;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.io.File;

/**
 * Created by wangrenjun on 2017/6/12. HTTPS双向认证测试
 */
public class HttpsClientTestBidirection {

    @Test
    public void testWithLoadCert() throws Exception {

        // Trust own CA and all self-signed certs
        SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(new File(System.getProperty("user.home")
                                                                                + "/dev/https_config/tomcat/bidirection/server/tmall_server.keystore"),
                                                                       "tmalltmall".toCharArray(),
                                                                       new TrustSelfSignedStrategy()).loadKeyMaterial(new File(System.getProperty("user.home")
                                                                                                                               + "/dev/https_config/tomcat/bidirection/client/tmall_client.p12"),
                                                                                                                      "tmallclient".toCharArray(),
                                                                                                                      "tmallclient".toCharArray()).build();

        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null,
                                                                          SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        try {

            HttpGet httpget = new HttpGet("https://tmall.com:8443/");

            System.out.println("Executing request " + httpget.getRequestLine());

            CloseableHttpResponse response = httpclient.execute(httpget);
            System.out.println(response.getStatusLine() + ":" + IOUtils.toString(response.getEntity().getContent()));

        } finally {
            httpclient.close();
        }

    }

    @Test
    public void testHTTPSWithSystemProperty() throws Exception {

        System.setProperty("javax.net.ssl.trustStore", System.getProperty("user.home")
                + "/dev/https_config/tomcat/bidirection/server/tmall_server.keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "tmalltmall");

        System.setProperty("javax.net.ssl.keyStore", System.getProperty("user.home")
                + "/dev/https_config/tomcat/bidirection/client/tmall_client.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "tmallclient");

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("https://tmall.com:8443/");
        CloseableHttpResponse response = httpclient.execute(httpget);
        System.out.println(response.getStatusLine() + ":" + IOUtils.toString(response.getEntity().getContent()));

    }
}
