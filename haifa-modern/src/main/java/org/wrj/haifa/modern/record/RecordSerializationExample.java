package org.wrj.haifa.modern.record;

import java.io.*;

public class RecordSerializationExample {
    public static void main(String[] args) throws Exception {
        var p = new PersonRecord("Dave", 28);

        byte[] data;
        try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(p);
            oos.flush();
            data = baos.toByteArray();
        }

        try (var bais = new ByteArrayInputStream(data); var ois = new ObjectInputStream(bais)) {
            Object o = ois.readObject();
            System.out.println("Deserialized: " + o);
        }
    }
}
