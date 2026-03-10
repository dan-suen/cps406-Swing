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
    public User(String username, String passwordHash, Role role) { }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //
    public String getUsername()     { return username; }
    public Role   getRole()         { return role; }
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
    public UserStore() { }

    // ------------------------------------------------------------------ //
    //  Auth
    // ------------------------------------------------------------------ //

    /**
     * Attempts to authenticate a user.
     *
     * @param username  the entered username
     * @param password  the entered plaintext password
     * @return the matching User, or null on failure
     */
    public User authenticate(String username, String password) { return null; }

    // ------------------------------------------------------------------ //
    //  Management
    // ------------------------------------------------------------------ //

    /** Add a new user to the store. */
    public void addUser(User user, String plainPassword) { }

    /** Returns the User for the given username, or null. */
    public User findUser(String username) { return null; }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    /** Hashes a plaintext password (e.g. SHA-256). */
    private String hashPassword(String plainPassword) { return null; }
}
