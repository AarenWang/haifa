package org.wrj.haifa.reenterable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by wangrenjun on 2017/9/22.
 */
public class FlowExecuteRecordHandler implements InvocationHandler {

    private  Object target;

    public  FlowExecuteRecordHandler(Object target){
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ReenterableFlow[] flows = method.getAnnotationsByType(ReenterableFlow.class);

        if(flows != null){
            Annotation[][] parameterAnnotation = method.getParameterAnnotations();
            int i = 0;
            int j = 0;

            String flowId = null;

            for (Annotation[] annotations : parameterAnnotation) {
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().equals(ReenterableFlowId.class)) {
                        flowId = args[i].toString();
                        break;
                    }
                    j++;
                }

                i++;
            }
            if(flowId != null){
                recordFlow(target, method, args, flows[0], flowId);
            }


        }

        ReenterableStep[] steps = method.getAnnotationsByType(ReenterableStep.class);
        if(steps != null ){
            ThreadLocal<String> tl = new ThreadLocal<>();
            String flowId = tl.get();
            recordFlowStep(proxy, method, args,steps[0],flowId);
        }

        return method.invoke(target, args);

    }

    private void recordFlow(Object proxy, Method method, Object[] args, ReenterableFlow flow, String flowId) {
        System.out.printf("class_name %s  method %s flow.name=%s,flowId= \r\n", proxy.getClass().getClass().getName(),
                          method.getName(), args, flow.flowName(), flowId);

        ThreadLocal tl = new ThreadLocal();
        tl.set(flowId);


    }

    private  void recordFlowStep(Object proxy, Method method, Object[] args, ReenterableStep step, String flowId){
        System.out.printf("class_name %s  method %s flow.name=%s,flowId= \r\n", proxy.getClass().getClass().getName(),
                method.getName(), args, step.flowName()+"_"+step.stepName()+"_"+step.order(), flowId);
    }
}
