package org.wrj.haifa.rabbitmq.spring.oms;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wrj.haifa.rabbitmq.spring.api.GoodsInfo;
import org.wrj.haifa.rabbitmq.spring.oms.vo.OmsOrderDetailInfo;
import org.wrj.haifa.rabbitmq.spring.oms.vo.OmsOrderInfo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public class OMSApp {

    public static void main(String[] args) {

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-oms.xml");
        OrderService orderService = applicationContext.getBean(OrderService.class);

        int i = 1;
        while (true){

            OmsOrderInfo omsOrderInfo  = generateWmsOrder(i);
            System.out.println("订单系统接收订单"+omsOrderInfo+",即将通知WMS发货");
            i++;
            orderService.notifyWmsDelivery(omsOrderInfo);

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    private static OmsOrderInfo generateWmsOrder(int i) {
        OmsOrderInfo omsOrderInfo = new OmsOrderInfo();
        omsOrderInfo.setOrderId("PO-"+i);
        omsOrderInfo.setOrderPayTime(new Timestamp(System.currentTimeMillis()));
        omsOrderInfo.setPayAmount(new BigDecimal(100));
        omsOrderInfo.setReceiverAddress("杭州钱塘江");
        omsOrderInfo.setReceiverName("张三");
        omsOrderInfo.setPayAmount(new BigDecimal(Math.floor(Math.random()*100)));

        OmsOrderDetailInfo gooodsInfo = new OmsOrderDetailInfo();
        gooodsInfo.setGoodsName("商品"+i);
        gooodsInfo.setGoodsCount(new Double(Math.rint(Math.floor(Math.random()*10))).intValue());
        gooodsInfo.setGoodsPrice(new BigDecimal(Math.floor(Math.random()*100)));
        List<OmsOrderDetailInfo> list = new ArrayList<>();
        list.add(gooodsInfo);
        omsOrderInfo.setOrderDetailInfoList(list);

        return omsOrderInfo;

    }
}
