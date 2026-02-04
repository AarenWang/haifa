package org.wrj.haifa.designpattern.orderpolicyrule.model;

import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 折扣候选：规则产出的最小决策单元。
 */
public final class DiscountCandidate {

    private final String id;
    private final DiscountScope scope;
    private final String mutexGroup;
    private final int priority;
    private final int discountCents;
    private final boolean stackable;
    private final Map<String, Object> meta;
    private final DiscountEntry entry;

    private DiscountCandidate(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.scope = Objects.requireNonNull(builder.scope, "scope");
        this.mutexGroup = builder.mutexGroup;
        this.priority = builder.priority;
        this.discountCents = builder.discountCents;
        this.stackable = builder.stackable;
        this.meta = Collections.unmodifiableMap(new LinkedHashMap<>(builder.meta));
        this.entry = builder.entry;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public DiscountScope getScope() {
        return scope;
    }

    public String getMutexGroup() {
        return mutexGroup;
    }

    public int getPriority() {
        return priority;
    }

    public int getDiscountCents() {
        return discountCents;
    }

    public boolean isStackable() {
        return stackable;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public DiscountEntry getEntry() {
        return entry;
    }

    @Override
    public String toString() {
        return "DiscountCandidate{" +
                "id='" + id + '\'' +
                ", scope=" + scope +
                ", mutexGroup='" + mutexGroup + '\'' +
                ", priority=" + priority +
                ", discountCents=" + discountCents +
                ", stackable=" + stackable +
                ", meta=" + meta +
                '}';
    }

    public static final class Builder {
        private String id;
        private DiscountScope scope;
        private String mutexGroup;
        private int priority = 100;
        private int discountCents;
        private boolean stackable = true;
        private final Map<String, Object> meta = new LinkedHashMap<>();
        private DiscountEntry entry;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder scope(DiscountScope scope) {
            this.scope = scope;
            return this;
        }

        public Builder mutexGroup(String mutexGroup) {
            this.mutexGroup = mutexGroup;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder discountCents(int discountCents) {
            this.discountCents = discountCents;
            return this;
        }

        public Builder stackable(boolean stackable) {
            this.stackable = stackable;
            return this;
        }

        public Builder meta(String key, Object value) {
            if (key != null && value != null) {
                this.meta.put(key, value);
            }
            return this;
        }

        public Builder meta(Map<String, Object> meta) {
            if (meta != null) {
                meta.forEach(this::meta);
            }
            return this;
        }

        public Builder entry(DiscountEntry entry) {
            this.entry = entry;
            return this;
        }

        public DiscountCandidate build() {
            return new DiscountCandidate(this);
        }
    }
}
