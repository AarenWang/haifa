package org.wrj.haifa.jenkins;

import com.offbytwo.jenkins.JenkinsServer;

import java.net.URI;
import java.net.URISyntaxException;

public class CreateJenkinsJOb {

    public static void main(String[] args) throws URISyntaxException {
        String jobXml = "";
        JenkinsServer jenkins = new JenkinsServer(new URI("http://localhost:8080"), "admin", "123456");



    }
}
