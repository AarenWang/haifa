package org.wrj.haifa.yaml;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;

import java.io.File;
import java.io.IOException;

public class YAMLTest {

    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("please run YAMLTest with yaml file path");
            System.exit(1);
        }
        String path =args[0];
        //File file = new File(path);
        try {
            YamlMapping yamlMapping = Yaml.createYamlInput(new File(path)).readYamlMapping();
            yamlMapping.yamlMapping("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
