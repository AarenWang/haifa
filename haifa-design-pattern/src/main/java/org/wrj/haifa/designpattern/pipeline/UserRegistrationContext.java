package org.wrj.haifa.designpattern.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 1. 数据上下文
 * 携带数据在管道中流动的对象。
 * 它包含了原始数据、处理后的数据、验证状态和错误信息。
 */
public class UserRegistrationContext {
    private final String rawInput;
    private String username;
    private String password;
    private String email;
    private int age;

    private boolean isValid = true; // 标志数据是否有效
    private final List<String> errors = new ArrayList<>();

    public UserRegistrationContext(String rawInput) {
        this.rawInput = rawInput;
    }

    public void addError(String error) {
        this.isValid = false;
        this.errors.add(error);
    }

    public String getRawInput() {
        return rawInput;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isValid() {
        return isValid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
