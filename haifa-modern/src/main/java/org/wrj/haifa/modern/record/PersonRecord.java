package org.wrj.haifa.modern.record;

import java.io.Serializable;

public record PersonRecord(String name, int age) implements Serializable {
}
