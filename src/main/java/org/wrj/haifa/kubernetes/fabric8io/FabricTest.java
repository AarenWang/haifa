package org.wrj.haifa.kubernetes.fabric8io;


import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.JobList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.extensions.NetworkPolicy;
import io.fabric8.kubernetes.api.model.extensions.NetworkPolicyBuilder;
import io.fabric8.kubernetes.client.AutoAdaptableKubernetesClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceVisitFromServerGetWatchDeleteRecreateWaitApplicable;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.CORBA.Util;
import java.util.HashMap;
import java.util.List;


public class FabricTest {

    public static void main(String[] args) {

        KubernetesClient client = getKubernetesClient();
        System.out.println(client);
        PodList podList = client.pods().list();
        System.out.println(podList);

        testJob(client);
        //testPod(client);


    }


    static KubernetesClient getKubernetesClient(){
        Config config = new ConfigBuilder().withMasterUrl("http://127.0.0.1:8001/").withNamespace("default").build();
        //KubernetesClient client = new DefaultKubernetesClient(config);


        KubernetesClient client = new AutoAdaptableKubernetesClient(config);
        return  client;
    }

    static  void  testJob(KubernetesClient client){

        Job job = client.extensions().jobs().withName("process-item-apple").get();
        System.out.println(job);
        List<Job> jobList0  = client.extensions().jobs().withLabel("jobgroup","jobexample").list().getItems();


        List<Job> jobList = client.extensions().jobs().list().getItems();
        for(Job j : jobList){
            if(j.getMetadata().getName().equals("process-item-apple")){
                System.out.printf("name %s job was found \n",j.getMetadata().getName());

                String uid = j.getMetadata().getUid();
                List<Pod> podList = client.pods().list().getItems();
                for (Pod pod : podList){

                    if(StringUtils.equals(pod.getMetadata().getLabels().get("controller-uid"),uid)){
                        String podLog = client.pods().withName(pod.getMetadata().getName()).getLog();
                        System.out.println(podLog);
                    }
                }

            }

        }


        System.out.println(jobList);

    }
    static  void testPod(KubernetesClient client){
        String name="kubernetes-bootcamp-5c69669756-zf4ht";
        Pod pod = client.pods().withName("process-item-apple-xsvbs").get();
        String logStr = client.pods().withName(name).getLog();
        System.out.println(logStr);


        PodList podList = client.pods().withLabel("job-name","process-item-apple").list();
        //Assert.assertNotNull(pod);
        pod = podList.getItems().get(0);



    }

}
