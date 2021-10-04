package org.wrj.haifa.rabbitmq.spring.mq;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.ConsumerTagStrategy;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Enumeration;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public class ConsumerTagStrategyImpl implements ConsumerTagStrategy {

    private  static Logger log = LoggerFactory.getLogger(ConsumerTagStrategyImpl.class);

    private final String module;

    public ConsumerTagStrategyImpl(String module) {
        this.module = module;
    }

    @Override
    public String createConsumerTag(String queue) {
        return module + "_" + getRandom() + "_" + getLocalHost();
    }

    private String getRandom() {
        return Base64.getEncoder().encodeToString(RandomUtils.nextBytes(8));
    }

    private String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn(e.getMessage(), e);
        }

        return getAllIPAddr();
    }

    private String getAllIPAddr() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface inter = nics.nextElement();
                if (inter.isUp() && !inter.isLoopback()) {
                    Enumeration<InetAddress> addrs = inter.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        ip += addr.getHostAddress() + "|";
                    }
                }
            }
        } catch (SocketException e) {
            log.warn("failed to get ip", e);
        }

        return ip;
    }
}