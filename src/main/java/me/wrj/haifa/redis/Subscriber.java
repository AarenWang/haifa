package me.wrj.haifa.redis;

import redis.clients.jedis.JedisPubSub;

public class Subscriber extends JedisPubSub {

    public Subscriber(){
    }

    public void onMessage(String channel, String message) {
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));
    }

    public void onSubscribe(String channel, int subscribedChannels) {
        System.out.println(String.format("subscribe redis channel success, channel %s, subscribedChannels %d", channel,
                                         subscribedChannels));
    }

    public void onUnsubscribe(String channel, int subscribedChannels) {
        System.out.println(String.format("unsubscribe redis channel, channel %s, subscribedChannels %d", channel,
                                         subscribedChannels));

    }
}
