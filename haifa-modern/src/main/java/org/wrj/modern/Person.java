package org.wrj.modern;

/**
 * Java 14+ Record 特性示例
 *
 * Records 是不可变数据对象的简洁声明方式
 * 自动生成 equals(), hashCode(), toString() 和 getter 方法
 */
public record Person(String name, int age, String email) {

    /**
     * Compact constructor (Java 14+)
     * 用于验证字段
     */
    public Person {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
    }

    /**
     * 额外的静态工厂方法
     */
    public static Person ofNameAndAge(String name, int age) {
        return new Person(name, age, "");
    }
}
