package org.wrj.haifa.velocity;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class VelocityTool {

    static String getVelocityText(String vmResource, Map<String,Object> map) throws IOException {
        Velocity.init();

        Velocity.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        Velocity.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");

        VelocityEngine ve = new VelocityEngine();
        ve.setProperty("file.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        VelocityContext context = new VelocityContext();
        for(String key : map.keySet()) {
            context.put(key,map.get(key));
        }

        StringWriter sw = new StringWriter();

        Template t = ve.getTemplate(vmResource);
        t.merge(context,sw);

        return sw.toString();
    }

    public static void main(String[] args) throws IOException{


        Map<String,Object> map = new HashMap<>();
        map.put("topic","consult_servce");
        map.put("kafka_broker_server","10.20.180.115:6063,10.20.180.134:6063,10.20.180.147:6063");
        String out = VelocityTool.getVelocityText("config.vm",map);
        System.out.println(out);
    }
}
