package org.wrj.haifa.ssh;

import com.jcabi.ssh.Shell;
import com.jcabi.ssh.SshByPassword;

import java.io.IOException;

public class JcabiSSHClient {

    public void test1() throws IOException {
        String ip = System.getProperty("ip");
        String port =  System.getProperty("port");
        String username = System.getProperty("username");
        String password = System.getProperty("password");

        Shell shell = new SshByPassword(ip, Integer.parseInt(port), username, password);


        String stdout = new Shell.Plain(shell).exec("cat /etc/passwd");


        System.out.println(stdout);
    }

    public static void main(String[] args) throws IOException {
        JcabiSSHClient client = new JcabiSSHClient();
        client.test1();
    }
}
