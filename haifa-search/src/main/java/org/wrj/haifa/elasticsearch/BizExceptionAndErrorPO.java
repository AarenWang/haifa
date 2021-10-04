package org.wrj.haifa.elasticsearch;

public class BizExceptionAndErrorPO {

    private String serviceName;

    private Long timestamp;

    private Integer count;

    // 1:bizException,2:SystemError
    private int exceptionOrError;

    private String bizExceptionCode;

    private String bizExceptionClass;

    private String errorCode;

    private String errorClass;

    private String  message;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public int getExceptionOrError() {
        return exceptionOrError;
    }

    public void setExceptionOrError(int exceptionOrError) {
        this.exceptionOrError = exceptionOrError;
    }

    public String getBizExceptionCode() {
        return bizExceptionCode;
    }

    public void setBizExceptionCode(String bizExceptionCode) {
        this.bizExceptionCode = bizExceptionCode;
    }

    public String getBizExceptionClass() {
        return bizExceptionClass;
    }

    public void setBizExceptionClass(String bizExceptionClass) {
        this.bizExceptionClass = bizExceptionClass;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorClass() {
        return errorClass;
    }

    public void setErrorClass(String errorClass) {
        this.errorClass = errorClass;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "BizExceptionAndErrorPO{" +
                "serviceName='" + serviceName + '\'' +
                ", timestamp=" + timestamp +
                ", count=" + count +
                ", exceptionOrError=" + exceptionOrError +
                ", bizExceptionCode='" + bizExceptionCode + '\'' +
                ", bizExceptionClass='" + bizExceptionClass + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorClass='" + errorClass + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
