package org.wrj.haifa.modern.record;

public class RecordMethodsExample {
    public static void main(String[] args) {
        var p = new PersonRecord("Bob", 25);
        System.out.println(p.greeting());
        System.out.println("Is adult? " + p.isAdult());
    }

    // Demonstrate methods inside a record
    public record PersonWithMethods(String name, int age) {
        public String greeting() {
            return "Hello, " + name + " (" + age + ")";
        }

        public boolean isAdult() {
            return age >= 18;
        }
    }
}
