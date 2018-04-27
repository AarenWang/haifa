package org.wrj.haifa.kubernetes.fabric8io;


import io.fabric8.kubernetes.api.model.extensions.NetworkPolicy;
import io.fabric8.kubernetes.api.model.extensions.NetworkPolicyBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceVisitFromServerGetWatchDeleteRecreateWaitApplicable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.CORBA.Util;
import java.util.HashMap;


public class FabricTest {

    public static void main(String[] args) {

        KubernetesClient client = getKubernetesClient();
        System.out.println(client);



    }


    static  KubernetesClient  getKubernetesClient(){
        Config config = new ConfigBuilder().withMasterUrl("https://192.168.99.100.8443").build();
        KubernetesClient client = new DefaultKubernetesClient();
        return  client;
    }
}
