package com.mysterymansion.model;

/** Everything that can appear in the mansion or the player's inventory. */
public enum Item {
    TORCH("Torch", true),
    HEALTH_POTION("Health Potion", true),
    CODE_PAPER("Code Paper", true),
    /** Marks the Laboratory safe. It is a fixture, not something you can pick up. */
    SAFE_KEY("Safe Key", false),
    MANSION_KEY("Mansion Key", true);

    private final String displayName;
    private final boolean collectible;

    Item(String displayName, boolean collectible) {
        this.displayName = displayName;
        this.collectible = collectible;
    }

    public String displayName() { return displayName; }
    public boolean isCollectible() { return collectible; }

    /** Returns null for unknown names (e.g. a corrupted save row). */
    public static Item fromName(String name) {
        for (Item i : values()) {
            if (i.displayName.equalsIgnoreCase(name)) return i;
        }
        return null;
    }

    @Override
    public String toString() { return displayName; }
}
