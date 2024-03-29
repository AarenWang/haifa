package me.wrj.concurrent.app.flow.news.publisher;

import me.wrj.concurrent.app.flow.news.data.News;

public class PublisherTask implements Runnable {

    private ConsumerData consumerData;
    private News news;

    public PublisherTask(ConsumerData consumerData, News news) {
        this.consumerData = consumerData;
        this.news = news;
    }

    @Override
    public void run() {
        MySubscription subscription = consumerData.getSubscription();
        if (!(subscription.isCancelled()) && (subscription.getRequested() > 0)
                && (subscription.hasCategory(news.getCategory()))) {
            consumerData.getConsumer().onNext(news);
            subscription.decreaseRequested();
        }
    }
}
