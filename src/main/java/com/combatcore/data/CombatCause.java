/*
 * Decompiled with CFR 0.152.
 */
package com.combatcore.data;

public enum CombatCause {
    MELEE,
    BOW,
    LAVA,
    FIRE,
    TNT,
    CRYSTAL,
    ANCHOR,
    POTION,
    TRIDENT,
    PET,
    INDIRECT,
    UNKNOWN;


    public static CombatCause fromName(String name) {
        try {
            return CombatCause.valueOf(name.toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
