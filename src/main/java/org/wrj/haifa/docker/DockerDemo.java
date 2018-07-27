package org.wrj.haifa.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InfoCmd;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import java.io.IOException;

public class DockerDemo {

    public static void main(String[] args) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock").build();

        DockerClient docker = DockerClientBuilder.getInstance(config).build();
        InfoCmd infoCmd = docker.infoCmd();
        Info info = infoCmd.exec();
        System.out.println(info.toString());
        try {
            docker.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
