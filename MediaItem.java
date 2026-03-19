package application;

public abstract class MediaItem implements Recommendable {

    // ── Shared fields (inherited by all subclasses) ───────────────────────────
    private final String genre;
    private final String description;

    // ── Constructor ───────────────────────────────────────────────────────────
    protected MediaItem(String genre, String description) {
        this.genre       = genre;
        this.description = description;
    }

    // ── Concrete getters (inherited, no need to override) ─────────────────────
    @Override
    public String getGenre() {
        return genre;
    }

    public String getDescription() {
        return description;
    }

    // ── Abstract method: each subclass formats its own detail text ────────────
    /**
     * Returns the full detail string for the info popup.
     * Subclasses MUST override this to provide type-specific formatting.
     * This is the Polymorphism hook - Book formats it one way,
     * a future Movie class would format it differently.
     */
    @Override
    public abstract String getDetailText();

    // ── toString for debugging ────────────────────────────────────────────────
    @Override
    public String toString() {
        return getClass().getSimpleName() +
               "{title='" + getTitle() + "', genre='" + genre + "'}";
    }
}