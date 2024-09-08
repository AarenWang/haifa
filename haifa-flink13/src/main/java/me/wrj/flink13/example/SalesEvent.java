package me.wrj.flink13.example;

public class SalesEvent {
    public String productId;
    public double amount;
    public long timestamp;

    public SalesEvent() {}

    public SalesEvent(String productId, double amount, long timestamp) {
        this.productId = productId;
        this.amount = amount;
        this.timestamp = timestamp;
    }


}
