package model;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Top-level application state.
 * Holds the shared ProductBacklog, all SprintBacklogs, and the UserStore.
 * Acts as the single source of truth passed between controllers/views.
 */
public class ScrumProject {

    private static final File DATA_DIR     = new File("data");
    private static final File BACKLOG_FILE = new File(DATA_DIR, "backlog.txt");
    private static final File SPRINTS_FILE = new File(DATA_DIR, "sprints.txt");

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
    /**
     * Registers a new user. Returns false if the username is already taken.
     */
    public boolean registerUser(String username, String password, User.Role role) {
        if (userStore.findUser(username) != null) return false;
        userStore.addUser(new User(username, password, role), password);
        return true;
    }

    public ProductBacklog getProductBacklog() { return productBacklog; }
    public User           getCurrentUser()    { return currentUser; }
    public UserStore      getUserStore()      { return userStore; }

    // ------------------------------------------------------------------ //
    //  Persistence
    // ------------------------------------------------------------------ //

    /** Save backlog and sprints to data/. */
    public void save() {
        DATA_DIR.mkdirs();
        saveBacklog();
        saveSprints();
    }

    /** Load backlog and sprints from data/ (call once on startup). */
    public void load() {
        if (BACKLOG_FILE.exists()) loadBacklog();
        if (SPRINTS_FILE.exists()) loadSprints();
    }

