package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper.java  -  Data Access Layer
 *
 * Contains ALL SQL and JDBC logic. Main.java never writes SQL directly.
 * This enforces Separation of Concerns: UI layer talks to this class,
 * this class talks to MySQL.
 *
 * Every public method is commented to explain its purpose and return value.
 */
public class DatabaseHelper {

    // JDBC connection settings - update DB_PASS if your MySQL password differs
    private static final String DB_URL  =
        "jdbc:mysql://localhost:3306/dbconnect" +
        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "@rmintask2008";

    /** Opens and returns a new JDBC connection with auto-commit enabled. */
    public Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        c.setAutoCommit(true);
        return c;
    }

    /**
     * Verifies the connection on startup and prints diagnostics to the console.
     * Returns true if the database is reachable, false otherwise.
     */
    public boolean verifyConnection() {
        try (Connection c = getConnection()) {
            DatabaseMetaData m = c.getMetaData();
            System.out.println("Connected: " + m.getDatabaseProductName()
                + " " + m.getDatabaseProductVersion()
                + " | Catalog: " + c.getCatalog());
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next()) System.out.println("Users in DB: " + rs.getInt(1));
            return true;
        } catch (SQLException e) {
            System.err.println("CONNECTION FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates all 4 required tables if they do not already exist.
     * Safe to call on every application launch.
     */
    public void initTables() {
        String[] ddl = {
            // users: stores registered account credentials
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  username VARCHAR(50) NOT NULL UNIQUE," +
            "  password VARCHAR(50) NOT NULL)",

            // books: the main catalogue with a UNIQUE title constraint
            "CREATE TABLE IF NOT EXISTS books (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  title VARCHAR(255) UNIQUE," +
            "  author VARCHAR(255)," +
            "  genre VARCHAR(100)," +
            "  pages INT," +
            "  description TEXT)",

            // favourites: links users to their saved books (many-to-many)
            "CREATE TABLE IF NOT EXISTS favourites (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  user_id INT NOT NULL," +
            "  book_title VARCHAR(255) NOT NULL," +
            "  UNIQUE KEY uq_fav (user_id, book_title)," +
            "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)",

            // reviews: one rating+review per user per book
            "CREATE TABLE IF NOT EXISTS reviews (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  user_id INT NOT NULL," +
            "  book_title VARCHAR(255) NOT NULL," +
            "  rating INT NOT NULL," +
            "  review_text TEXT," +
            "  UNIQUE KEY uq_review (user_id, book_title)," +
            "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)"
        };
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            for (String sql : ddl) s.execute(sql);
            System.out.println("All tables verified.");
        } catch (SQLException e) {
            System.err.println("initTables error: " + e.getMessage());
        }
    }

    // =========================================================================
    //  Books
    // =========================================================================

    /**
     * Loads all books from the database ordered by average rating DESC.
     * This drives the Trending section - highest rated books appear first.
     * Uses getSummary() via the Recommendable interface (Polymorphism).
     */
    public List<Book> loadBooks() {
        List<Book> books = new ArrayList<>();
        String sql =
            "SELECT b.title, b.author, b.genre, b.pages, b.description " +
            "FROM books b " +
            "LEFT JOIN reviews r ON r.book_title = b.title " +
            "GROUP BY b.id, b.title, b.author, b.genre, b.pages, b.description " +
            "ORDER BY AVG(COALESCE(r.rating, 0)) DESC, b.title ASC";
        try (Connection c = getConnection();
             ResultSet  rs = c.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                Book b = new Book(
                    rs.getString("title"),  rs.getString("author"),
                    rs.getString("genre"),  rs.getInt("pages"),
                    rs.getString("description")
                );
                // toString() logged to console - demonstrates the method is used
                System.out.println("Loaded: " + b);
                books.add(b);
            }
        } catch (SQLException e) {
            System.err.println("loadBooks error: " + e.getMessage());
        }
        return books;
    }

    /**
     * Inserts a new book into the database.
     * Throws SQLIntegrityConstraintViolationException if the title already exists.
     * Throws SQLException for any other database error.
     */
    public void addBook(String title, String author, String genre,
                        int pages, String desc) throws SQLException {
        String sql = "INSERT INTO books (title, author, genre, pages, description) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);  ps.setString(2, author);
            ps.setString(3, genre);  ps.setInt(4, pages);
            ps.setString(5, desc);
            ps.executeUpdate();
            System.out.println("Book added: " + title);
        }
    }

    // =========================================================================
    //  Authentication
    // =========================================================================

    /**
     * Attempts to log in with the given credentials.
     * Returns the user's database ID on success, or -1 if credentials are wrong.
     */
    public int loginUser(String username, String password) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

    /**
     * Registers a new user and returns their auto-generated database ID.
     * Throws SQLIntegrityConstraintViolationException if username is taken.
     */
    public int registerUser(String username, String password) throws SQLException {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    /**
     * Returns all registered users as formatted strings for the Admin Panel.
     * Format: "ID: 1   Username: alice"
     */
    public List<String> getAllUsers() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id, username FROM users ORDER BY id";
        try (Connection c = getConnection();
             ResultSet  rs = c.createStatement().executeQuery(sql)) {
            while (rs.next())
                list.add("ID: " + rs.getInt("id") +
                         "   Username: " + rs.getString("username"));
        } catch (SQLException e) {
            System.err.println("getAllUsers error: " + e.getMessage());
        }
        return list;
    }

    // =========================================================================
    //  Favourites
    // =========================================================================

    /** Returns a list of book titles the given user has saved as favourites. */
    public List<String> getFavourites(int userId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT book_title FROM favourites WHERE user_id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("book_title"));
        } catch (SQLException e) {
            System.err.println("getFavourites error: " + e.getMessage());
        }
        return list;
    }

    /** Returns true if the given user has saved this book as a favourite. */
    public boolean isFavourite(int userId, String title) {
        String sql = "SELECT id FROM favourites WHERE user_id = ? AND book_title = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, title);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    /** Saves a book to the user's favourites. INSERT IGNORE prevents duplicates. */
    public void addFavourite(int userId, String title) {
        String sql = "INSERT IGNORE INTO favourites (user_id, book_title) VALUES (?, ?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, title);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("addFavourite error: " + e.getMessage()); }
    }

    /** Removes a book from the user's favourites. */
    public void removeFavourite(int userId, String title) {
        String sql = "DELETE FROM favourites WHERE user_id = ? AND book_title = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, title);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("removeFavourite error: " + e.getMessage()); }
    }

    // =========================================================================
    //  Reviews and Ratings
    // =========================================================================

    /**
     * Returns the average star rating for a book.
     * Returns 0.0 if no reviews exist yet.
     */
    public double getAverageRating(String title) {
        String sql = "SELECT AVG(rating) FROM reviews WHERE book_title = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("getAverageRating error: " + e.getMessage()); }
        return 0.0;
    }

    /**
     * Loads the current user's existing review for a book into the output arrays.
     * If no review exists, arrays remain unchanged (0 and "").
     */
    public void getUserReview(int userId, String title, int[] ratingOut, String[] textOut) {
        String sql = "SELECT rating, review_text FROM reviews " +
                     "WHERE user_id = ? AND book_title = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ratingOut[0] = rs.getInt("rating");
                String t = rs.getString("review_text");
                textOut[0]  = t != null ? t : "";
            }
        } catch (SQLException e) { System.err.println("getUserReview error: " + e.getMessage()); }
    }

    /**
     * Saves or updates a review using ON DUPLICATE KEY UPDATE.
     * If the user has already reviewed this book, their review is updated.
     */
    public void saveReview(int userId, String title, int rating, String text) {
        String sql =
            "INSERT INTO reviews (user_id, book_title, rating, review_text) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE rating = VALUES(rating), review_text = VALUES(review_text)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, title);
            ps.setInt(3, rating); ps.setString(4, text);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("saveReview error: " + e.getMessage()); }
    }

    /**
     * Returns all community reviews for a book as formatted display strings.
     * Format: "username  ★★★★☆  \"review text\""
     */
    public List<String> getAllReviews(String title) {
        List<String> list = new ArrayList<>();
        String sql =
            "SELECT u.username, r.rating, r.review_text " +
            "FROM reviews r JOIN users u ON r.user_id = u.id " +
            "WHERE r.book_title = ? ORDER BY r.id DESC";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String t = rs.getString("review_text");
                list.add(rs.getString("username") + "  " + starsString(rs.getInt("rating")) +
                         (t != null && !t.isEmpty() ? "  \"" + t + "\"" : ""));
            }
        } catch (SQLException e) { System.err.println("getAllReviews error: " + e.getMessage()); }
        return list;
    }

    /**
     * Converts a numeric rating to a Unicode star string.
     * Example: 4 → "★★★★☆"
     */
    public String starsString(double rating) {
        int full = (int) Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < full ? "\u2605" : "\u2606");
        return sb.toString();
    }
}
