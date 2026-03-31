package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an authenticated user.
 */
public class User {

    public enum Role { PRODUCT_OWNER, SCRUM_MASTER, SCRUM_TEAM }

    // --- Fields ---
    private String username;
    private String passwordHash;  // store plain text for now
    private Role   role;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //
    public User(String username, String passwordHash, Role role) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role;
    }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //
    public String getUsername()     { return username; }
    public Role   getRole()         { return role; }
    public String getPasswordHash() { return passwordHash; }
}


// ======================================================================
//  UserStore  –  simple credential store  (Req 16) with plain-text passwords
// ======================================================================

class UserStore {

    private final File userFile = new File("data/users.txt");
    private final Map<String, User> users = new HashMap<>();

    // ------------------------------------------------------------------ //
    //  Constructor – seed defaults and load from file
    // ------------------------------------------------------------------ //
    public UserStore() {
        // DEMO ONLY: one default account per role seeded on every startup.
        // These are always present regardless of data/users.txt contents.
        // Credentials are listed in the README under Default Accounts.
        addUser(new User("productowner", "po123",   User.Role.PRODUCT_OWNER), "po123");
        addUser(new User("scrummaster",  "sm123",   User.Role.SCRUM_MASTER),  "sm123");
        addUser(new User("teamMember",   "team123", User.Role.SCRUM_TEAM),    "team123");


        // Persist seeded users to file
        saveUsers();

        // Load any existing users from file (overwrites defaults if file exists)
        loadUsers();
    }

    // ------------------------------------------------------------------ //
    //  Authentication
    // ------------------------------------------------------------------ //

    /**
     * Attempts to authenticate a user.
     */
    public User authenticate(String username, String password) {
        User user = users.get(username);
        if (user == null) {
            return null;
        }
        // Direct comparison without hashing
        if (password != null && password.equals(user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    // ------------------------------------------------------------------ //
    //  Management
    // ------------------------------------------------------------------ //

    /** Add a new user to the store. */
    public void addUser(User user, String plainPassword) {
        // Store the plain password as-is
        User storedUser = new User(user.getUsername(), plainPassword, user.getRole());
        users.put(user.getUsername(), storedUser);

        // Persist to file
        saveUsers();
    }

    /** Returns the User for the given username, or null. */
    public User findUser(String username) {
        return users.get(username);
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    /** Load users from file (overwrites any in-memory users). */
    private void loadUsers() {
        if (!userFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(userFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 3) continue;
                String username = parts[0];
                String password = parts[1]; // plain text
                User.Role role = User.Role.valueOf(parts[2]);
                users.put(username, new User(username, password, role));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read user file", e);
        }
    }

    /** Save all users to file. */
    private void saveUsers() {
        userFile.getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(userFile))) {
            for (User u : users.values()) {
                pw.printf("%s:%s:%s%n", u.getUsername(), u.getPasswordHash(), u.getRole().name());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write user file", e);
        }
    }
}