package model;

/**
 * Represents the priority level of a task.
 * Used for sorting and visual indicators in the UI.
 */
public enum Priority {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    private final String displayName;
    private final int level;

    Priority(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Gets the color associated with this priority for UI display.
     * @return Hex color code
     */
    public String getColor() {
        return switch (this) {
            case LOW -> "#10b981";      // Green
            case MEDIUM -> "#f59e0b";   // Amber
            case HIGH -> "#f97316";     // Orange
            case URGENT -> "#ef4444";   // Red
        };
    }
}
