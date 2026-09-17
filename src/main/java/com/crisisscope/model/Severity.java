package com.crisisscope.model;

/**
 * Severity ranking, ordered low to critical. {@code rank} allows sorting/comparison
 * without relying on enum declaration order; {@code colorClass} is a Tailwind
 * utility class consumed directly by the templates.
 */
public enum Severity {
    LOW(1, "Low", "severity-low"),
    MODERATE(2, "Moderate", "severity-moderate"),
    HIGH(3, "High", "severity-high"),
    CRITICAL(4, "Critical", "severity-critical");

    private final int rank;
    private final String label;
    private final String colorClass;

    Severity(int rank, String label, String colorClass) {
        this.rank = rank;
        this.label = label;
        this.colorClass = colorClass;
    }

    public int rank() {
        return rank;
    }

    public String label() {
        return label;
    }

    public String colorClass() {
        return colorClass;
    }
}
