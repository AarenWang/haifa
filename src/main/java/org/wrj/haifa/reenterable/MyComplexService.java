package org.wrj.haifa.reenterable;

/**
 * Created by wangrenjun on 2017/9/22.
 */
public interface MyComplexService {

    @ReenterableFlow(flowName = "refund")
    public Object refund(@ReenterableFlowId String purchaseId,Purchase p);
}
