package org.wrj.haifa.bid;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class BidTest {

    private ThreadLocalRandom random = ThreadLocalRandom.current();
    @Test
    public  void bid(){
        List<Bid> list = new ArrayList<>();
        for(int i=0;i<10;i++){
            Bid bid = new Bid();
            bid.setBidTime(new BigDecimal(random.nextDouble(60.00)).setScale(2, RoundingMode.HALF_DOWN ));
            bid.setAmount(new BigDecimal(random.nextDouble(10,100)).setScale(2,RoundingMode.HALF_DOWN ));
            list.add(bid);
        }
        print(list);

        System.out.println(getBombTime(list));
    }

    private void print(List<Bid> list) {
        for(Bid bid:list){
            System.out.printf("bidTime:%s,amount:%s \n",bid.getBidTime(),bid.getAmount());
        }
    }

    public BigDecimal getBombTime(List<Bid> list){
        Collections.sort(list,(o1,o2)->o1.getBidTime().compareTo(o2.getBidTime()));
        List<BigDecimal> multiply  = list.stream().map(e -> e.getAmount().multiply(e.getBidTime())).collect(Collectors.toList());
        printMultiply(multiply);
        BigDecimal win = BigDecimal.ZERO;
        BigDecimal lost = BigDecimal.ZERO;
        for(int i = 0; i < multiply.size(); i++){
            BigDecimal tempWin = win.add(multiply.get(i));
            BigDecimal tempLost = lost.add(multiply.get(multiply.size()-1-i));
            if(tempWin.compareTo(tempLost) > 0){
               return list.get(i).getBidTime();
            }
            win = tempWin;
            lost = tempLost;
        }
        return list.get(list.size()/2).getBidTime();
    }

    private void printMultiply(List<BigDecimal> multiply) {
        for(BigDecimal bid:multiply){
            System.out.printf("multiply:%s \n",bid);
        }
    }
}


class Bid{
    private BigDecimal bidTime;

    private BigDecimal amount;

    public BigDecimal getBidTime() {
        return bidTime;
    }

    public void setBidTime(BigDecimal bidTime) {
        this.bidTime = bidTime;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


}