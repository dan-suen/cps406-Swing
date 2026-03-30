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
    public ScrumProject() {
        this.productBacklog  = new ProductBacklog();
        this.sprintBacklogs  = new ArrayList<>();
        this.userStore       = new UserStore();
        this.currentUser     = null;
    }


    // ------------------------------------------------------------------ //
    //  Authentication
    // ------------------------------------------------------------------ //

    /**
     * Authenticates credentials; sets currentUser on success.
     *
     * @return the authenticated User, or null on failure
     */
    public User login(String username, String password) {
        User user = userStore.authenticate(username, password);
        if (user != null) {
            this.currentUser = user;
        }
        return user;
    }

    /** Clears the current session. */
    public void logout() {
        this.currentUser = null;
    }

    // ------------------------------------------------------------------ //
    //  Sprint management 
    // ------------------------------------------------------------------ //

    /** Creates and registers a new SprintBacklog. */
    public SprintBacklog createSprint(double capacityHours) {
        int sprintNumber = sprintBacklogs.size() + 1;
        SprintBacklog sprint = new SprintBacklog(sprintNumber, capacityHours);
        sprintBacklogs.add(sprint);
        return sprint;
    }

    /** Returns all sprint backlogs in order. */
    public List<SprintBacklog> getAllSprints() {
        return new ArrayList<>(sprintBacklogs);
    }

    /** Returns the most recent (current) sprint, or null. */
    public SprintBacklog getCurrentSprint() {
        if (sprintBacklogs.isEmpty()) return null;
        return sprintBacklogs.get(sprintBacklogs.size() - 1);
    }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //
    public ProductBacklog getProductBacklog() { return productBacklog; }
    public User           getCurrentUser()    { return currentUser; }
    public UserStore      getUserStore()      { return userStore; }
}
