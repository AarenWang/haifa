package org.wrj.haifa.modern.typeinference;

public class RecordExample {
    public static void main(String[] args) {
        var p = new Point(3, 4);
        System.out.println("Point: x=" + p.x() + ", y=" + p.y());
    }
}
