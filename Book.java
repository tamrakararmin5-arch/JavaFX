package application;

/**
 * Book.java  -  Model Class
 *
 * OOP principles demonstrated:
 *   Encapsulation - private fields, public getters AND setters
 *   Inheritance   - extends MediaItem (which implements Recommendable)
 *   Abstraction   - fulfils the Recommendable interface contract
 *   Polymorphism  - overrides getSummary() and getDetailText()
 *                   with Book-specific formatting
 */
public class Book extends MediaItem {

    // Private fields (Encapsulation)
    private String title;
    private String author;
    private int    pages;

    // Constructor - passes shared fields up to MediaItem via super()
    public Book(String title, String author, String genre, int pages, String description) {
        super(genre, description);
        this.title  = title;
        this.author = author;
        this.pages  = pages;
    }

    // Getters (Encapsulation - controlled read access)
    public String getTitle()  { return title; }
    public String getAuthor() { return author; }
    public int    getPages()  { return pages; }

    // Setters (Encapsulation - controlled write access)
    public void setTitle(String title)   { this.title  = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setPages(int pages)      { this.pages  = pages; }

    // Recommendable: getCreator() maps to author for this media type
    @Override public String getCreator() { return author; }

    // Polymorphism: Book-specific one-line summary used in search result cards
    @Override
    public String getSummary() {
        return title + " by " + author + "  (" + pages + " pages)  [" + getGenre() + "]";
    }

    // Polymorphism: Book-specific detail block shown in the info popup
    @Override
    public String getDetailText() {
        return "Author : " + author      + "\n" +
               "Genre  : " + getGenre()  + "\n" +
               "Pages  : " + pages       + "\n\n" +
               getDescription();
    }

    // toString - used for logging and displayed in Admin Panel book list
    @Override
    public String toString() {
        return "Book{title='" + title + "', author='" + author +
               "', genre='" + getGenre() + "', pages=" + pages + "}";
    }
}