    private void saveBacklog() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(BACKLOG_FILE))) {
            for (BacklogItem item : productBacklog.getAllItems()) {
                pw.println("title:"    + item.getTitle());
                pw.println("desc:"     + item.getDescription());
                pw.println("priority:" + item.getPriority().name());
                pw.println("time:"     + item.getTimeEstimate());
                pw.println("effort:"   + item.getEffortEstimate());
                pw.println("risk:"     + item.getRiskLevel());
                pw.println("status:"   + item.getStatus().name());
                if (item.getAssignee() != null)
                    pw.println("assignee:" + item.getAssignee());
                for (EngineeringTask t : item.getTasks()) {
                    pw.println("task:" + t.getTitle()
                        + "|" + t.getDescription()
                        + "|" + t.getEffortEstimate()
                        + "|" + t.getStatus().name()
                        + "|" + (t.getAssignee() != null ? t.getAssignee() : ""));
                }
                pw.println("===");
            }
        } catch (IOException e) { throw new RuntimeException("Failed to save backlog", e); }
    }

    private void loadBacklog() {
        try (BufferedReader br = new BufferedReader(new FileReader(BACKLOG_FILE))) {
            String line;
            String title = null, desc = null, assignee = null;
            BacklogItem.Priority priority = null;
            BacklogItem.Status   status   = null;
            double time = 0, effort = 0, risk = 0;
            List<String[]> tasks = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                if (line.equals("===")) {
                    if (title != null) {
                        BacklogItem item = new BacklogItem(title, desc, priority, time, effort, risk);
                        productBacklog.addItem(item);
                        item.setStatus(status); // addItem resets to IN_PRODUCT_BACKLOG; restore saved status
                        if (assignee != null) item.setAssignee(assignee);
                        for (String[] t : tasks) {
                            EngineeringTask et = new EngineeringTask(t[0], t[1], Double.parseDouble(t[2]), item);
                            et.setStatus(EngineeringTask.TaskStatus.valueOf(t[3]));
                            if (!t[4].isEmpty()) et.setAssignee(t[4]);
                            item.addTask(et);
                        }
                    }
                    title = desc = assignee = null;
                    priority = null; status = null;
                    time = effort = risk = 0;
                    tasks = new ArrayList<>();
                } else if (line.startsWith("title:"))    title    = line.substring(6);
                  else if (line.startsWith("desc:"))     desc     = line.substring(5);
                  else if (line.startsWith("priority:")) priority = BacklogItem.Priority.valueOf(line.substring(9));
                  else if (line.startsWith("time:"))     time     = Double.parseDouble(line.substring(5));
                  else if (line.startsWith("effort:"))   effort   = Double.parseDouble(line.substring(7));
                  else if (line.startsWith("risk:"))     risk     = Double.parseDouble(line.substring(5));
                  else if (line.startsWith("status:"))   status   = BacklogItem.Status.valueOf(line.substring(7));
                  else if (line.startsWith("assignee:")) assignee = line.substring(9);
                  else if (line.startsWith("task:")) {
                      String[] parts = line.substring(5).split("\\|", 5);
                      if (parts.length == 5) tasks.add(parts);
                  }
            }
        } catch (IOException e) { throw new RuntimeException("Failed to load backlog", e); }
    }

    private void saveSprints() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SPRINTS_FILE))) {
            for (SprintBacklog s : sprintBacklogs) {
                pw.println("number:"         + s.getSprintNumber());
                pw.println("capacity:"       + s.getCapacityHours());
                pw.println("state:"          + s.getState().name());
                pw.println("approved:"       + s.isApproved());
                pw.println("plannedEffort:"  + s.getTotalPlannedEffort());
                pw.println("completedEffort:"+ s.getTotalCompletedEffort());

                StringJoiner proposed = new StringJoiner(",");
                for (BacklogItem i : s.getProposedItems())  proposed.add(i.getTitle());
                pw.println("proposed:" + proposed);

                StringJoiner committed = new StringJoiner(",");
                for (BacklogItem i : s.getCommittedItems()) committed.add(i.getTitle());
                pw.println("committed:" + committed);

                StringJoiner bd = new StringJoiner(",");
                for (Map.Entry<Integer, Double> e : s.getBurndownData().entrySet())
                    bd.add(e.getKey() + "=" + e.getValue());
                pw.println("burndown:" + bd);
                if (s.getStartDate() != null)
                    pw.println("startDate:" + s.getStartDate());
                pw.println("===");
            }
        } catch (IOException e) { throw new RuntimeException("Failed to save sprints", e); }
    }

    private void loadSprints() {
        try (BufferedReader br = new BufferedReader(new FileReader(SPRINTS_FILE))) {
            String line;
            int number = -1;
            double capacity = 0, plannedEffort = 0, completedEffort = 0;
            SprintBacklog.SprintState state = null;
            boolean approved = false;
            String proposedStr = "", committedStr = "", burndownStr = "";
            LocalDate startDate = null;

            while ((line = br.readLine()) != null) {
                if (line.equals("===")) {
                    if (number > 0 && state != null) {
                        SprintBacklog s = new SprintBacklog(number, capacity);

                        if (!proposedStr.isEmpty()) {
                            List<BacklogItem> proposed = new ArrayList<>();
                            for (String t : proposedStr.split(",")) {
                                BacklogItem item = productBacklog.findItemByTitle(t.trim());
                                if (item != null) proposed.add(item);
                            }
                            if (!proposed.isEmpty()) s.setProposedItems(proposed);
                        }

                        if (!committedStr.isEmpty()) {
                            List<BacklogItem> committed = new ArrayList<>();
                            for (String t : committedStr.split(",")) {
                                BacklogItem item = productBacklog.findItemByTitle(t.trim());
                                if (item != null) committed.add(item);
                            }
                            s.restoreCommittedItems(committed);
                        }

                        if (!burndownStr.isEmpty()) {
                            Map<Integer, Double> bd = new HashMap<>();
                            for (String entry : burndownStr.split(",")) {
                                String[] kv = entry.split("=");
                                if (kv.length == 2)
                                    bd.put(Integer.parseInt(kv[0]), Double.parseDouble(kv[1]));
                            }
                            s.restoreBurndown(bd);
                        }

                        s.restoreState(state, approved, plannedEffort, completedEffort, startDate);
                        sprintBacklogs.add(s);
                    }
                    number = -1; capacity = plannedEffort = completedEffort = 0;
                    state = null; approved = false; startDate = null;
                    proposedStr = committedStr = burndownStr = "";
                } else if (line.startsWith("number:"))          number          = Integer.parseInt(line.substring(7));
                  else if (line.startsWith("capacity:"))        capacity        = Double.parseDouble(line.substring(9));
                  else if (line.startsWith("state:"))           state           = SprintBacklog.SprintState.valueOf(line.substring(6));
                  else if (line.startsWith("approved:"))        approved        = Boolean.parseBoolean(line.substring(9));
                  else if (line.startsWith("plannedEffort:"))   plannedEffort   = Double.parseDouble(line.substring(14));
                  else if (line.startsWith("completedEffort:")) completedEffort = Double.parseDouble(line.substring(16));
                  else if (line.startsWith("proposed:"))        proposedStr     = line.substring(9);
                  else if (line.startsWith("committed:"))       committedStr    = line.substring(10);
                  else if (line.startsWith("burndown:"))        burndownStr     = line.substring(9);
                  else if (line.startsWith("startDate:"))       startDate       = LocalDate.parse(line.substring(10));
            }
        } catch (IOException e) { throw new RuntimeException("Failed to load sprints", e); }
    }
}
