package org.wrj.haifa.netty.rsyslog.socketserver;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SyslogServer {

    private SyslogParser parser = new SyslogParser();

    // TCP Server
    public void startTcpServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TCP Syslog Server is listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String syslogData;
                while ((syslogData = in.readLine()) != null) {
                    SyslogMessage message = parser.parse(syslogData);
                    System.out.println("Received TCP Syslog: " + message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // UDP Server
    public void startUdpServer(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP Syslog Server is listening on port " + port);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (true) {
                socket.receive(packet);
                String syslogData = new String(packet.getData(), 0, packet.getLength());
                SyslogMessage message = parser.parse(syslogData);
                System.out.println("Received UDP Syslog: " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SyslogServer server = new SyslogServer();
        // Start both TCP and UDP servers (You can run them in separate threads if needed)
        new Thread(() -> server.startTcpServer(514)).start();  // Syslog TCP on port 514
        new Thread(() -> server.startUdpServer(514)).start();  // Syslog UDP on port 514
    }
}
