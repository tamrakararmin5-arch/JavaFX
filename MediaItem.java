package application;

/**
 * MediaItem.java  -  Abstract Class
 *
 * OOP principles:
 *   Abstraction  - declared abstract; cannot be instantiated directly
 *   Inheritance  - Book extends this and inherits genre + description
 *   Encapsulation- private fields, accessed only through getters/setters
 *   Polymorphism - getDetailText() is abstract; each subclass overrides it
 *
 * Any future media type (Movie, Podcast) can extend this class
 * and get genre/description for free.
 */
public abstract class MediaItem implements Recommendable {

    // Private fields - accessed only via getters and setters (Encapsulation)
    private String genre;
    private String description;

    // Constructor
    protected MediaItem(String genre, String description) {
        this.genre       = genre;
        this.description = description;
    }

    // Getters
    @Override public String getGenre()       { return genre; }
    public       String getDescription()     { return description; }

    // Setters - allow subclasses and admin panel to update values
    public void setGenre(String genre)             { this.genre = genre; }
    public void setDescription(String description) { this.description = description; }

    // Abstract - every subclass MUST implement its own version (Polymorphism)
    @Override
    public abstract String getDetailText();

    // toString - useful for debugging and logging
    @Override
    public String toString() {
        return getClass().getSimpleName() +
               "{title='" + getTitle() + "', genre='" + genre + "'}";
    }
}
