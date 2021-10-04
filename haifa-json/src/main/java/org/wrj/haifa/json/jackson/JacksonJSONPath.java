package org.wrj.haifa.json.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;

public class JacksonJSONPath {

    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.USE_LONG_FOR_INTS, true);
        Configuration.defaultConfiguration().jsonProvider(new JacksonJsonProvider(objectMapper));
        Configuration conf = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider(objectMapper))
                .build();

        Object document = conf.jsonProvider().parse(json);
        Long sizeInBytes = JsonPath.read(document, "$._all.primaries.store.size_in_bytes");
        System.out.println(sizeInBytes);
    }


    private static final String json  = "{\n" +
            "    \"_shards\": {\n" +
            "        \"total\": 35,\n" +
            "        \"successful\": 35,\n" +
            "        \"failed\": 0\n" +
            "    },\n" +
            "    \"_all\": {\n" +
            "        \"primaries\": {\n" +
            "            \"docs\": {\n" +
            "                \"count\": 1220608246,\n" +
            "                \"deleted\": 0\n" +
            "            },\n" +
            "            \"store\": {\n" +
            "                \"size_in_bytes\": 2078960508250,\n" +
            "                \"throttle_time_in_millis\": 0\n" +
            "            },\n" +
            "            \"indexing\": {\n" +
            "                \"index_total\": 1220725632,\n" +
            "                \"index_time_in_millis\": 2387692064,\n" +
            "                \"index_current\": 3,\n" +
            "                \"index_failed\": 0,\n" +
            "                \"delete_total\": 0,\n" +
            "                \"delete_time_in_millis\": 0,\n" +
            "                \"delete_current\": 0,\n" +
            "                \"noop_update_total\": 0,\n" +
            "                \"is_throttled\": false,\n" +
            "                \"throttle_time_in_millis\": 0\n" +
            "            },\n" +
            "            \"get\": {\n" +
            "                \"total\": 0,\n" +
            "                \"time_in_millis\": 0,\n" +
            "                \"exists_total\": 0,\n" +
            "                \"exists_time_in_millis\": 0,\n" +
            "                \"missing_total\": 0,\n" +
            "                \"missing_time_in_millis\": 0,\n" +
            "                \"current\": 0\n" +
            "            },\n" +
            "            \"search\": {\n" +
            "                \"open_contexts\": 0,\n" +
            "                \"query_total\": 154490,\n" +
            "                \"query_time_in_millis\": 1969263,\n" +
            "                \"query_current\": 0,\n" +
            "                \"fetch_total\": 282,\n" +
            "                \"fetch_time_in_millis\": 50895,\n" +
            "                \"fetch_current\": 0,\n" +
            "                \"scroll_total\": 0,\n" +
            "                \"scroll_time_in_millis\": 0,\n" +
            "                \"scroll_current\": 0,\n" +
            "                \"suggest_total\": 0,\n" +
            "                \"suggest_time_in_millis\": 0,\n" +
            "                \"suggest_current\": 0\n" +
            "            },\n" +
            "            \"merges\": {\n" +
            "                \"current\": 8,\n" +
            "                \"current_docs\": 12629831,\n" +
            "                \"current_size_in_bytes\": 22023622248,\n" +
            "                \"total\": 68445,\n" +
            "                \"total_time_in_millis\": 2022022746,\n" +
            "                \"total_docs\": 3717586390,\n" +
            "                \"total_size_in_bytes\": 6618612338031,\n" +
            "                \"total_stopped_time_in_millis\": 66848,\n" +
            "                \"total_throttled_time_in_millis\": 1589220655,\n" +
            "                \"total_auto_throttle_in_bytes\": 185026001\n" +
            "            },\n" +
            "            \"refresh\": {\n" +
            "                \"total\": 34177,\n" +
            "                \"total_time_in_millis\": 17449459,\n" +
            "                \"listeners\": 0\n" +
            "            },\n" +
            "            \"flush\": {\n" +
            "                \"total\": 6299,\n" +
            "                \"total_time_in_millis\": 34472911\n" +
            "            },\n" +
            "            \"warmer\": {\n" +
            "                \"current\": 0,\n" +
            "                \"total\": 34101,\n" +
            "                \"total_time_in_millis\": 1178\n" +
            "            },\n" +
            "            \"query_cache\": {\n" +
            "                \"memory_size_in_bytes\": 368782291,\n" +
            "                \"total_count\": 2048403,\n" +
            "                \"hit_count\": 443719,\n" +
            "                \"miss_count\": 1604684,\n" +
            "                \"cache_size\": 10304,\n" +
            "                \"cache_count\": 18156,\n" +
            "                \"evictions\": 7852\n" +
            "            },\n" +
            "            \"fielddata\": {\n" +
            "                \"memory_size_in_bytes\": 0,\n" +
            "                \"evictions\": 0\n" +
            "            },\n" +
            "            \"completion\": {\n" +
            "                \"size_in_bytes\": 0\n" +
            "            },\n" +
            "            \"segments\": {\n" +
            "                \"count\": 1513,\n" +
            "                \"memory_in_bytes\": 2717535903,\n" +
            "                \"terms_memory_in_bytes\": 2057161990,\n" +
            "                \"stored_fields_memory_in_bytes\": 627178984,\n" +
            "                \"term_vectors_memory_in_bytes\": 0,\n" +
            "                \"norms_memory_in_bytes\": 0,\n" +
            "                \"points_memory_in_bytes\": 27021749,\n" +
            "                \"doc_values_memory_in_bytes\": 6173180,\n" +
            "                \"index_writer_memory_in_bytes\": 520906640,\n" +
            "                \"version_map_memory_in_bytes\": 19135059,\n" +
            "                \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                \"file_sizes\": {}\n" +
            "            },\n" +
            "            \"translog\": {\n" +
            "                \"operations\": 931749,\n" +
            "                \"size_in_bytes\": 2611237883\n" +
            "            },\n" +
            "            \"request_cache\": {\n" +
            "                \"memory_size_in_bytes\": 178129,\n" +
            "                \"evictions\": 0,\n" +
            "                \"hit_count\": 108214,\n" +
            "                \"miss_count\": 47949\n" +
            "            },\n" +
            "            \"recovery\": {\n" +
            "                \"current_as_source\": 0,\n" +
            "                \"current_as_target\": 0,\n" +
            "                \"throttle_time_in_millis\": 0\n" +
            "            }\n" +
            "        },\n" +
            "        \"total\": {\n" +
            "            \"docs\": {\n" +
            "                \"count\": 1220608246,\n" +
            "                \"deleted\": 0\n" +
            "            },\n" +
            "            \"store\": {\n" +
            "                \"size_in_bytes\": 2078960508250,\n" +
            "                \"throttle_time_in_millis\": 0\n" +
            "            },\n" +
            "            \"indexing\": {\n" +
            "                \"index_total\": 1220725632,\n" +
            "                \"index_time_in_millis\": 2387692064,\n" +
            "                \"index_current\": 3,\n" +
            "                \"index_failed\": 0,\n" +
            "                \"delete_total\": 0,\n" +
            "                \"delete_time_in_millis\": 0,\n" +
            "                \"delete_current\": 0,\n" +
            "                \"noop_update_total\": 0,\n" +
            "                \"is_throttled\": false,\n" +
            "                \"throttle_time_in_millis\": 0\n" +
            "            },\n" +
            "            \"get\": {\n" +
            "                \"total\": 0,\n" +
            "                \"time_in_millis\": 0,\n" +
            "                \"exists_total\": 0,\n" +
            "                \"exists_time_in_millis\": 0,\n" +
            "                \"missing_total\": 0,\n" +
            "                \"missing_time_in_millis\": 0,\n" +
            "                \"current\": 0\n" +
            "            },\n" +
            "            \"search\": {\n" +
            "                \"open_contexts\": 0,\n" +
            "                \"query_total\": 154490,\n" +
            "                \"query_time_in_millis\": 1969263,\n" +
            "                \"query_current\": 0,\n" +
            "                \"fetch_total\": 282,\n" +
            "                \"fetch_time_in_millis\": 50895,\n" +
            "                \"fetch_current\": 0,\n" +
            "                \"scroll_total\": 0,\n" +
            "                \"scroll_time_in_millis\": 0,\n" +
            "                \"scroll_current\": 0,\n" +
            "                \"suggest_total\": 0,\n" +
            "                \"suggest_time_in_millis\": 0,\n" +
            "                \"suggest_current\": 0\n" +
            "            },\n" +
            "            \"merges\": {\n" +
            "                \"current\": 8,\n" +
            "                \"current_docs\": 12629831,\n" +
            "                \"current_size_in_bytes\": 22023622248,\n" +
            "                \"total\": 68445,\n" +
            "                \"total_time_in_millis\": 2022022746,\n" +
            "                \"total_docs\": 3717586390,\n" +
            "                \"total_size_in_bytes\": 6618612338031,\n" +
            "                \"total_stopped_time_in_millis\": 66848,\n" +
            "                \"total_throttled_time_in_millis\": 1589220655,\n" +
            "                \"total_auto_throttle_in_bytes\": 185026001\n" +
            "            },\n" +
            "            \"refresh\": {\n" +
            "                \"total\": 34177,\n" +
            "                \"total_time_in_millis\": 17449459,\n" +
            "                \"listeners\": 0\n" +
            "            },\n" +
            "            \"flush\": {\n" +
            "                \"total\": 6299,\n" +
            "                \"total_time_in_millis\": 34472911\n" +
            "            },\n" +
            "            \"warmer\": {\n" +
            "                \"current\": 0,\n" +
            "                \"total\": 34101,\n" +
            "                \"total_time_in_millis\": 1178\n" +
            "            },\n" +
            "            \"query_cache\": {\n" +
            "                \"memory_size_in_bytes\": 368782291,\n" +
            "                \"total_count\": 2048403,\n" +
            "                \"hit_count\": 443719,\n" +
            "                \"miss_count\": 1604684,\n" +
            "                \"cache_size\": 10304,\n" +
            "                \"cache_count\": 18156,\n" +
            "                \"evictions\": 7852\n" +
            "            },\n" +
            "            \"fielddata\": {\n" +
            "                \"memory_size_in_bytes\": 0,\n" +
            "                \"evictions\": 0\n" +
            "            },\n" +
            "            \"completion\": {\n" +
            "                \"size_in_bytes\": 0\n" +
            "            },\n" +
            "            \"segments\": {\n" +
            "                \"count\": 1513,\n" +
            "                \"memory_in_bytes\": 2717535903,\n" +
            "                \"terms_memory_in_bytes\": 2057161990,\n" +
            "                \"stored_fields_memory_in_bytes\": 627178984,\n" +
            "                \"term_vectors_memory_in_bytes\": 0,\n" +
            "                \"norms_memory_in_bytes\": 0,\n" +
            "                \"points_memory_in_bytes\": 27021749,\n" +
            "                \"doc_values_memory_in_bytes\": 6173180,\n" +
            "                \"index_writer_memory_in_bytes\": 520906640,\n" +
            "                \"version_map_memory_in_bytes\": 19135059,\n" +
            "                \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                \"file_sizes\": {}\n" +
            "            },\n" +
            "            \"translog\": {\n" +
            "                \"operations\": 931749,\n" +
            "                \"size_in_bytes\": 2611237883\n" +
            "            },\n" +
            "            \"request_cache\": {\n" +
            "                \"memory_size_in_bytes\": 178129,\n" +
            "                \"evictions\": 0,\n" +
            "                \"hit_count\": 108214,\n" +
            "                \"miss_count\": 47949\n" +
            "            },\n" +
            "            \"recovery\": {\n" +
            "                \"current_as_source\": 0,\n" +
            "                \"current_as_target\": 0,\n" +
            "                \"throttle_time_in_millis\": 0\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    \"indices\": {\n" +
            "        \"sc_nginx_gateway_service-2020-12-26\": {\n" +
            "            \"primaries\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 270844076,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 453568016811,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 270844076,\n" +
            "                    \"index_time_in_millis\": 566413995,\n" +
            "                    \"index_current\": 0,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 54943,\n" +
            "                    \"query_time_in_millis\": 208373,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 0,\n" +
            "                    \"fetch_time_in_millis\": 0,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"current_docs\": 0,\n" +
            "                    \"current_size_in_bytes\": 0,\n" +
            "                    \"total\": 16367,\n" +
            "                    \"total_time_in_millis\": 451165873,\n" +
            "                    \"total_docs\": 841043433,\n" +
            "                    \"total_size_in_bytes\": 1486230733583,\n" +
            "                    \"total_stopped_time_in_millis\": 0,\n" +
            "                    \"total_throttled_time_in_millis\": 349907036,\n" +
            "                    \"total_auto_throttle_in_bytes\": 36700160\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 8126,\n" +
            "                    \"total_time_in_millis\": 4198995,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 1396,\n" +
            "                    \"total_time_in_millis\": 8003173\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 8109,\n" +
            "                    \"total_time_in_millis\": 286\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 48638790,\n" +
            "                    \"total_count\": 415918,\n" +
            "                    \"hit_count\": 103257,\n" +
            "                    \"miss_count\": 312661,\n" +
            "                    \"cache_size\": 1948,\n" +
            "                    \"cache_count\": 3795,\n" +
            "                    \"evictions\": 1847\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 260,\n" +
            "                    \"memory_in_bytes\": 589681984,\n" +
            "                    \"terms_memory_in_bytes\": 443398184,\n" +
            "                    \"stored_fields_memory_in_bytes\": 139326688,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 5969312,\n" +
            "                    \"doc_values_memory_in_bytes\": 987800,\n" +
            "                    \"index_writer_memory_in_bytes\": 0,\n" +
            "                    \"version_map_memory_in_bytes\": 0,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 0,\n" +
            "                    \"size_in_bytes\": 301\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 10241,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 44254,\n" +
            "                    \"miss_count\": 11452\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            },\n" +
            "            \"total\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 270844076,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 453568016811,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 270844076,\n" +
            "                    \"index_time_in_millis\": 566413995,\n" +
            "                    \"index_current\": 0,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 54943,\n" +
            "                    \"query_time_in_millis\": 208373,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 0,\n" +
            "                    \"fetch_time_in_millis\": 0,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"current_docs\": 0,\n" +
            "                    \"current_size_in_bytes\": 0,\n" +
            "                    \"total\": 16367,\n" +
            "                    \"total_time_in_millis\": 451165873,\n" +
            "                    \"total_docs\": 841043433,\n" +
            "                    \"total_size_in_bytes\": 1486230733583,\n" +
            "                    \"total_stopped_time_in_millis\": 0,\n" +
            "                    \"total_throttled_time_in_millis\": 349907036,\n" +
            "                    \"total_auto_throttle_in_bytes\": 36700160\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 8126,\n" +
            "                    \"total_time_in_millis\": 4198995,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 1396,\n" +
            "                    \"total_time_in_millis\": 8003173\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 8109,\n" +
            "                    \"total_time_in_millis\": 286\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 48638790,\n" +
            "                    \"total_count\": 415918,\n" +
            "                    \"hit_count\": 103257,\n" +
            "                    \"miss_count\": 312661,\n" +
            "                    \"cache_size\": 1948,\n" +
            "                    \"cache_count\": 3795,\n" +
            "                    \"evictions\": 1847\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 260,\n" +
            "                    \"memory_in_bytes\": 589681984,\n" +
            "                    \"terms_memory_in_bytes\": 443398184,\n" +
            "                    \"stored_fields_memory_in_bytes\": 139326688,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 5969312,\n" +
            "                    \"doc_values_memory_in_bytes\": 987800,\n" +
            "                    \"index_writer_memory_in_bytes\": 0,\n" +
            "                    \"version_map_memory_in_bytes\": 0,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 0,\n" +
            "                    \"size_in_bytes\": 301\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 10241,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 44254,\n" +
            "                    \"miss_count\": 11452\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"sc_nginx_gateway_service-2020-12-27\": {\n" +
            "            \"primaries\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 273919169,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 456238629419,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 273919169,\n" +
            "                    \"index_time_in_millis\": 500004567,\n" +
            "                    \"index_current\": 0,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 43022,\n" +
            "                    \"query_time_in_millis\": 802620,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 0,\n" +
            "                    \"fetch_time_in_millis\": 0,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"current_docs\": 0,\n" +
            "                    \"current_size_in_bytes\": 0,\n" +
            "                    \"total\": 15875,\n" +
            "                    \"total_time_in_millis\": 443099413,\n" +
            "                    \"total_docs\": 843216914,\n" +
            "                    \"total_size_in_bytes\": 1480825280880,\n" +
            "                    \"total_stopped_time_in_millis\": 0,\n" +
            "                    \"total_throttled_time_in_millis\": 349465542,\n" +
            "                    \"total_auto_throttle_in_bytes\": 38225361\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 8145,\n" +
            "                    \"total_time_in_millis\": 3963050,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 1411,\n" +
            "                    \"total_time_in_millis\": 6971836\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 8127,\n" +
            "                    \"total_time_in_millis\": 274\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 112937123,\n" +
            "                    \"total_count\": 495781,\n" +
            "                    \"hit_count\": 99671,\n" +
            "                    \"miss_count\": 396110,\n" +
            "                    \"cache_size\": 2460,\n" +
            "                    \"cache_count\": 4288,\n" +
            "                    \"evictions\": 1828\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 280,\n" +
            "                    \"memory_in_bytes\": 595659633,\n" +
            "                    \"terms_memory_in_bytes\": 448322428,\n" +
            "                    \"stored_fields_memory_in_bytes\": 140226240,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 6040333,\n" +
            "                    \"doc_values_memory_in_bytes\": 1070632,\n" +
            "                    \"index_writer_memory_in_bytes\": 0,\n" +
            "                    \"version_map_memory_in_bytes\": 0,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 0,\n" +
            "                    \"size_in_bytes\": 301\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 95221,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 32135,\n" +
            "                    \"miss_count\": 11510\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            },\n" +
            "            \"total\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 273919169,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 456238629419,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 273919169,\n" +
            "                    \"index_time_in_millis\": 500004567,\n" +
            "                    \"index_current\": 0,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 43022,\n" +
            "                    \"query_time_in_millis\": 802620,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 0,\n" +
            "                    \"fetch_time_in_millis\": 0,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"current_docs\": 0,\n" +
            "                    \"current_size_in_bytes\": 0,\n" +
            "                    \"total\": 15875,\n" +
            "                    \"total_time_in_millis\": 443099413,\n" +
            "                    \"total_docs\": 843216914,\n" +
            "                    \"total_size_in_bytes\": 1480825280880,\n" +
            "                    \"total_stopped_time_in_millis\": 0,\n" +
            "                    \"total_throttled_time_in_millis\": 349465542,\n" +
            "                    \"total_auto_throttle_in_bytes\": 38225361\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 8145,\n" +
            "                    \"total_time_in_millis\": 3963050,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 1411,\n" +
            "                    \"total_time_in_millis\": 6971836\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 8127,\n" +
            "                    \"total_time_in_millis\": 274\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 112937123,\n" +
            "                    \"total_count\": 495781,\n" +
            "                    \"hit_count\": 99671,\n" +
            "                    \"miss_count\": 396110,\n" +
            "                    \"cache_size\": 2460,\n" +
            "                    \"cache_count\": 4288,\n" +
            "                    \"evictions\": 1828\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 280,\n" +
            "                    \"memory_in_bytes\": 595659633,\n" +
            "                    \"terms_memory_in_bytes\": 448322428,\n" +
            "                    \"stored_fields_memory_in_bytes\": 140226240,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 6040333,\n" +
            "                    \"doc_values_memory_in_bytes\": 1070632,\n" +
            "                    \"index_writer_memory_in_bytes\": 0,\n" +
            "                    \"version_map_memory_in_bytes\": 0,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 0,\n" +
            "                    \"size_in_bytes\": 301\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 95221,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 32135,\n" +
            "                    \"miss_count\": 11510\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"sc_nginx_gateway_service-2020-12-30\": {\n" +
            "            \"primaries\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 49145502,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 110612886050,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 49262888,\n" +
            "                    \"index_time_in_millis\": 92117770,\n" +
            "                    \"index_current\": 3,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 6363,\n" +
            "                    \"query_time_in_millis\": 47646,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 13,\n" +
            "                    \"fetch_time_in_millis\": 5149,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 8,\n" +
            "                    \"current_docs\": 12629831,\n" +
            "                    \"current_size_in_bytes\": 22023622248,\n" +
            "                    \"total\": 2416,\n" +
            "                    \"total_time_in_millis\": 61682573,\n" +
            "                    \"total_docs\": 126641301,\n" +
            "                    \"total_size_in_bytes\": 229884712226,\n" +
            "                    \"total_stopped_time_in_millis\": 0,\n" +
            "                    \"total_throttled_time_in_millis\": 47283810,\n" +
            "                    \"total_auto_throttle_in_bytes\": 36700160\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 1218,\n" +
            "                    \"total_time_in_millis\": 613593,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 252,\n" +
            "                    \"total_time_in_millis\": 1314931\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 1211,\n" +
            "                    \"total_time_in_millis\": 39\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 7671980,\n" +
            "                    \"total_count\": 52835,\n" +
            "                    \"hit_count\": 9154,\n" +
            "                    \"miss_count\": 43681,\n" +
            "                    \"cache_size\": 583,\n" +
            "                    \"cache_count\": 1348,\n" +
            "                    \"evictions\": 765\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 390,\n" +
            "                    \"memory_in_bytes\": 125469632,\n" +
            "                    \"terms_memory_in_bytes\": 96954900,\n" +
            "                    \"stored_fields_memory_in_bytes\": 25705800,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 1101524,\n" +
            "                    \"doc_values_memory_in_bytes\": 1707408,\n" +
            "                    \"index_writer_memory_in_bytes\": 520906640,\n" +
            "                    \"version_map_memory_in_bytes\": 19135059,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 931749,\n" +
            "                    \"size_in_bytes\": 2611236679\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 4840,\n" +
            "                    \"miss_count\": 1523\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            },\n" +
            "            \"total\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 49145502,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 110612886050,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 49262888,\n" +
            "                    \"index_time_in_millis\": 92117770,\n" +
            "                    \"index_current\": 3,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 6363,\n" +
            "                    \"query_time_in_millis\": 47646,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 13,\n" +
            "                    \"fetch_time_in_millis\": 5149,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 8,\n" +
            "                    \"current_docs\": 12629831,\n" +
            "                    \"current_size_in_bytes\": 22023622248,\n" +
            "                    \"total\": 2416,\n" +
            "                    \"total_time_in_millis\": 61682573,\n" +
            "                    \"total_docs\": 126641301,\n" +
            "                    \"total_size_in_bytes\": 229884712226,\n" +
            "                    \"total_stopped_time_in_millis\": 0,\n" +
            "                    \"total_throttled_time_in_millis\": 47283810,\n" +
            "                    \"total_auto_throttle_in_bytes\": 36700160\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 1218,\n" +
            "                    \"total_time_in_millis\": 613593,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 252,\n" +
            "                    \"total_time_in_millis\": 1314931\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 1211,\n" +
            "                    \"total_time_in_millis\": 39\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 7671980,\n" +
            "                    \"total_count\": 52835,\n" +
            "                    \"hit_count\": 9154,\n" +
            "                    \"miss_count\": 43681,\n" +
            "                    \"cache_size\": 583,\n" +
            "                    \"cache_count\": 1348,\n" +
            "                    \"evictions\": 765\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 390,\n" +
            "                    \"memory_in_bytes\": 125469632,\n" +
            "                    \"terms_memory_in_bytes\": 96954900,\n" +
            "                    \"stored_fields_memory_in_bytes\": 25705800,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 1101524,\n" +
            "                    \"doc_values_memory_in_bytes\": 1707408,\n" +
            "                    \"index_writer_memory_in_bytes\": 520906640,\n" +
            "                    \"version_map_memory_in_bytes\": 19135059,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 931749,\n" +
            "                    \"size_in_bytes\": 2611236679\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 4840,\n" +
            "                    \"miss_count\": 1523\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"sc_nginx_gateway_service-2020-12-28\": {\n" +
            "            \"primaries\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 322762034,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 547733177585,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 322762034,\n" +
            "                    \"index_time_in_millis\": 676928014,\n" +
            "                    \"index_current\": 0,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 31178,\n" +
            "                    \"query_time_in_millis\": 598627,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 100,\n" +
            "                    \"fetch_time_in_millis\": 22282,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"current_docs\": 0,\n" +
            "                    \"current_size_in_bytes\": 0,\n" +
            "                    \"total\": 17586,\n" +
            "                    \"total_time_in_millis\": 559744825,\n" +
            "                    \"total_docs\": 980424221,\n" +
            "                    \"total_size_in_bytes\": 1770426804900,\n" +
            "                    \"total_stopped_time_in_millis\": 45970,\n" +
            "                    \"total_throttled_time_in_millis\": 436002713,\n" +
            "                    \"total_auto_throttle_in_bytes\": 36700160\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 8392,\n" +
            "                    \"total_time_in_millis\": 4507680,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 1669,\n" +
            "                    \"total_time_in_millis\": 10829984\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 8375,\n" +
            "                    \"total_time_in_millis\": 288\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 104587597,\n" +
            "                    \"total_count\": 569487,\n" +
            "                    \"hit_count\": 115371,\n" +
            "                    \"miss_count\": 454116,\n" +
            "                    \"cache_size\": 2744,\n" +
            "                    \"cache_count\": 4346,\n" +
            "                    \"evictions\": 1602\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 302,\n" +
            "                    \"memory_in_bytes\": 733937809,\n" +
            "                    \"terms_memory_in_bytes\": 559619495,\n" +
            "                    \"stored_fields_memory_in_bytes\": 165933192,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 7199114,\n" +
            "                    \"doc_values_memory_in_bytes\": 1186008,\n" +
            "                    \"index_writer_memory_in_bytes\": 0,\n" +
            "                    \"version_map_memory_in_bytes\": 0,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 0,\n" +
            "                    \"size_in_bytes\": 301\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 54061,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 19885,\n" +
            "                    \"miss_count\": 11636\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            },\n" +
            "            \"total\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 322762034,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 547733177585,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 322762034,\n" +
            "                    \"index_time_in_millis\": 676928014,\n" +
            "                    \"index_current\": 0,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 31178,\n" +
            "                    \"query_time_in_millis\": 598627,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 100,\n" +
            "                    \"fetch_time_in_millis\": 22282,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"current_docs\": 0,\n" +
            "                    \"current_size_in_bytes\": 0,\n" +
            "                    \"total\": 17586,\n" +
            "                    \"total_time_in_millis\": 559744825,\n" +
            "                    \"total_docs\": 980424221,\n" +
            "                    \"total_size_in_bytes\": 1770426804900,\n" +
            "                    \"total_stopped_time_in_millis\": 45970,\n" +
            "                    \"total_throttled_time_in_millis\": 436002713,\n" +
            "                    \"total_auto_throttle_in_bytes\": 36700160\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 8392,\n" +
            "                    \"total_time_in_millis\": 4507680,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 1669,\n" +
            "                    \"total_time_in_millis\": 10829984\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 8375,\n" +
            "                    \"total_time_in_millis\": 288\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 104587597,\n" +
            "                    \"total_count\": 569487,\n" +
            "                    \"hit_count\": 115371,\n" +
            "                    \"miss_count\": 454116,\n" +
            "                    \"cache_size\": 2744,\n" +
            "                    \"cache_count\": 4346,\n" +
            "                    \"evictions\": 1602\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 302,\n" +
            "                    \"memory_in_bytes\": 733937809,\n" +
            "                    \"terms_memory_in_bytes\": 559619495,\n" +
            "                    \"stored_fields_memory_in_bytes\": 165933192,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 7199114,\n" +
            "                    \"doc_values_memory_in_bytes\": 1186008,\n" +
            "                    \"index_writer_memory_in_bytes\": 0,\n" +
            "                    \"version_map_memory_in_bytes\": 0,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 0,\n" +
            "                    \"size_in_bytes\": 301\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 54061,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 19885,\n" +
            "                    \"miss_count\": 11636\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"sc_nginx_gateway_service-2020-12-29\": {\n" +
            "            \"primaries\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 303937465,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 510807798385,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 303937465,\n" +
            "                    \"index_time_in_millis\": 552227718,\n" +
            "                    \"index_current\": 0,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 18984,\n" +
            "                    \"query_time_in_millis\": 311997,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 169,\n" +
            "                    \"fetch_time_in_millis\": 23464,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"current_docs\": 0,\n" +
            "                    \"current_size_in_bytes\": 0,\n" +
            "                    \"total\": 16201,\n" +
            "                    \"total_time_in_millis\": 506330062,\n" +
            "                    \"total_docs\": 926260521,\n" +
            "                    \"total_size_in_bytes\": 1651244806442,\n" +
            "                    \"total_stopped_time_in_millis\": 20878,\n" +
            "                    \"total_throttled_time_in_millis\": 406561554,\n" +
            "                    \"total_auto_throttle_in_bytes\": 36700160\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 8296,\n" +
            "                    \"total_time_in_millis\": 4166141,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 1571,\n" +
            "                    \"total_time_in_millis\": 7352987\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 8279,\n" +
            "                    \"total_time_in_millis\": 291\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 94946801,\n" +
            "                    \"total_count\": 514382,\n" +
            "                    \"hit_count\": 116266,\n" +
            "                    \"miss_count\": 398116,\n" +
            "                    \"cache_size\": 2569,\n" +
            "                    \"cache_count\": 4379,\n" +
            "                    \"evictions\": 1810\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 281,\n" +
            "                    \"memory_in_bytes\": 672786845,\n" +
            "                    \"terms_memory_in_bytes\": 508866983,\n" +
            "                    \"stored_fields_memory_in_bytes\": 155987064,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 6711466,\n" +
            "                    \"doc_values_memory_in_bytes\": 1221332,\n" +
            "                    \"index_writer_memory_in_bytes\": 0,\n" +
            "                    \"version_map_memory_in_bytes\": 0,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 0,\n" +
            "                    \"size_in_bytes\": 301\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 18606,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 7100,\n" +
            "                    \"miss_count\": 11828\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            },\n" +
            "            \"total\": {\n" +
            "                \"docs\": {\n" +
            "                    \"count\": 303937465,\n" +
            "                    \"deleted\": 0\n" +
            "                },\n" +
            "                \"store\": {\n" +
            "                    \"size_in_bytes\": 510807798385,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"indexing\": {\n" +
            "                    \"index_total\": 303937465,\n" +
            "                    \"index_time_in_millis\": 552227718,\n" +
            "                    \"index_current\": 0,\n" +
            "                    \"index_failed\": 0,\n" +
            "                    \"delete_total\": 0,\n" +
            "                    \"delete_time_in_millis\": 0,\n" +
            "                    \"delete_current\": 0,\n" +
            "                    \"noop_update_total\": 0,\n" +
            "                    \"is_throttled\": false,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                },\n" +
            "                \"get\": {\n" +
            "                    \"total\": 0,\n" +
            "                    \"time_in_millis\": 0,\n" +
            "                    \"exists_total\": 0,\n" +
            "                    \"exists_time_in_millis\": 0,\n" +
            "                    \"missing_total\": 0,\n" +
            "                    \"missing_time_in_millis\": 0,\n" +
            "                    \"current\": 0\n" +
            "                },\n" +
            "                \"search\": {\n" +
            "                    \"open_contexts\": 0,\n" +
            "                    \"query_total\": 18984,\n" +
            "                    \"query_time_in_millis\": 311997,\n" +
            "                    \"query_current\": 0,\n" +
            "                    \"fetch_total\": 169,\n" +
            "                    \"fetch_time_in_millis\": 23464,\n" +
            "                    \"fetch_current\": 0,\n" +
            "                    \"scroll_total\": 0,\n" +
            "                    \"scroll_time_in_millis\": 0,\n" +
            "                    \"scroll_current\": 0,\n" +
            "                    \"suggest_total\": 0,\n" +
            "                    \"suggest_time_in_millis\": 0,\n" +
            "                    \"suggest_current\": 0\n" +
            "                },\n" +
            "                \"merges\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"current_docs\": 0,\n" +
            "                    \"current_size_in_bytes\": 0,\n" +
            "                    \"total\": 16201,\n" +
            "                    \"total_time_in_millis\": 506330062,\n" +
            "                    \"total_docs\": 926260521,\n" +
            "                    \"total_size_in_bytes\": 1651244806442,\n" +
            "                    \"total_stopped_time_in_millis\": 20878,\n" +
            "                    \"total_throttled_time_in_millis\": 406561554,\n" +
            "                    \"total_auto_throttle_in_bytes\": 36700160\n" +
            "                },\n" +
            "                \"refresh\": {\n" +
            "                    \"total\": 8296,\n" +
            "                    \"total_time_in_millis\": 4166141,\n" +
            "                    \"listeners\": 0\n" +
            "                },\n" +
            "                \"flush\": {\n" +
            "                    \"total\": 1571,\n" +
            "                    \"total_time_in_millis\": 7352987\n" +
            "                },\n" +
            "                \"warmer\": {\n" +
            "                    \"current\": 0,\n" +
            "                    \"total\": 8279,\n" +
            "                    \"total_time_in_millis\": 291\n" +
            "                },\n" +
            "                \"query_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 94946801,\n" +
            "                    \"total_count\": 514382,\n" +
            "                    \"hit_count\": 116266,\n" +
            "                    \"miss_count\": 398116,\n" +
            "                    \"cache_size\": 2569,\n" +
            "                    \"cache_count\": 4379,\n" +
            "                    \"evictions\": 1810\n" +
            "                },\n" +
            "                \"fielddata\": {\n" +
            "                    \"memory_size_in_bytes\": 0,\n" +
            "                    \"evictions\": 0\n" +
            "                },\n" +
            "                \"completion\": {\n" +
            "                    \"size_in_bytes\": 0\n" +
            "                },\n" +
            "                \"segments\": {\n" +
            "                    \"count\": 281,\n" +
            "                    \"memory_in_bytes\": 672786845,\n" +
            "                    \"terms_memory_in_bytes\": 508866983,\n" +
            "                    \"stored_fields_memory_in_bytes\": 155987064,\n" +
            "                    \"term_vectors_memory_in_bytes\": 0,\n" +
            "                    \"norms_memory_in_bytes\": 0,\n" +
            "                    \"points_memory_in_bytes\": 6711466,\n" +
            "                    \"doc_values_memory_in_bytes\": 1221332,\n" +
            "                    \"index_writer_memory_in_bytes\": 0,\n" +
            "                    \"version_map_memory_in_bytes\": 0,\n" +
            "                    \"fixed_bit_set_memory_in_bytes\": 0,\n" +
            "                    \"max_unsafe_auto_id_timestamp\": -1,\n" +
            "                    \"file_sizes\": {}\n" +
            "                },\n" +
            "                \"translog\": {\n" +
            "                    \"operations\": 0,\n" +
            "                    \"size_in_bytes\": 301\n" +
            "                },\n" +
            "                \"request_cache\": {\n" +
            "                    \"memory_size_in_bytes\": 18606,\n" +
            "                    \"evictions\": 0,\n" +
            "                    \"hit_count\": 7100,\n" +
            "                    \"miss_count\": 11828\n" +
            "                },\n" +
            "                \"recovery\": {\n" +
            "                    \"current_as_source\": 0,\n" +
            "                    \"current_as_target\": 0,\n" +
            "                    \"throttle_time_in_millis\": 0\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";
}
