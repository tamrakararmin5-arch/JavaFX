package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    // ── CHANGE ONLY THESE IF NEEDED ──────────────────────────────────────────
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "dbconnect";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "@rmintask2008";
    // ─────────────────────────────────────────────────────────────────────────

    private static final String DB_URL =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useSSL=false&allowPublicKeyRetrieval=true"
        + "&serverTimezone=UTC&autoReconnect=true";

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    private Stage  primaryStage;
    private int    currentUserId   = -1;
    private String currentUsername = "";
    private final List<Book> bookCatalogue = new ArrayList<>();

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("BookMatch - Recommendation System");
        
        // Verify connection on startup and print diagnostic info
        verifyConnection();
        initTables();
        loadBooksFromDatabase();
        showLoginScreen();
    }

    // =========================================================================
    //  Connection & diagnostics
    // =========================================================================

    private Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        c.setAutoCommit(true); // Always ensure auto-commit is ON
        return c;
    }

    /**
     * Runs on startup. Prints the exact DB URL, host, and database name to
     * the Eclipse console so you can confirm the app is hitting the right DB.
     */
    private void verifyConnection() {
        System.out.println("=== BookMatch DB Diagnostic ===");
        System.out.println("Connecting to: " + DB_URL);
        try (Connection c = getConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            System.out.println("Connected!  Server: " + meta.getDatabaseProductName()
                + " " + meta.getDatabaseProductVersion());
            System.out.println("Catalog (database): " + c.getCatalog());
            // Count existing users
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next()) System.out.println("Users in DB right now: " + rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("STARTUP CONNECTION FAILED: " + e.getMessage());
            e.printStackTrace();
            showBootAlert("Cannot Connect to Database",
                "Failed to connect to MySQL.\n\n"
                + "URL: " + DB_URL + "\n"
                + "User: " + DB_USER + "\n\n"
                + "Error: " + e.getMessage()
                + "\n\nFix: Check MySQL is running, and DB_PASS in Main.java is correct.");
        }
    }

    private void initTables() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS users ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "username VARCHAR(50) NOT NULL UNIQUE,"
            + "password VARCHAR(50) NOT NULL)",

            "CREATE TABLE IF NOT EXISTS books ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "title VARCHAR(255) UNIQUE,"
            + "author VARCHAR(255),"
            + "genre VARCHAR(100),"
            + "pages INT,"
            + "description TEXT)",

            "CREATE TABLE IF NOT EXISTS favourites ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "user_id INT NOT NULL,"
            + "book_title VARCHAR(255) NOT NULL,"
            + "UNIQUE KEY uq_fav (user_id, book_title),"
            + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS reviews ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "user_id INT NOT NULL,"
            + "book_title VARCHAR(255) NOT NULL,"
            + "rating INT NOT NULL,"
            + "review_text TEXT,"
            + "UNIQUE KEY uq_review (user_id, book_title),"
            + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)"
        };
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            for (String sql : ddl) s.execute(sql);
            System.out.println("Tables verified/created OK.");
        } catch (SQLException e) {
            System.err.println("initTables ERROR: " + e.getMessage());
            showBootAlert("Database Setup Error",
                "Could not create tables.\n\n" + e.getMessage());
        }
    }

    private void loadBooksFromDatabase() {
        bookCatalogue.clear();
        String sql = "SELECT b.title, b.author, b.genre, b.pages, b.description "
                   + "FROM books b LEFT JOIN reviews r ON r.book_title = b.title "
                   + "GROUP BY b.id, b.title, b.author, b.genre, b.pages, b.description "
                   + "ORDER BY AVG(COALESCE(r.rating,0)) DESC, b.title ASC";
        try (Connection c = getConnection();
             ResultSet rs = c.createStatement().executeQuery(sql)) {
            while (rs.next())
                bookCatalogue.add(new Book(rs.getString("title"), rs.getString("author"),
                    rs.getString("genre"), rs.getInt("pages"), rs.getString("description")));
            System.out.println("Loaded " + bookCatalogue.size() + " books from DB.");
        } catch (SQLException e) {
            System.err.println("loadBooks ERROR: " + e.getMessage());
        }
    }

    // =========================================================================
    //  Screens
    // =========================================================================

    private void showLoginScreen() {
        currentUserId = -1; currentUsername = "";
        TextField userF = field("Enter username");
        PasswordField passF = new PasswordField(); passF.setPromptText("Enter password");
        Label err = errLbl();
        Button loginBtn  = btn("Login",                           "btn-primary");
        Button signupBtn = btn("Don't have an account?  Sign Up", "btn-secondary");
        loginBtn.setMaxWidth(Double.MAX_VALUE); signupBtn.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(10, lbl("BookMatch", "title-label"),
            lbl("Your personal book recommendation engine", "label"), new Separator(),
            lbl("Username", "label"), userF, lbl("Password", "label"), passF,
            err, new Separator(), loginBtn, signupBtn);
        card.getStyleClass().add("login-card"); card.setAlignment(Pos.CENTER);

        loginBtn.setOnAction(e -> handleLogin(userF.getText().trim(), passF.getText(), err));
        passF.setOnAction(e -> loginBtn.fire());
        signupBtn.setOnAction(e -> showSignupScreen());
        setScene(new StackPane(card), 480, 540);
    }

    private void showSignupScreen() {
        TextField userF = field("Choose a username");
        PasswordField passF = new PasswordField(); passF.setPromptText("At least 6 characters");
        PasswordField confF = new PasswordField(); confF.setPromptText("Re-enter password");
        Label err = errLbl();
        Button createBtn = btn("Create Account", "btn-primary");
        Button backBtn   = btn("Back to Login",  "btn-secondary");
        createBtn.setMaxWidth(Double.MAX_VALUE); backBtn.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(10, lbl("Create Account", "title-label"), new Separator(),
            lbl("Username", "label"), userF, lbl("Password", "label"), passF,
            lbl("Confirm Password", "label"), confF, err, new Separator(), createBtn, backBtn);
        card.getStyleClass().add("login-card"); card.setAlignment(Pos.CENTER);

        createBtn.setOnAction(e ->
            handleSignup(userF.getText().trim(), passF.getText(), confF.getText(), err));
        backBtn.setOnAction(e -> showLoginScreen());
        setScene(new StackPane(card), 480, 580);
    }

    private void showDashboard(String username) {
        Label welcome = lbl("Welcome, " + username, "section-label");
        HBox.setHgrow(welcome, Priority.ALWAYS);
        Button favBtn    = btn("My Favourites", "btn-secondary");
        Button adminBtn  = btn("Admin Panel",   "btn-secondary");
        Button logoutBtn = btn("Logout",        "btn-secondary");
        adminBtn.setVisible(username.equals(ADMIN_USER));
        HBox topBar = topBar(welcome, favBtn, adminBtn, logoutBtn);
        logoutBtn.setOnAction(e -> showLoginScreen());
        favBtn.setOnAction(e   -> showFavouritesScreen());
        adminBtn.setOnAction(e -> showAdminPanel());

        HBox trendRow = new HBox(12); trendRow.setPadding(new Insets(6));
        int lim = Math.min(8, bookCatalogue.size());
        for (int i = 0; i < lim; i++) {
            Book b = bookCatalogue.get(i);
            double avg = getAvgRating(b.getTitle());
            Button tb = btn(b.getTitle() + "\n" + b.getAuthor()
                + (avg > 0 ? "\n" + stars(avg) : ""), "btn-trending");
            tb.setOnAction(e -> showBookPopup(b));
            trendRow.getChildren().add(tb);
        }

        ComboBox<String> genreCombo = new ComboBox<>();
        genreCombo.getItems().addAll(buildGenreList()); genreCombo.setValue("Any");
        TextField pagesF    = field("Max pages");     pagesF.setPrefWidth(130);
        TextField titleKeyF = field("Title keyword"); titleKeyF.setPrefWidth(180);
        Button searchBtn = btn("Search", "btn-primary");
        Button clearBtn  = btn("Clear",  "btn-secondary");
        Label searchErr  = errLbl();
        Label resultCount = lbl("Use the filters above and click Search", "label");
        resultCount.setStyle("-fx-text-fill:#7c5cbf;");
        VBox resultsBox = new VBox(10); resultsBox.setPadding(new Insets(4));

        searchBtn.setOnAction(e -> {
            searchErr.setText("");
            int maxP = Integer.MAX_VALUE;
            if (!pagesF.getText().trim().isEmpty()) {
                try { maxP = Integer.parseInt(pagesF.getText().trim());
                      if (maxP <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    searchErr.setText("Max Pages must be a positive number."); return;
                }
            }
            List<Book> res = searchBooks(genreCombo.getValue(), maxP,
                                         titleKeyF.getText().trim().toLowerCase());
            resultsBox.getChildren().clear();
            if (res.isEmpty()) {
                resultCount.setText("No books found. Try adjusting your filters.");
            } else {
                resultCount.setText("Found " + res.size() + " book" + (res.size() == 1 ? "" : "s"));
                res.forEach(b -> resultsBox.getChildren().add(buildResultCard(b)));
            }
        });
        clearBtn.setOnAction(e -> {
            genreCombo.setValue("Any"); pagesF.clear(); titleKeyF.clear();
            searchErr.setText(""); resultsBox.getChildren().clear();
            resultCount.setText("Use the filters above and click Search");
        });
        pagesF.setOnAction(e -> searchBtn.fire());
        titleKeyF.setOnAction(e -> searchBtn.fire());

        HBox controls = new HBox(10,
            lbl("Genre:", "label"), genreCombo,
            lbl("Max Pages:", "label"), pagesF,
            lbl("Title:", "label"), titleKeyF,
            searchBtn, clearBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox searchPanel = new VBox(10, controls, searchErr, resultCount,
                                    scroll(resultsBox, 330, true));
        searchPanel.getStyleClass().add("search-panel");

        VBox content = new VBox(20,
            new VBox(8, lbl("Trending Now  (top rated)", "section-label"),
                        scroll(trendRow, 135, false)),
            new VBox(10, lbl("Find Your Next Read", "section-label"), searchPanel));
        content.setPadding(new Insets(20, 24, 20, 24));

        BorderPane root = new BorderPane();
        root.setTop(topBar); root.setCenter(scroll(content, -1, true));
        setScene(root, 1050, 760);
    }

    private void showFavouritesScreen() {
        Button backBtn = btn("Back to Dashboard", "btn-secondary");
        backBtn.setOnAction(e -> showDashboard(currentUsername));
        HBox tb = topBar(lbl("My Favourites", "title-label"), backBtn);

        VBox list = new VBox(10); list.setPadding(new Insets(10));
        List<String> favs = loadFavourites();
        if (favs.isEmpty()) {
            Label none = lbl("You have no favourites yet.\nClick the heart on any book to add it here.", "label");
            none.setStyle("-fx-text-fill:#7c5cbf;");
            list.getChildren().add(none);
        } else {
            for (String t : favs)
                bookCatalogue.stream().filter(b -> b.getTitle().equals(t)).findFirst()
                    .ifPresent(b -> list.getChildren().add(buildResultCard(b)));
        }
        VBox content = new VBox(16, scroll(list, -1, true));
        content.setPadding(new Insets(20, 24, 20, 24));
        BorderPane root = new BorderPane(); root.setTop(tb); root.setCenter(content);
        setScene(root, 1050, 760);
    }

    private void showAdminPanel() {
        Button backBtn = btn("Back to Dashboard", "btn-secondary");
        backBtn.setOnAction(e -> showDashboard(currentUsername));
        HBox tb = topBar(lbl("Admin Panel", "title-label"), backBtn);

        VBox userRows = new VBox(6); userRows.setPadding(new Insets(8));
        try (Connection c = getConnection();
             ResultSet rs = c.createStatement()
                 .executeQuery("SELECT id, username FROM users ORDER BY id")) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                userRows.getChildren().add(lbl("ID: " + rs.getInt("id")
                    + "   Username: " + rs.getString("username"), "label"));
            }
            if (!any) userRows.getChildren().add(
                lbl("No users registered yet.", "label"));
        } catch (SQLException e) {
            userRows.getChildren().add(lbl("Could not load users: " + e.getMessage(), "label"));
            System.err.println("showAdminPanel users ERROR: " + e.getMessage());
        }
        VBox usersSection = new VBox(8, lbl("Registered Users", "section-label"),
                                     scroll(userRows, 160, true));
        usersSection.getStyleClass().add("search-panel");

        TextField nTitle=field("Title"), nAuthor=field("Author"), nGenre=field("Genre"),
                  nPages=field("Pages"), nDesc=field("Description");
        Label addErr = errLbl();
        Label addOk  = lbl("", "label"); addOk.setStyle("-fx-text-fill:#86efac;");
        Button addBtn = btn("Add Book to Database", "btn-primary");
        addBtn.setOnAction(e -> {
            addErr.setText(""); addOk.setText("");
            String t=nTitle.getText().trim(), a=nAuthor.getText().trim(),
                   g=nGenre.getText().trim(), d=nDesc.getText().trim(),
                   p=nPages.getText().trim();
            if (t.isEmpty()||a.isEmpty()||g.isEmpty()||p.isEmpty()) {
                addErr.setText("Title, Author, Genre and Pages are required."); return;
            }
            int pg;
            try { pg = Integer.parseInt(p); if (pg <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) {
                addErr.setText("Pages must be a positive number."); return;
            }
            try (Connection c2 = getConnection();
                 PreparedStatement ps = c2.prepareStatement(
                     "INSERT INTO books (title,author,genre,pages,description) VALUES(?,?,?,?,?)")) {
                ps.setString(1,t); ps.setString(2,a); ps.setString(3,g);
                ps.setInt(4,pg);   ps.setString(5,d);
                ps.executeUpdate();
                loadBooksFromDatabase();
                addOk.setText("\"" + t + "\" added successfully!");
                nTitle.clear(); nAuthor.clear(); nGenre.clear(); nPages.clear(); nDesc.clear();
            } catch (SQLIntegrityConstraintViolationException ex) {
                addErr.setText("A book with that title already exists.");
            } catch (SQLException ex) {
                addErr.setText("DB error: " + ex.getMessage());
                System.err.println("addBook ERROR: " + ex.getMessage());
            }
        });
        VBox addForm = new VBox(8,
            lbl("Title:", "label"), nTitle, lbl("Author:", "label"), nAuthor,
            lbl("Genre:", "label"), nGenre, lbl("Pages:", "label"), nPages,
            lbl("Description:", "label"), nDesc, addErr, addBtn, addOk);
        addForm.getStyleClass().add("search-panel");

        VBox content = new VBox(20, usersSection,
            new VBox(10, lbl("Add New Book", "section-label"), addForm));
        content.setPadding(new Insets(20, 24, 20, 24));
        BorderPane root = new BorderPane(); root.setTop(tb);
        root.setCenter(scroll(content, -1, true));
        setScene(root, 900, 760);
    }

    private void showBookPopup(Book book) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(primaryStage);
        popup.setTitle(book.getTitle());

        double avg = getAvgRating(book.getTitle());
        Label avgLbl = lbl("Avg Rating: " + (avg > 0
            ? String.format("%.1f/5.0  %s", avg, stars(avg)) : "No ratings yet"), "label");
        avgLbl.setStyle("-fx-text-fill:#fbbf24;-fx-font-weight:bold;");

        boolean[] isFav = { isFavourite(book.getTitle()) };
        Button favBtn = btn(isFav[0] ? "Remove from Favourites" : "Add to Favourites",
                            isFav[0] ? "btn-fav-active" : "btn-secondary");
        favBtn.setOnAction(e -> {
            if (isFav[0]) { removeFavourite(book.getTitle()); isFav[0] = false;
                favBtn.setText("Add to Favourites");
                favBtn.getStyleClass().set(favBtn.getStyleClass().size()-1, "btn-secondary"); }
            else           { addFavourite(book.getTitle());    isFav[0] = true;
                favBtn.setText("Remove from Favourites");
                favBtn.getStyleClass().set(favBtn.getStyleClass().size()-1, "btn-fav-active"); }
        });

        int[] existRating = {0}; String[] existText = {""};
        loadUserReview(book.getTitle(), existRating, existText);
        ComboBox<String> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll("1 - Poor","2 - Fair","3 - Good","4 - Great","5 - Excellent");
        ratingCombo.setPrefWidth(160);
        if (existRating[0] > 0) ratingCombo.getSelectionModel().select(existRating[0] - 1);
        TextArea reviewArea = new TextArea(existText[0]);
        reviewArea.setPromptText("Write a short review (optional)...");
        reviewArea.setPrefRowCount(3); reviewArea.setWrapText(true);
        reviewArea.setStyle("-fx-control-inner-background:#2d1b4e;-fx-text-fill:#e0d4f7;");
        Label revErr = errLbl();
        Button saveBtn = btn(existRating[0] > 0 ? "Update Review" : "Save Review", "btn-primary");
        saveBtn.setOnAction(e -> {
            revErr.setText("");
            if (ratingCombo.getValue() == null) {
                revErr.setText("Please select a rating."); return;
            }
            int r = ratingCombo.getSelectionModel().getSelectedIndex() + 1;
            saveReview(book.getTitle(), r, reviewArea.getText().trim());
            saveBtn.setText("Update Review");
            double na = getAvgRating(book.getTitle());
            avgLbl.setText("Avg Rating: " + String.format("%.1f/5.0  %s", na, stars(na)));
            showAlert("Review Saved", "Your review has been saved!", Alert.AlertType.INFORMATION);
        });

        VBox allRevs = new VBox(6);
        loadAllReviews(book.getTitle(), allRevs);

        Label tLbl = lbl(book.getTitle(), "title-label");
        tLbl.setStyle("-fx-font-size:20px;"); tLbl.setWrapText(true);
        Label descLbl = lbl(book.getDescription(), "label");
        descLbl.setWrapText(true); descLbl.setMaxWidth(460);
        descLbl.setStyle("-fx-text-fill:#c4b5d6;");

        VBox left = new VBox(10, tLbl, new Separator(),
            lbl("Author:  " + book.getAuthor(), "label"),
            lbl("Genre:   " + book.getGenre(),  "label"),
            lbl("Pages:   " + book.getPages(),  "label"),
            avgLbl, new Separator(), descLbl, favBtn);
        left.setPrefWidth(300);

        VBox right = new VBox(10, lbl("Your Rating & Review", "section-label"),
            new HBox(10, lbl("Rating:", "label"), ratingCombo),
            reviewArea, revErr, saveBtn, new Separator(),
            lbl("Community Reviews", "section-label"), scroll(allRevs, 110, true));
        right.setPrefWidth(300);

        Button closeBtn = btn("Close", "btn-primary");
        closeBtn.setOnAction(e -> popup.close());
        VBox wrapper = new VBox(new HBox(24, left, right), closeBtn);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(20, 24, 20, 24));
        wrapper.setSpacing(10); wrapper.getStyleClass().add("login-card");

        Scene scene = new Scene(new StackPane(wrapper), 680, 600);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        popup.setScene(scene); popup.showAndWait();
        loadBooksFromDatabase();
    }

    // =========================================================================
    //  Auth handlers
    // =========================================================================

    private void handleLogin(String username, String password, Label err) {
        err.setText("");
        if (username.isEmpty() || password.isEmpty()) {
            err.setText("Username and password cannot be empty."); return;
        }
        if (username.equals(ADMIN_USER) && password.equals(ADMIN_PASS)) {
            currentUserId = 0; currentUsername = ADMIN_USER;
            showDashboard(ADMIN_USER); return;
        }
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id FROM users WHERE username = ? AND password = ?")) {
            ps.setString(1, username); ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentUserId = rs.getInt("id");
                currentUsername = username;
                System.out.println("Login OK: " + username + " (id=" + currentUserId + ")");
                showDashboard(username);
            } else {
                err.setText("Incorrect username or password.");
            }
        } catch (SQLException e) {
            err.setText("Database connection error: " + e.getMessage());
            System.err.println("LOGIN ERROR: " + e.getMessage());
        }
    }

    private void handleSignup(String username, String password, String confirm, Label err) {
        err.setText("");
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            err.setText("All fields are required."); return;
        }
        if (username.length() > 50) {
            err.setText("Username must be 50 characters or fewer."); return;
        }
        if (!password.equals(confirm)) {
            err.setText("Passwords do not match."); return;
        }
        if (password.length() < 6) {
            err.setText("Password must be at least 6 characters."); return;
        }

        System.out.println("Attempting signup for: " + username);
        try (Connection c = getConnection()) {
            // Extra safety: helps confirm which DB we're in
            System.out.println("  Signup using catalog: " + c.getCatalog());
            
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, password);
            int rows = ps.executeUpdate();
            System.out.println("  INSERT rows affected: " + rows);

            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next())
                    System.out.println("  New user ID: " + keys.getInt(1));
                // Verify it's actually there
                ResultSet check = c.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM users");
                if (check.next())
                    System.out.println("  Total users after insert: " + check.getInt(1));

                showAlert("Account Created",
                    "Account created successfully!\nYou can now log in with: " + username,
                    Alert.AlertType.INFORMATION);
                showLoginScreen();
            } else {
                err.setText("Signup failed — INSERT returned 0 rows. Check Eclipse console.");
                System.err.println("  INSERT returned 0 rows — unexpected.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            err.setText("Username \"" + username + "\" is already taken.");
            System.err.println("  Duplicate username: " + username);
        } catch (SQLException e) {
            err.setText("Database error: " + e.getMessage());
            System.err.println("SIGNUP SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  DB operations
    // =========================================================================

    private List<Book> searchBooks(String genre, int maxPages, String keyword) {
        List<Book> res = new ArrayList<>();
        for (Book b : bookCatalogue) {
            if (("Any".equals(genre) || b.getGenre().equalsIgnoreCase(genre))
                && b.getPages() <= maxPages
                && (keyword.isEmpty()
                    || b.getTitle().toLowerCase().contains(keyword)
                    || b.getAuthor().toLowerCase().contains(keyword)))
                res.add(b);
        }
        return res;
    }

    private List<String> loadFavourites() {
        List<String> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT book_title FROM favourites WHERE user_id = ?")) {
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("book_title"));
        } catch (SQLException e) { System.err.println("loadFavs: " + e.getMessage()); }
        return list;
    }

    private boolean isFavourite(String title) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id FROM favourites WHERE user_id = ? AND book_title = ?")) {
            ps.setInt(1, currentUserId); ps.setString(2, title);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    private void addFavourite(String title) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT IGNORE INTO favourites (user_id, book_title) VALUES (?, ?)")) {
            ps.setInt(1, currentUserId); ps.setString(2, title); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("addFav: " + e.getMessage()); }
    }

    private void removeFavourite(String title) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM favourites WHERE user_id = ? AND book_title = ?")) {
            ps.setInt(1, currentUserId); ps.setString(2, title); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("removeFav: " + e.getMessage()); }
    }

    private double getAvgRating(String title) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT AVG(rating) FROM reviews WHERE book_title = ?")) {
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("avgRating: " + e.getMessage()); }
        return 0.0;
    }

    private void loadUserReview(String title, int[] ratingOut, String[] textOut) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT rating, review_text FROM reviews WHERE user_id = ? AND book_title = ?")) {
            ps.setInt(1, currentUserId); ps.setString(2, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ratingOut[0] = rs.getInt("rating");
                textOut[0] = rs.getString("review_text") != null
                           ? rs.getString("review_text") : "";
            }
        } catch (SQLException e) { System.err.println("loadReview: " + e.getMessage()); }
    }

    private void saveReview(String title, int rating, String text) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO reviews (user_id, book_title, rating, review_text) VALUES (?,?,?,?) "
               + "ON DUPLICATE KEY UPDATE rating=VALUES(rating), review_text=VALUES(review_text)")) {
            ps.setInt(1, currentUserId); ps.setString(2, title);
            ps.setInt(3, rating);        ps.setString(4, text);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("saveReview: " + e.getMessage()); }
    }

    private void loadAllReviews(String title, VBox container) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT u.username, r.rating, r.review_text FROM reviews r "
               + "JOIN users u ON r.user_id = u.id "
               + "WHERE r.book_title = ? ORDER BY r.id DESC")) {
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            boolean any = false;
            while (rs.next()) {
                any = true;
                String t2 = rs.getString("review_text");
                String line = rs.getString("username") + "  " + stars(rs.getInt("rating"))
                    + (t2 != null && !t2.isEmpty() ? "  \"" + t2 + "\"" : "");
                Label l = lbl(line, "label");
                l.setWrapText(true);
                l.setStyle("-fx-font-size:12px;-fx-text-fill:#c4b5d6;");
                container.getChildren().add(l);
            }
            if (!any) {
                Label l = lbl("No community reviews yet. Be the first!", "label");
                l.setStyle("-fx-font-size:12px;-fx-text-fill:#7c5cbf;");
                container.getChildren().add(l);
            }
        } catch (SQLException e) { System.err.println("allReviews: " + e.getMessage()); }
    }

    // =========================================================================
    //  UI builders
    // =========================================================================

    private VBox buildResultCard(Book book) {
        Label titleLbl = lbl(book.getTitle() + " (" + book.getPages() + " pages)", "section-label");
        titleLbl.setStyle("-fx-font-size:15px;");
        Label metaLbl = lbl("by " + book.getAuthor() + "   ·   " + book.getGenre(), "label");
        metaLbl.setStyle("-fx-text-fill:#a78bfa;");
        double avg = getAvgRating(book.getTitle());
        Label ratLbl = lbl(avg > 0
            ? "Rating: " + String.format("%.1f", avg) + "/5  " + stars(avg)
            : "Not yet rated", "label");
        ratLbl.setStyle("-fx-text-fill:#fbbf24;-fx-font-size:12px;");
        Label descLbl = lbl(book.getDescription(), "label");
        descLbl.setWrapText(true); descLbl.setStyle("-fx-text-fill:#c4b5d6;");

        boolean[] isFav = { isFavourite(book.getTitle()) };
        Button favBtn = btn(isFav[0] ? "Remove Fav" : "Favourite",
                            isFav[0] ? "btn-fav-active" : "btn-secondary");
        favBtn.setOnAction(e -> {
            if (isFav[0]) { removeFavourite(book.getTitle()); isFav[0] = false;
                favBtn.setText("Favourite");
                favBtn.getStyleClass().set(favBtn.getStyleClass().size()-1, "btn-secondary"); }
            else           { addFavourite(book.getTitle());   isFav[0] = true;
                favBtn.setText("Remove Fav");
                favBtn.getStyleClass().set(favBtn.getStyleClass().size()-1, "btn-fav-active"); }
        });
        Button detailBtn = btn("View Details / Rate", "btn-secondary");
        detailBtn.setOnAction(e -> showBookPopup(book));
        VBox card = new VBox(6, titleLbl, metaLbl, ratLbl, descLbl,
                             new HBox(10, detailBtn, favBtn));
        card.getStyleClass().add("card");
        return card;
    }

    private List<String> buildGenreList() {
        List<String> g = new ArrayList<>();
        bookCatalogue.forEach(b -> { if (!g.contains(b.getGenre())) g.add(b.getGenre()); });
        g.sort(String::compareTo); g.add(0, "Any");
        return g;
    }

    // =========================================================================
    //  Utilities
    // =========================================================================

    private String stars(double r) {
        int f = (int) Math.round(r); StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < f ? "\u2605" : "\u2606");
        return sb.toString();
    }

    private Label     lbl(String t, String s) { Label l=new Label(t); l.getStyleClass().add(s); return l; }
    private Label     errLbl()                { return lbl("", "error-label"); }
    private Button    btn(String t, String s) { Button b=new Button(t); b.getStyleClass().add(s); return b; }
    private TextField field(String p)         { TextField tf=new TextField(); tf.setPromptText(p); return tf; }

    private ScrollPane scroll(javafx.scene.Node content, double prefH, boolean fitW) {
        ScrollPane sp = new ScrollPane(content); sp.getStyleClass().add("scroll-pane");
        if (fitW) { sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); }
        else      { sp.setFitToHeight(true); sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); }
        if (prefH > 0) sp.setPrefHeight(prefH);
        return sp;
    }

    private HBox topBar(javafx.scene.Node... nodes) {
        HBox bar = new HBox(10, nodes); bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nodes[0], Priority.ALWAYS);
        return bar;
    }

    private void setScene(javafx.scene.Parent root, double w, double h) {
        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setScene(scene); primaryStage.centerOnScreen(); primaryStage.show();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(title); a.setContentText(msg);
        if (primaryStage != null && primaryStage.getScene() != null) a.initOwner(primaryStage);
        DialogPane dp = a.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dp.getStyleClass().add("dialog-pane"); a.showAndWait();
    }

    private void showBootAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(msg); a.showAndWait();
    }
}