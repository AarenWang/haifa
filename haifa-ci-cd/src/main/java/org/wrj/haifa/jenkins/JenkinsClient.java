package org.wrj.haifa.jenkins;


import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class JenkinsClient {

    public static void main(String[] args) throws URISyntaxException,IOException {

        JenkinsServer jenkins = new JenkinsServer(new URI("http://localhost:8080"), "admin", "123456");
        Map<String,Job> allJobs = jenkins.getJobs();
        allJobs.entrySet().forEach(e -> {
            String jobName = e.getKey();
            Job job = e.getValue();
            System.out.println(jobName+":"+job.getUrl());

        });

        Job job = jenkins.getJob("haifa-pipeline");
        List<Build> allBuild =  ((JobWithDetails) job).getAllBuilds();
        allBuild.stream().forEach(e -> {
            System.out.printf(e.getNumber()+":"+e.getQueueId()+""+e.getUrl());

        });

        BuildWithDetails details = jenkins.getJob("haifa-pipeline").getLastBuild().details();
        System.out.println(details.getDisplayName()+":"+details.getDescription()+":"+details.getConsoleOutputText());

        QueueReference reference = job.build();
        System.out.println("ItemUrlPart="+reference.getQueueItemUrlPart());


    }
}
