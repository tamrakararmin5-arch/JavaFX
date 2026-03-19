package application;

public class Book extends MediaItem {

    // ── Private fields (Encapsulation) ───────────────────────────────────────
    private final String title;
    private final String author;
    private final int    pages;

    // ── Constructor ──────────────────────────────────────────────────────────
    public Book(String title, String author, String genre, int pages, String description) {
        super(genre, description);   // pass shared fields up to MediaItem
        this.title  = title;
        this.author = author;
        this.pages  = pages;
    }

    // ── Getters (Encapsulation - public read-only interface) ──────────────────
    public String getTitle()  { return title; }
    public String getAuthor() { return author; }
    public int    getPages()  { return pages; }

    // ── Recommendable interface: getCreator() maps to author ──────────────────
    @Override
    public String getCreator() {
        return author;
    }

    // ── Polymorphism: Book-specific summary line ──────────────────────────────
    @Override
    public String getSummary() {
        return title + " by " + author + "  (" + pages + " pages)  [" + getGenre() + "]";
    }

    // ── Polymorphism: Book-specific detail text for the popup ─────────────────
    @Override
    public String getDetailText() {
        return "Author : " + author  + "\n" +
               "Genre  : " + getGenre() + "\n" +
               "Pages  : " + pages   + "\n\n" +
               getDescription();
    }

    // ── toString ──────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "Book{title='" + title + "', author='" + author +
               "', genre='" + getGenre() + "', pages=" + pages + "}";
    }
}