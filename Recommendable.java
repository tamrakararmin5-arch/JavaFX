package application;

/**
 * Recommendable.java  -  Interface
 *
 * OOP: Abstraction - defines WHAT any recommendable item must provide,
 * without specifying HOW. Any class implementing this is guaranteed to
 * expose a title, creator, genre, summary, and detail text, making it
 * possible to build generic UI components that work for any media type.
 */
public interface Recommendable {

    String getTitle();       // Primary name of this item
    String getCreator();     // Author, director, artist, etc.
    String getGenre();       // Genre or category
    String getSummary();     // One-line display string for lists
    String getDetailText();  // Full detail string for popups
}
