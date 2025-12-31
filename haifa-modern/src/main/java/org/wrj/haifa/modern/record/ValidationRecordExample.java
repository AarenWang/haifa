package org.wrj.haifa.modern.record;

public class ValidationRecordExample {
    public static void main(String[] args) {
        try {
            var p = new PersonRecord("", -1);
            System.out.println(p);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected validation failure: " + e.getMessage());
        }

        var good = new PersonRecord("Alice", 30);
        System.out.println("Valid person: " + good);
    }

    // A record with a compact constructor for validation
    public record ValidatedPerson(String name, int age) {
        public ValidatedPerson {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
            if (age < 0) throw new IllegalArgumentException("age must be >= 0");
        }
    }
}
