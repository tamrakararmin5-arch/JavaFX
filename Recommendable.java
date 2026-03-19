package application;

public interface Recommendable {

    /** Returns the primary title / name of this item. */
    String getTitle();

    /** Returns the creator (author, director, artist, etc.). */
    String getCreator();

    /** Returns the genre or category this item belongs to. */
    String getGenre();

    /**
     * Returns a short one-line summary suitable for list display.
     * Example: "Dune by Frank Herbert  (412 pages)  [Sci-Fi]"
     */
    String getSummary();

    /**
     * Returns the full detail text shown in the info popup.
     * Implementations may format this however they like.
     */
    String getDetailText();
}