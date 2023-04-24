package me.wrj.concurrent.app.flow.news.publisher;
import me.wrj.concurrent.app.flow.news.data.News;

import java.util.concurrent.Flow.Subscriber;


public class ConsumerData {

    private Subscriber<News> consumer;
    private MySubscription subscription;
    /**
     * @return the consumer
     */
    public Subscriber<News>  getConsumer() {
        return consumer;
    }
    /**
     * @param consumer the consumer to set
     */
    public void setConsumer(Subscriber<News> consumer) {
        this.consumer = consumer;
    }
    /**
     * @return the subscription
     */
    public MySubscription getSubscription() {
        return subscription;
    }
    /**
     * @param subscription the subscription to set
     */
    public void setSubscription(MySubscription subscription) {
        this.subscription = subscription;
    }



}