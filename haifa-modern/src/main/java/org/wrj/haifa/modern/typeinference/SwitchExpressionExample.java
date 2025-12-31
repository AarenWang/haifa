package org.wrj.haifa.modern.typeinference;

public class SwitchExpressionExample {
    public static void main(String[] args) {
        var result = switch ("two") {
            case "one" -> 1;
            case "two" -> 2;
            default -> 0;
        };
        System.out.println("switch result = " + result);
    }
}
