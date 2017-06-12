package me.wrj.haifa.httpclient;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContexts;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Map;

/**
 * Created by wangrenjun on 2017/6/6.
 */

public class HttpsClientTest {

    @Test
    public void testWeibo() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("https://weibo.com:8443/");
        CloseableHttpResponse response = httpclient.execute(httpget);
        System.out.println(response.getStatusLine() + ":" + IOUtils.toString(response.getEntity().getContent()));

    }

    public void testTmall() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("https://tmall.com:8443/");
        CloseableHttpResponse response = httpclient.execute(httpget);
        System.out.println(response.getStatusLine() + ":" + IOUtils.toString(response.getEntity().getContent()));

    }



    @Test
    public void testWithLoadTrustStore() throws Exception {

        SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(new File(System.getProperty("user.home")
                                                                                + "/dev/https_config/tomcat/weibo_server.keystore"),
                                                                       "weiboweibo".toCharArray(),
                                                                       new TrustSelfSignedStrategy()).build();

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null,
                                                                          SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        try {

            HttpGet httpget = new HttpGet("https://weibo.com:8443/");

            System.out.println("Executing request " + httpget.getRequestLine());

            CloseableHttpResponse response = httpclient.execute(httpget);
            System.out.println(response.getStatusLine() + ":" + IOUtils.toString(response.getEntity().getContent()));

        } finally {
            httpclient.close();
        }

    }

    @Test
    public void testWithSystemProperty() throws Exception {

        System.setProperty("javax.net.ssl.trustStore",
                           System.getProperty("user.home") + "/dev/https_config/tomcat/weibo_server.keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "weiboweibo");

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("https://weibo.com:8443/");
        CloseableHttpResponse response = httpclient.execute(httpget);
        System.out.println(response.getStatusLine() + ":" + IOUtils.toString(response.getEntity().getContent()));

    }



    /**
     * 百度 CA根证书在Java内置，可以直接访问
     * @throws Exception
     */
    @Test
    public void testBaiduHttps() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("https://www.baidu.com/");
        CloseableHttpResponse response = httpclient.execute(httpget);
        System.out.println(response.getStatusLine() + ":" + IOUtils.toString(response.getEntity().getContent()));

    }

}
