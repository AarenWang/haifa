package org.wrj.haifa.ai.deerflow.sandbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DockerPathMapperTest {

    @Test
    void mapsWindowsDrivePathToDockerDesktopBindSource() {
        assertThat(DockerPathMapper.bindSource("D:\\workspace\\haifa\\outputs\\sandbox"))
                .isEqualTo("//d/workspace/haifa/outputs/sandbox");
    }

    @Test
    void leavesUnixPathAsAbsoluteBindSource() {
        assertThat(DockerPathMapper.bindSource("/workspace/haifa/outputs/sandbox"))
                .isEqualTo("/workspace/haifa/outputs/sandbox");
    }
}
