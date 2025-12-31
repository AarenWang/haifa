package org.wrj.haifa.modern.record;

public class RecordPatternExample {
    public static void main(String[] args) {
        Object o = new PersonRecord("Carol", 40);

        // instanceof 模式匹配（解构记录）
        if (o instanceof PersonRecord p) {
            System.out.println("Matched PersonRecord: name=" + p.name() + ", age=" + p.age());
        } else {
            System.out.println("Not a PersonRecord");
        }

        // 可以通过访问器解构使用记录组件
        if (o instanceof PersonRecord person && person.age() > 18) {
            System.out.println(person.name() + " is adult");
        }
    }
}
