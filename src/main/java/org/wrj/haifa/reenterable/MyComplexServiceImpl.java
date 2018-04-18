package org.wrj.haifa.reenterable;

/**
 * Created by wangrenjun on 2017/9/22.
 */
public class MyComplexServiceImpl implements  MyComplexService{



    @Override
    public Object refund(String purchaseId, Purchase p){
        refundStep1(p);
        refundStep2(p);
        refundStep3(p);
        return  p;

    }

    @ReenterableStep(flowName = "refund",order = 1,stepName = "refundStep1")
    private   Object refundStep1(Purchase p){
        System.out.println("refundStep1,purchase="+p);
        return  p;
    }

    @ReenterableStep(flowName = "refund",order = 2,stepName = "refundStep1")
    private  Object refundStep2(Purchase p){
        System.out.println("refundStep1,purchase="+p);
        return  p;
    }


    @ReenterableStep(flowName = "refund",order = 3,stepName = "refundStep1")
    private  Object refundStep3(Purchase p){
        System.out.println("refundStep1,purchase="+p);
        return  p;
    }

}
