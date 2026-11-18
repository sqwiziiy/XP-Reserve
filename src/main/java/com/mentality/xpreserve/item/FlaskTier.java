package com.mentality.xpreserve.item;

import com.mentality.xpreserve.xp.XpMath;

/** Canonical raw-XP capacities for XP Reserve's initial flask progression. */
public enum FlaskTier {
    I(30),
    II(50),
    III(100);

    private final int equivalentLevel;
    private final long capacity;

    FlaskTier(int equivalentLevel) {
        this.equivalentLevel = equivalentLevel;
        this.capacity = XpMath.totalXpAtLevel(equivalentLevel);
    }

    public int equivalentLevel() {
        return equivalentLevel;
    }

    public long capacity() {
        return capacity;
    }
}
