package model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an authenticated user.
 */
public class User {

    public enum Role { PRODUCT_OWNER, SCRUM_MASTER, SCRUM_TEAM }

    // --- Fields ---
    private String username;
    private String passwordHash;  // store hashed, never plaintext
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
//  UserStore  –  simple in-memory credential store  (Req 16)
// ======================================================================

class UserStore {

    // Dictionary: username → User  (as described in sprint doc)
    private Map<String, User> users = new HashMap<>();

    // ------------------------------------------------------------------ //
    //  Constructor – seed with default accounts
    // ------------------------------------------------------------------ //
   public UserStore() {
        // Seed one default account per role so the app can be used immediately
        addUser(new User("productowner", hashPassword("po123"),   User.Role.PRODUCT_OWNER), "po123");
        addUser(new User("scrummaster",  hashPassword("sm123"),   User.Role.SCRUM_MASTER),  "sm123");
        addUser(new User("teamMember",   hashPassword("team123"), User.Role.SCRUM_TEAM),    "team123");
    }

    // ------------------------------------------------------------------ //
    //  Auth
    // ------------------------------------------------------------------ //

    /**
     * Attempts to authenticate a user.
     */
    public User authenticate(String username, String password) {
        User user = users.get(username);
        if (user == null) {
            return null;
        }
        String hashed = hashPassword(password);
        if (hashed != null && hashed.equals(user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    // ------------------------------------------------------------------ //
    //  Management
    // ------------------------------------------------------------------ //

    /** Add a new user to the store. */
    public void addUser(User user, String plainPassword) {
        // If the user was constructed with a pre-hashed password, store as-is;
        // otherwise hash the provided plain password.
        String hash = hashPassword(plainPassword);
        User storedUser = new User(user.getUsername(), hash, user.getRole());
        users.put(user.getUsername(), storedUser);
    }

    /** Returns the User for the given username, or null. */
    public User findUser(String username) {
        return users.get(username);
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    /** Hashes a plaintext password (e.g. SHA-256). */
    private String hashPassword(String plainPassword) {
        if (plainPassword == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JDK spec; this should never happen
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
