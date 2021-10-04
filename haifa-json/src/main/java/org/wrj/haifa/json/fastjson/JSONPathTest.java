package org.wrj.haifa.json.fastjson;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.wrj.haifa.okhttp3.HttpUtil;

import java.util.List;

public class JSONPathTest {

    public static void main(String[] args) {
        if(args.length < 0) {
            System.out.println("必须指定url");
            return;
        }
        String url = args[0];
        String json = HttpUtil.httpGetBodyContext(url);
        JSONObject jsonObject = JSONObject.parseObject(json);

        String path = "filebeat.harvester.files..last_event_published_time";
        boolean contains = JSONPath.contains(jsonObject,path);
        System.out.println("contains?:"+contains);
        if(contains){
            Object evalObject = JSONPath.eval(jsonObject,path);
            System.out.println("evalObject="+evalObject);
            List<String> publishedTime = (List<String>)evalObject;
            String maxTime = publishedTime.stream().max(String::compareTo).get();
            System.out.println("maxTime?="+maxTime);
        }
    }


    private static final String json = "{\n" +
            "    \"beat\": {\n" +
            "        \"info\": {\n" +
            "            \"ephemeral_id\": \"fbd1fd6f-4f6c-4c58-a50e-d22ab1ef20cb\",\n" +
            "            \"uptime\": {\n" +
            "                \"ms\": 953736693\n" +
            "            }\n" +
            "        },\n" +
            "        \"memstats\": {\n" +
            "            \"gc_next\": 28933488,\n" +
            "            \"memory_alloc\": 14723824,\n" +
            "            \"memory_total\": 5510071304\n" +
            "        },\n" +
            "        \"runtime\": {\n" +
            "            \"goroutines\": 56\n" +
            "        }\n" +
            "    },\n" +
            "    \"filebeat\": {\n" +
            "        \"events\": {\n" +
            "            \"active\": 0,\n" +
            "            \"added\": 3346,\n" +
            "            \"done\": 3346\n" +
            "        },\n" +
            "        \"harvester\": {\n" +
            "            \"closed\": 16,\n" +
            "            \"files\": {\n" +
            "                \"12ad9c1a-b783-4ce8-b024-55c56a738883\": {\n" +
            "                    \"last_event_published_time\": \"2020-12-25T23:50:15.761Z\",\n" +
            "                    \"last_event_timestamp\": \"2020-12-25T23:50:10.760Z\",\n" +
            "                    \"name\": \"/data/logs/bhc-doctor-service/bhc-doctor-service-all.log\",\n" +
            "                    \"read_offset\": 426366,\n" +
            "                    \"size\": 426472,\n" +
            "                    \"start_time\": \"2020-12-25T00:00:13.392Z\"\n" +
            "                },\n" +
            "                \"59d7561f-31e5-4ed5-aaeb-3233c09544f0\": {\n" +
            "                    \"last_event_published_time\": \"2020-12-26T16:40:12.802Z\",\n" +
            "                    \"last_event_timestamp\": \"2020-12-26T16:40:07.802Z\",\n" +
            "                    \"name\": \"/data/logs/bhc-doctor-service/bhc-doctor-service-all.log\",\n" +
            "                    \"read_offset\": 300612,\n" +
            "                    \"size\": 300718,\n" +
            "                    \"start_time\": \"2020-12-26T00:00:17.986Z\"\n" +
            "                },\n" +
            "                \"98adb993-acc8-49f4-89c6-9692c172b33e\": {\n" +
            "                    \"last_event_published_time\": \"2020-12-26T09:54:01.928Z\",\n" +
            "                    \"last_event_timestamp\": \"2020-12-26T09:53:56.927Z\",\n" +
            "                    \"name\": \"/data/logs/bhc-doctor-service/bhc-doctor-service-error.log\",\n" +
            "                    \"read_offset\": 3105,\n" +
            "                    \"size\": 3105,\n" +
            "                    \"start_time\": \"2020-12-26T09:52:49.922Z\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"open_files\": 3,\n" +
            "            \"running\": 3,\n" +
            "            \"skipped\": 0,\n" +
            "            \"started\": 19\n" +
            "        },\n" +
            "        \"input\": {\n" +
            "            \"log\": {\n" +
            "                \"files\": {\n" +
            "                    \"renamed\": 0,\n" +
            "                    \"truncated\": 0\n" +
            "                }\n" +
            "            },\n" +
            "            \"netflow\": {\n" +
            "                \"flows\": 0,\n" +
            "                \"packets\": {\n" +
            "                    \"dropped\": 0,\n" +
            "                    \"received\": 0\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    \"libbeat\": {\n" +
            "        \"config\": {\n" +
            "            \"module\": {\n" +
            "                \"running\": 0,\n" +
            "                \"starts\": 0,\n" +
            "                \"stops\": 0\n" +
            "            },\n" +
            "            \"reloads\": 0,\n" +
            "            \"scans\": 0\n" +
            "        },\n" +
            "        \"output\": {\n" +
            "            \"events\": {\n" +
            "                \"acked\": 3294,\n" +
            "                \"active\": 0,\n" +
            "                \"batches\": 2022,\n" +
            "                \"dropped\": 0,\n" +
            "                \"duplicates\": 0,\n" +
            "                \"failed\": 0,\n" +
            "                \"toomany\": 0,\n" +
            "                \"total\": 3294\n" +
            "            },\n" +
            "            \"read\": {\n" +
            "                \"bytes\": 0,\n" +
            "                \"errors\": 0\n" +
            "            },\n" +
            "            \"type\": \"kafka\",\n" +
            "            \"write\": {\n" +
            "                \"bytes\": 0,\n" +
            "                \"errors\": 0\n" +
            "            }\n" +
            "        },\n" +
            "        \"outputs\": {\n" +
            "            \"kafka\": {\n" +
            "                \"bytes_read\": 868018,\n" +
            "                \"bytes_write\": 2829761\n" +
            "            }\n" +
            "        },\n" +
            "        \"pipeline\": {\n" +
            "            \"clients\": 1,\n" +
            "            \"events\": {\n" +
            "                \"active\": 0,\n" +
            "                \"dropped\": 0,\n" +
            "                \"failed\": 0,\n" +
            "                \"filtered\": 52,\n" +
            "                \"published\": 3294,\n" +
            "                \"retry\": 18,\n" +
            "                \"total\": 3346\n" +
            "            },\n" +
            "            \"queue\": {\n" +
            "                \"acked\": 3294\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    \"registrar\": {\n" +
            "        \"states\": {\n" +
            "            \"cleanup\": 16,\n" +
            "            \"current\": 3,\n" +
            "            \"update\": 3346\n" +
            "        },\n" +
            "        \"writes\": {\n" +
            "            \"fail\": 0,\n" +
            "            \"success\": 2068,\n" +
            "            \"total\": 2068\n" +
            "        }\n" +
            "    },\n" +
            "    \"system\": {\n" +
            "        \"cpu\": {\n" +
            "            \"cores\": 8\n" +
            "        },\n" +
            "        \"load\": {\n" +
            "            \"1\": 0,\n" +
            "            \"5\": 0.01,\n" +
            "            \"15\": 0.05,\n" +
            "            \"norm\": {\n" +
            "                \"1\": 0,\n" +
            "                \"5\": 0.0013,\n" +
            "                \"15\": 0.0063\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";
}
