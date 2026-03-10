package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level application state.
 * Holds the shared ProductBacklog, all SprintBacklogs, and the UserStore.
 * Acts as the single source of truth passed between controllers/views.
 */
public class ScrumProject {

    // --- Fields ---
    private ProductBacklog      productBacklog;   // single shared backlog
    private List<SprintBacklog> sprintBacklogs;   // multiple sprints 
    private UserStore           userStore;        // credential store
    private User                currentUser;      // currently logged-in user

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //
    public ScrumProject() { }

    // ------------------------------------------------------------------ //
    //  Authentication
    // ------------------------------------------------------------------ //

    /**
     * Authenticates credentials; sets currentUser on success.
     *
     * @return the authenticated User, or null on failure
     */
    public User login(String username, String password) { return null; }

    /** Clears the current session. */
    public void logout() { }

    // ------------------------------------------------------------------ //
    //  Sprint management 
    // ------------------------------------------------------------------ //

    /** Creates and registers a new SprintBacklog. */
    public SprintBacklog createSprint(double capacityHours) { return null; }

    /** Returns all sprint backlogs in order. */
    public List<SprintBacklog> getAllSprints() { return null; }

    /** Returns the most recent (current) sprint, or null. */
    public SprintBacklog getCurrentSprint() { return null; }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //
    public ProductBacklog getProductBacklog() { return productBacklog; }
    public User           getCurrentUser()    { return currentUser; }
    public UserStore      getUserStore()      { return userStore; }
}
