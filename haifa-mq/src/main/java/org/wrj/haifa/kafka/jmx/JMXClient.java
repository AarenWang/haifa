package org.wrj.haifa.kafka.jmx;


import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;

public class JMXClient {

    public static void main(String[] args) throws IOException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        JMXServiceURL url = new JMXServiceURL
                ("service:jmx:rmi:///jndi/rmi://192.168.3.125:9099/jmxrmi");
        JMXConnector jmxc = JMXConnectorFactory.connect(url,null);

        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        //ObjectName的名称与前面注册时候的保持一致
        ObjectName sizeMbeanName = new ObjectName("kafka.log:type=Log,name=Size,topic=logcenter_logagent_service,partition=0");
        System.out.println("mbeanName="+sizeMbeanName);
        System.out.println("mbsc.getMBeanInfo="+mbsc.getMBeanInfo(sizeMbeanName));
        Hashtable<String,String> hashTable = sizeMbeanName.getKeyPropertyList();
        for(String key : hashTable.keySet()) {
            System.out.println(key+":"+hashTable.get(key));
            // Object result = mbsc.invoke(mbeanName,"objectName",null,null);
        }

        Object result = null;
        try {
            result = mbsc.getAttribute(sizeMbeanName,"Value");
            System.out.println("result="+result);
        } catch (MBeanException e) {
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }

        beanFind(mbsc);
    }


    static  void beanFind(MBeanServerConnection mbsc ) throws MalformedObjectNameException, IOException {
        ObjectName sizeMbeanName = new ObjectName("kafka.log:type=Log,name=Size,topic=logcenter_logagent_service");
        QueryExp exp1 = Query.eq(Query.attr( "partition" ),Query.value( "0" ) );
        Set<ObjectInstance> sets = mbsc.queryMBeans(sizeMbeanName,exp1);
        System.out.println("set size = "+sets.size());
    }

}
