package org.wrj.haifa.kubernetes.client;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class K8sClientExample {

    public static void main(String[] args) throws IOException,ApiException {

        ApiClient client = Config.defaultClient();

        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();


        BatchV1Api batchV1Api = new BatchV1Api();
        V1JobList v1JobList = batchV1Api.listNamespacedJob("default",null,null,null,null,null
        ,0,null,30,false);

        int size = v1JobList.getItems().size();
        System.out.println(size);

        for(V1Job v1Job : v1JobList.getItems()){

            String jobUid = v1Job.getMetadata().getUid();

            V1PodList v1PodList = api.listNamespacedPod("default","true",null,null,true,"web-uid="+jobUid,null,
                    null,null,false);


            V1Pod v1Pod = v1PodList.getItems().get(0);

            PodLogs logs = new PodLogs();
            InputStream is = logs.streamNamespacedPodLog("default",v1Pod.getMetadata().getName(),null,null,50,true);
            String logContent = CharStreams.toString(new InputStreamReader(is));

            System.out.println("logContent="+logContent);

        }




    }
}
