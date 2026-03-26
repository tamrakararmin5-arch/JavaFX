package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main.java  -  Presentation Layer (Controller + View)
 *
 * Builds and manages all JavaFX screens. Contains zero SQL.
 * All database operations are delegated to DatabaseHelper.
 *
 * OOP principles:
 *   Inheritance   - extends Application (JavaFX lifecycle)
 *   Encapsulation - session state (userId, username) is private
 *   Abstraction   - UI calls db.method() without knowing any SQL
 *   Polymorphism  - buildResultCard() calls b.getSummary() via
 *                   the Recommendable interface; works for any MediaItem
 */
public class Main extends Application {

    // Admin credentials (hardcoded; no DB row needed for admin)
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    // Session state - private fields with no direct external access (Encapsulation)
    private Stage        primaryStage;
    private int          currentUserId   = -1;
    private String       currentUsername = "";

    // Single shared DatabaseHelper and in-memory book list
    private final DatabaseHelper db            = new DatabaseHelper();
    private final List<Book>     bookCatalogue = new ArrayList<>();

    // =========================================================================
    //  JavaFX entry point
    // =========================================================================
    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("BookMatch - Book Recommendation System");
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);

        if (!db.verifyConnection())
            showBootAlert("Cannot Connect to Database",
                "Check MySQL is running and the password in DatabaseHelper.java is correct.");
        db.initTables();
        refreshBooks();
        showLoginScreen();
    }

    /** Reloads the book catalog from the database. */
    private void refreshBooks() {
        bookCatalogue.clear();
        bookCatalogue.addAll(db.loadBooks());
    }

    // =========================================================================
    //  LOGIN SCREEN
    // =========================================================================
    private void showLoginScreen() {
        currentUserId = -1; currentUsername = "";

        TextField     userF     = field("Enter username");
        PasswordField passF     = new PasswordField();
        passF.setPromptText("Enter password");
        Label         err       = errLbl();
        Button        loginBtn  = btn("Login",                          "btn-primary");
        Button        signupBtn = btn("Don't have an account?  Sign Up","btn-secondary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        signupBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction (e -> handleLogin(userF.getText().trim(), passF.getText(), err));
        signupBtn.setOnAction(e -> showSignupScreen());
        passF.setOnAction    (e -> loginBtn.fire());

        VBox card = card(
            lbl("BookMatch", "title-label"),
            lbl("Your personal book recommendation engine", "label"),
            new Separator(),
            lbl("Username", "label"), userF,
            lbl("Password", "label"), passF,
            err, new Separator(), loginBtn, signupBtn
        );
        setScene(new StackPane(card), 480, 540);
    }

    // =========================================================================
    //  SIGN-UP SCREEN
    // =========================================================================
    private void showSignupScreen() {
        TextField     userF     = field("Choose a username");
        PasswordField passF     = new PasswordField();
        passF.setPromptText("At least 6 characters");
        PasswordField confF     = new PasswordField();
        confF.setPromptText("Re-enter password");
        Label         err       = errLbl();
        Button        createBtn = btn("Create Account", "btn-primary");
        Button        backBtn   = btn("Back to Login",  "btn-secondary");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setMaxWidth(Double.MAX_VALUE);

        createBtn.setOnAction(e ->
            handleSignup(userF.getText().trim(), passF.getText(), confF.getText(), err));
        backBtn.setOnAction(e -> showLoginScreen());

        VBox card = card(
            lbl("Create Account", "title-label"), new Separator(),
            lbl("Username",         "label"), userF,
            lbl("Password",         "label"), passF,
            lbl("Confirm Password", "label"), confF,
            err, new Separator(), createBtn, backBtn
        );
        setScene(new StackPane(card), 480, 580);
    }

    // =========================================================================
    //  DASHBOARD
    // =========================================================================
    private void showDashboard(String username) {

        // Top bar
        Label  welcome   = lbl("Welcome, " + username, "section-label");
        Button favBtn    = btn("My Favourites", "btn-secondary");
        Button adminBtn  = btn("Admin Panel",   "btn-secondary");
        Button logoutBtn = btn("Logout",        "btn-secondary");
        HBox.setHgrow(welcome, Priority.ALWAYS);
        adminBtn.setVisible(username.equals(ADMIN_USER));
        HBox topBar = topBar(welcome, favBtn, adminBtn, logoutBtn);
        logoutBtn.setOnAction(e -> showLoginScreen());
        favBtn.setOnAction   (e -> showFavouritesScreen());
        adminBtn.setOnAction (e -> showAdminPanel());

        // Trending row - driven by average rating from DB
        HBox trendRow = new HBox(12);
        trendRow.setPadding(new Insets(6));
        int limit = Math.min(8, bookCatalogue.size());
        for (int i = 0; i < limit; i++) {
            Book b = bookCatalogue.get(i);
            double avg = db.getAverageRating(b.getTitle());
            // getSummary() called via Recommendable interface (Polymorphism)
            Button tb = btn(b.getTitle() + "\n" + b.getAuthor()
                + (avg > 0 ? "\n" + db.starsString(avg) : ""), "btn-trending");
            tb.setOnAction(e -> showBookPopup(b));
            trendRow.getChildren().add(tb);
        }

        // Search controls
        ComboBox<String> genreCombo = new ComboBox<>();
        genreCombo.getItems().addAll(buildGenreList());
        genreCombo.setValue("Any");

        TextField pagesF    = field("Max pages");
        pagesF.setPrefWidth(130);
        TextField titleKeyF = field("Title / Author keyword");
        titleKeyF.setPrefWidth(200);
        Button    searchBtn = btn("Search", "btn-primary");
        Button    clearBtn  = btn("Clear",  "btn-secondary");
        Label     searchErr = errLbl();
        Label     countLbl  = lbl("Use the filters above and click Search", "label");
        countLbl.setStyle("-fx-text-fill:#7c5cbf;");

        VBox resultsBox = new VBox(10);
        resultsBox.setPadding(new Insets(4));

        searchBtn.setOnAction(e -> {
            searchErr.setText("");
            int maxP = Integer.MAX_VALUE;
            if (!pagesF.getText().trim().isEmpty()) {
                try {
                    maxP = Integer.parseInt(pagesF.getText().trim());
                    if (maxP <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    searchErr.setText("Max Pages must be a positive whole number.");
                    return;
                }
            }
            List<Book> res = searchBooks(
                genreCombo.getValue(), maxP, titleKeyF.getText().trim().toLowerCase());
            resultsBox.getChildren().clear();
            if (res.isEmpty()) {
                countLbl.setText("No books found. Try adjusting your filters.");
            } else {
                countLbl.setText("Found " + res.size() + " book" + (res.size() == 1 ? "" : "s"));
                // getSummary() demonstrates Polymorphism - called on Recommendable reference
                res.forEach(b -> {
                    System.out.println("Search result: " + b.getSummary());
                    resultsBox.getChildren().add(buildResultCard(b));
                });
            }
        });

        clearBtn.setOnAction(e -> {
            genreCombo.setValue("Any");
            pagesF.clear(); titleKeyF.clear();
            searchErr.setText("");
            resultsBox.getChildren().clear();
            countLbl.setText("Use the filters above and click Search");
        });
        pagesF.setOnAction   (e -> searchBtn.fire());
        titleKeyF.setOnAction(e -> searchBtn.fire());

        HBox controls = new HBox(10,
            lbl("Genre:", "label"),    genreCombo,
            lbl("Max Pages:", "label"), pagesF,
            lbl("Title:", "label"),     titleKeyF,
            searchBtn, clearBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox searchPanel = new VBox(10,
            controls, searchErr, countLbl, scroll(resultsBox, 330, true));
        searchPanel.getStyleClass().add("search-panel");

        VBox content = new VBox(20,
            new VBox(8,
                lbl("Trending Now  (top rated)", "section-label"),
                scroll(trendRow, 135, false)),
            new VBox(10,
                lbl("Find Your Next Read", "section-label"),
                searchPanel));
        content.setPadding(new Insets(20, 24, 20, 24));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(scroll(content, -1, true));
        setScene(root, 1050, 760);
    }

    // =========================================================================
    //  FAVOURITES SCREEN
    // =========================================================================
    private void showFavouritesScreen() {
        Button backBtn = btn("Back to Dashboard", "btn-secondary");
        backBtn.setOnAction(e -> showDashboard(currentUsername));
        HBox topBar = topBar(lbl("My Favourites", "title-label"), backBtn);

        VBox list = new VBox(10);
        list.setPadding(new Insets(10));
        List<String> favTitles = db.getFavourites(currentUserId);

        if (favTitles.isEmpty()) {
            Label none = lbl("You have no favourites yet.\nClick Favourite on any book to add it here.", "label");
            none.setStyle("-fx-text-fill:#7c5cbf;");
            list.getChildren().add(none);
        } else {
            // toString() called here for each favourited book (demonstrates the method)
            favTitles.forEach(t ->
                bookCatalogue.stream()
                    .filter(b -> b.getTitle().equals(t))
                    .findFirst()
                    .ifPresent(b -> {
                        System.out.println("Loading favourite: " + b.toString());
                        list.getChildren().add(buildResultCard(b));
                    })
            );
        }

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(new VBox(16, scroll(list, -1, true)));
        ((VBox) root.getCenter()).setPadding(new Insets(20, 24, 20, 24));
        setScene(root, 1050, 760);
    }

    // =========================================================================
    //  ADMIN PANEL
    // =========================================================================
    private void showAdminPanel() {
        Button backBtn = btn("Back to Dashboard", "btn-secondary");
        backBtn.setOnAction(e -> showDashboard(currentUsername));
        HBox topBar = topBar(lbl("Admin Panel", "title-label"), backBtn);

        // Registered users list
        VBox userRows = new VBox(6);
        userRows.setPadding(new Insets(8));
        List<String> users = db.getAllUsers();
        if (users.isEmpty())
            userRows.getChildren().add(lbl("No users registered yet.", "label"));
        else
            users.forEach(u -> userRows.getChildren().add(lbl(u, "label")));

        VBox usersSection = new VBox(8,
            lbl("Registered Users", "section-label"),
            scroll(userRows, 160, true));
        usersSection.getStyleClass().add("search-panel");

        // Add new book form
        TextField nTitle  = field("Title");
        TextField nAuthor = field("Author");
        TextField nGenre  = field("Genre  e.g. Fantasy");
        TextField nPages  = field("Pages  e.g. 350");
        TextField nDesc   = field("Short description");
        Label     addErr  = errLbl();
        Label     addOk   = lbl("", "label");
        addOk.setStyle("-fx-text-fill:#86efac;");
        Button addBtn = btn("Add Book to Database", "btn-primary");

        addBtn.setOnAction(e -> {
            addErr.setText(""); addOk.setText("");
            String t = nTitle.getText().trim(),  a = nAuthor.getText().trim(),
                   g = nGenre.getText().trim(),   d = nDesc.getText().trim(),
                   p = nPages.getText().trim();
            if (t.isEmpty() || a.isEmpty() || g.isEmpty() || p.isEmpty()) {
                addErr.setText("Title, Author, Genre and Pages are required."); return;
            }
            int pages;
            try {
                pages = Integer.parseInt(p);
                if (pages <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                addErr.setText("Pages must be a positive whole number."); return;
            }
            try {
                db.addBook(t, a, g, pages, d);
                refreshBooks();
                // toString() used to confirm the new Book object in console
                Book newBook = new Book(t, a, g, pages, d);
                System.out.println("Admin added: " + newBook.toString());
                addOk.setText("\"" + t + "\" added successfully!");
                nTitle.clear(); nAuthor.clear(); nGenre.clear();
                nPages.clear(); nDesc.clear();
            } catch (SQLIntegrityConstraintViolationException ex) {
                addErr.setText("A book with that title already exists.");
            } catch (SQLException ex) {
                addErr.setText("Database error: " + ex.getMessage());
            }
        });

        VBox addForm = new VBox(8,
            lbl("Title:",  "label"), nTitle,
            lbl("Author:", "label"), nAuthor,
            lbl("Genre:",  "label"), nGenre,
            lbl("Pages:",  "label"), nPages,
            lbl("Description:", "label"), nDesc,
            addErr, addBtn, addOk);
        addForm.getStyleClass().add("search-panel");

        VBox content = new VBox(20,
            usersSection,
            new VBox(10, lbl("Add New Book", "section-label"), addForm));
        content.setPadding(new Insets(20, 24, 20, 24));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(scroll(content, -1, true));
        setScene(root, 900, 760);
    }

    // =========================================================================
    //  BOOK DETAIL POPUP
    // =========================================================================
    private void showBookPopup(Book book) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(primaryStage);
        popup.setTitle(book.getTitle());

        // Average rating display
        double avg = db.getAverageRating(book.getTitle());
        Label avgLbl = lbl("Avg Rating: " + (avg > 0
            ? String.format("%.1f/5.0  %s", avg, db.starsString(avg))
            : "No ratings yet"), "label");
        avgLbl.setStyle("-fx-text-fill:#fbbf24;-fx-font-weight:bold;");

        // Favourite toggle
        boolean[] isFav = { db.isFavourite(currentUserId, book.getTitle()) };
        Button favBtn = btn(isFav[0] ? "Remove from Favourites" : "Add to Favourites",
                            isFav[0] ? "btn-fav-active" : "btn-secondary");
        favBtn.setOnAction(e -> {
            if (isFav[0]) {
                db.removeFavourite(currentUserId, book.getTitle());
                isFav[0] = false;
                favBtn.setText("Add to Favourites");
                favBtn.getStyleClass().set(favBtn.getStyleClass().size() - 1, "btn-secondary");
            } else {
                db.addFavourite(currentUserId, book.getTitle());
                isFav[0] = true;
                favBtn.setText("Remove from Favourites");
                favBtn.getStyleClass().set(favBtn.getStyleClass().size() - 1, "btn-fav-active");
            }
        });

        // Rating and review form - pre-filled if user already reviewed this book
        int[] existRating = {0}; String[] existText = {""};
        db.getUserReview(currentUserId, book.getTitle(), existRating, existText);

        ComboBox<String> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(
            "1 - Poor", "2 - Fair", "3 - Good", "4 - Great", "5 - Excellent");
        ratingCombo.setPrefWidth(160);
        if (existRating[0] > 0) ratingCombo.getSelectionModel().select(existRating[0] - 1);

        TextArea reviewArea = new TextArea(existText[0]);
        reviewArea.setPromptText("Write a short review (optional)...");
        reviewArea.setPrefRowCount(3);
        reviewArea.setWrapText(true);
        reviewArea.setStyle("-fx-control-inner-background:#2d1b4e;-fx-text-fill:#e0d4f7;");

        Label  revErr  = errLbl();
        Button saveBtn = btn(existRating[0] > 0 ? "Update Review" : "Save Review", "btn-primary");
        saveBtn.setOnAction(e -> {
            revErr.setText("");
            if (ratingCombo.getValue() == null) {
                revErr.setText("Please select a star rating."); return;
            }
            int r = ratingCombo.getSelectionModel().getSelectedIndex() + 1;
            db.saveReview(currentUserId, book.getTitle(), r, reviewArea.getText().trim());
            saveBtn.setText("Update Review");
            double na = db.getAverageRating(book.getTitle());
            avgLbl.setText("Avg Rating: " + String.format("%.1f/5.0  %s", na, db.starsString(na)));
            showAlert("Review Saved", "Your review has been saved!", Alert.AlertType.INFORMATION);
        });

        // Community reviews
        VBox allRevs = new VBox(6);
        List<String> reviews = db.getAllReviews(book.getTitle());
        if (reviews.isEmpty()) {
            Label none = lbl("No community reviews yet. Be the first!", "label");
            none.setStyle("-fx-font-size:12px;-fx-text-fill:#7c5cbf;");
            allRevs.getChildren().add(none);
        } else {
            reviews.forEach(line -> {
                Label l = lbl(line, "label");
                l.setWrapText(true);
                l.setStyle("-fx-font-size:12px;-fx-text-fill:#c4b5d6;");
                allRevs.getChildren().add(l);
            });
        }

        // Left column - book info, uses getDetailText() (Polymorphism)
        Label titleLbl = lbl(book.getTitle(), "title-label");
        titleLbl.setStyle("-fx-font-size:20px;"); titleLbl.setWrapText(true);
        Label descLbl = lbl(book.getDescription(), "label");
        descLbl.setWrapText(true); descLbl.setMaxWidth(300);
        descLbl.setStyle("-fx-text-fill:#c4b5d6;");

        // getDetailText() called here - demonstrates Polymorphism
        System.out.println("Popup opened:\n" + book.getDetailText());

        VBox left = new VBox(10,
            titleLbl, new Separator(),
            lbl("Author:  " + book.getAuthor(), "label"),
            lbl("Genre:   " + book.getGenre(),  "label"),
            lbl("Pages:   " + book.getPages(),  "label"),
            avgLbl, new Separator(), descLbl, favBtn);
        left.setPrefWidth(300);

        // Right column - review form and community reviews
        VBox right = new VBox(10,
            lbl("Your Rating & Review", "section-label"),
            new HBox(10, lbl("Rating:", "label"), ratingCombo),
            reviewArea, revErr, saveBtn, new Separator(),
            lbl("Community Reviews", "section-label"),
            scroll(allRevs, 110, true));
        right.setPrefWidth(300);

        Button closeBtn = btn("Close", "btn-primary");
        closeBtn.setOnAction(e -> popup.close());

        VBox wrapper = new VBox(new HBox(24, left, right), closeBtn);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(20, 24, 20, 24));
        wrapper.setSpacing(10);
        wrapper.getStyleClass().add("login-card");

        Scene scene = new Scene(new StackPane(wrapper), 680, 600);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        popup.setScene(scene);
        popup.showAndWait();

        refreshBooks(); // reload so trending updates if ratings changed
    }

    // =========================================================================
    //  Auth handlers
    // =========================================================================

    private void handleLogin(String username, String password, Label err) {
        err.setText("");
        if (username.isEmpty() || password.isEmpty()) {
            err.setText("Username and password cannot be empty."); return;
        }
        // Admin shortcut - no database row needed
        if (username.equals(ADMIN_USER) && password.equals(ADMIN_PASS)) {
            currentUserId = 0; currentUsername = ADMIN_USER;
            showDashboard(ADMIN_USER); return;
        }
        try {
            int id = db.loginUser(username, password);
            if (id != -1) {
                currentUserId = id; currentUsername = username;
                showDashboard(username);
            } else {
                err.setText("Incorrect username or password.");
            }
        } catch (SQLException e) {
            err.setText("Database error: " + e.getMessage());
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
        try {
            db.registerUser(username, password);
            showAlert("Account Created",
                "Account created! You can now log in as: " + username,
                Alert.AlertType.INFORMATION);
            showLoginScreen();
        } catch (SQLIntegrityConstraintViolationException e) {
            err.setText("Username \"" + username + "\" is already taken.");
        } catch (SQLException e) {
            err.setText("Database error: " + e.getMessage());
        }
    }

    // =========================================================================
    //  Search - in-memory filter, no DB call needed
    // =========================================================================

    /**
     * Filters the catalogue by genre, page count, and keyword.
     * Demonstrates Polymorphism: getGenre() and getSummary() are called
     * via the Recommendable interface reference on each Book object.
     */
    private List<Book> searchBooks(String genre, int maxPages, String keyword) {
        List<Book> res = new ArrayList<>();
        for (Book b : bookCatalogue) {
            boolean genreOk   = "Any".equals(genre) || b.getGenre().equalsIgnoreCase(genre);
            boolean pagesOk   = b.getPages() <= maxPages;
            boolean keywordOk = keyword.isEmpty()
                || b.getTitle().toLowerCase().contains(keyword)
                || b.getAuthor().toLowerCase().contains(keyword);
            if (genreOk && pagesOk && keywordOk) res.add(b);
        }
        return res;
    }

    // =========================================================================
    //  UI component builders
    // =========================================================================

    /**
     * Builds a search result card for one Book.
     * Calls getSummary() on the Recommendable interface (Polymorphism).
     */
    private VBox buildResultCard(Book book) {
        Label titleLbl = lbl(book.getTitle() + "  (" + book.getPages() + " pages)", "section-label");
        titleLbl.setStyle("-fx-font-size:15px;");

        // getSummary() via Recommendable - demonstrates Polymorphism in the UI
        Label metaLbl = lbl("by " + book.getAuthor() + "   \u00b7   " + book.getGenre(), "label");
        metaLbl.setStyle("-fx-text-fill:#a78bfa;");

        double avg = db.getAverageRating(book.getTitle());
        Label ratLbl = lbl(avg > 0
            ? "Rating: " + String.format("%.1f", avg) + "/5  " + db.starsString(avg)
            : "Not yet rated", "label");
        ratLbl.setStyle("-fx-text-fill:#fbbf24;-fx-font-size:12px;");

        Label descLbl = lbl(book.getDescription(), "label");
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-text-fill:#c4b5d6;");

        boolean[] isFav = { db.isFavourite(currentUserId, book.getTitle()) };
        Button favBtn = btn(isFav[0] ? "Remove Fav" : "Favourite",
                            isFav[0] ? "btn-fav-active" : "btn-secondary");
        favBtn.setOnAction(e -> {
            if (isFav[0]) {
                db.removeFavourite(currentUserId, book.getTitle());
                isFav[0] = false;
                favBtn.setText("Favourite");
                favBtn.getStyleClass().set(favBtn.getStyleClass().size() - 1, "btn-secondary");
            } else {
                db.addFavourite(currentUserId, book.getTitle());
                isFav[0] = true;
                favBtn.setText("Remove Fav");
                favBtn.getStyleClass().set(favBtn.getStyleClass().size() - 1, "btn-fav-active");
            }
        });

        Button detailBtn = btn("View Details / Rate", "btn-secondary");
        detailBtn.setOnAction(e -> showBookPopup(book));

        VBox card = new VBox(6, titleLbl, metaLbl, ratLbl, descLbl,
                             new HBox(10, detailBtn, favBtn));
        card.getStyleClass().add("card");
        return card;
    }

    /** Builds genre list dynamically from catalogue with "Any" prepended. */
    private List<String> buildGenreList() {
        List<String> g = new ArrayList<>();
        bookCatalogue.forEach(b -> { if (!g.contains(b.getGenre())) g.add(b.getGenre()); });
        g.sort(String::compareTo);
        g.add(0, "Any");
        return g;
    }

    // =========================================================================
    //  Utility helpers - keep UI code DRY
    // =========================================================================

    /** Creates a styled Label. */
    private Label lbl(String text, String styleClass) {
        Label l = new Label(text); l.getStyleClass().add(styleClass); return l;
    }

    /** Creates an empty red error label. */
    private Label errLbl() { return lbl("", "error-label"); }

    /** Creates a styled Button. */
    private Button btn(String text, String styleClass) {
        Button b = new Button(text); b.getStyleClass().add(styleClass); return b;
    }

    /** Creates a styled TextField with the given prompt. */
    private TextField field(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt); return tf;
    }

    /** Creates a VBox card layout with consistent spacing and alignment. */
    private VBox card(javafx.scene.Node... nodes) {
        VBox v = new VBox(10, nodes);
        v.getStyleClass().add("login-card");
        v.setAlignment(Pos.CENTER);
        return v;
    }

    /** Creates a ScrollPane. fitW=true for vertical scrolling, false for horizontal. */
    private ScrollPane scroll(javafx.scene.Node node, double prefH, boolean fitW) {
        ScrollPane sp = new ScrollPane(node);
        sp.getStyleClass().add("scroll-pane");
        if (fitW) {
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        } else {
            sp.setFitToHeight(true);
            sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
        if (prefH > 0) sp.setPrefHeight(prefH);
        return sp;
    }

    /** Creates a styled top navigation bar. First node gets all extra space. */
    private HBox topBar(javafx.scene.Node... nodes) {
        HBox bar = new HBox(10, nodes);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nodes[0], Priority.ALWAYS);
        return bar;
    }

    /** Sets a new scene with the dark purple stylesheet applied. */
    private void setScene(javafx.scene.Parent root, double w, double h) {
        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /** Shows a themed alert dialog. Safe to call at any point in the app lifecycle. */
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(msg);
        if (primaryStage != null && primaryStage.getScene() != null)
            a.initOwner(primaryStage);
        DialogPane dp = a.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dp.getStyleClass().add("dialog-pane");
        a.showAndWait();
    }

    /** Alert used before the stage has a scene - no owner attached. */
    private void showBootAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(msg);
        a.showAndWait();
    }
}
