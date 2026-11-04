package com.mysterymansion.model;

/** The five rooms of the mansion, with their theme data. Colours are plain RGB ints so the model stays UI-free. */
public enum Room {
    MAIN_HALL("Main Hall", "🚪 MAIN HALL",
            "The reinforced oak door is secured with a digital keypad and lock.", 0x4B2323, false),
    LIVING_ROOM("Living Room", "🛋️ LIVING ROOM",
            "A cold parlor. Scratches on the floor suggest recent movement.", 0x46371E, false),
    LIBRARY("Library", "📚 LIBRARY",
            "Massive bookshelves reach into darkness. Secrets are hidden here.", 0x1E324B, false),
    LABORATORY("Laboratory", "🔬 LABORATORY",
            "Chemical fumes fill the air. A locked biometric safe sits in the corner.", 0x19462D, true),
    BASEMENT("Basement", "🔐 BASEMENT",
            "Pitch black and freezing cold. Strange breathing sounds echo.", 0x371E41, true);

    private final String displayName;
    private final String title;
    private final String description;
    private final int rgb;
    private final boolean dangerous;

    Room(String displayName, String title, String description, int rgb, boolean dangerous) {
        this.displayName = displayName;
        this.title = title;
        this.description = description;
        this.rgb = rgb;
        this.dangerous = dangerous;
    }

    public String displayName() { return displayName; }
    public String title() { return title; }
    public String description() { return description; }
    public int rgb() { return rgb; }
    /** Dangerous rooms can trigger a monster encounter. */
    public boolean isDangerous() { return dangerous; }

    /** Returns null for unknown names. */
    public static Room fromName(String name) {
        for (Room r : values()) {
            if (r.displayName.equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    @Override
    public String toString() { return displayName; }
}
