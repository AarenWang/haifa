package org.wrj.allspring.web.server;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.naming.resources.VirtualDirContext;

import java.io.File;

/**
 * Created by wangrenjun on 2017/10/17.
 */
public class SpringInternalWebTomcatRunner {

    public static void main(String[] args) throws Exception {
        String baseDir = System.getProperty("base_dir");
        baseDir = "/Users/wangrenjun/git/spring4/spring-web-internal/";
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8083);
        tomcat.setBaseDir(baseDir+"target/tomcat");
        tomcat.getConnector().setURIEncoding("UTF-8");
        Context ctx = tomcat.addWebapp("", new File(baseDir+"src/main/webapp").getAbsolutePath());
        File additionWebInfClasses = new File(baseDir+"target/classes");
        VirtualDirContext resources = new VirtualDirContext();
        resources.setExtraResourcePaths(baseDir+"/WEB-INF/classes=" + additionWebInfClasses);
        ctx.setResources(resources);
        tomcat.start();
        tomcat.getServer().await();
    }


}
