package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single Sprint Backlog.
 */
public class SprintBacklog {

    public enum SprintState { PROPOSED, PENDING_APPROVAL, ACTIVE, COMPLETE }

    // --- Fields ---
    private int              sprintNumber;
    private List<BacklogItem> proposedItems;   // mutable until approved
    private List<BacklogItem> committedItems;  // locked once sprint is active
    private SprintState      state;
    private double           capacityHours;
    private boolean          approved;       

    // Velocity tracking 
    private double totalPlannedEffort;
    private double totalCompletedEffort;

    // Burndown: map of day -> remaining effort
    private java.util.Map<Integer, Double> burndownData;

    // Date the sprint was started
    private LocalDate startDate;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //
    public SprintBacklog(int sprintNumber, double capacityHours) {
        this.sprintNumber       = sprintNumber;
        this.capacityHours      = capacityHours;
        this.proposedItems      = new ArrayList<>();
        this.committedItems     = new ArrayList<>();
        this.state              = SprintState.PROPOSED;
        this.approved           = false;
        this.totalPlannedEffort    = 0;
        this.totalCompletedEffort  = 0;
        this.burndownData          = new HashMap<>();
    }

    // ------------------------------------------------------------------ //
    //  Proposal phase 
    // ------------------------------------------------------------------ //

    /** Load a generated proposal into the sprint (before approval). */
    public void setProposedItems(List<BacklogItem> items) {
        if (state == SprintState.PROPOSED || state == SprintState.PENDING_APPROVAL) {
            this.proposedItems = new ArrayList<>(items);
            this.state = SprintState.PENDING_APPROVAL;
        }
    }

    /** Add an item to the proposal (Scrum Master edit). */
    public void addProposedItem(BacklogItem item) {
        if (item != null && !proposedItems.contains(item)
                && (state == SprintState.PROPOSED || state == SprintState.PENDING_APPROVAL)) {
            proposedItems.add(item);
            if (state == SprintState.PROPOSED) {
                state = SprintState.PENDING_APPROVAL;
            }
        }
    }

    /** Remove an item from the proposal (Scrum Master edit). */
    public void removeProposedItem(BacklogItem item) {
        if (state == SprintState.PROPOSED || state == SprintState.PENDING_APPROVAL) {
            proposedItems.remove(item);
        }
    }

    /** Returns the current proposed list. */
    public List<BacklogItem> getProposedItems() {
        return new ArrayList<>(proposedItems);
    }


    // ------------------------------------------------------------------ //
    //  Approval
    // ------------------------------------------------------------------ //

    /**
     * Product Owner approves the proposed list; items are locked and
     * sprint transitions to ACTIVE.
     */
    public void approve() {
        if (state == SprintState.PENDING_APPROVAL) {
            this.approved       = true;
            this.committedItems = new ArrayList<>(proposedItems);
            // Mark all committed items as IN_SPRINT
            for (BacklogItem item : committedItems) {
                item.setStatus(BacklogItem.Status.IN_SPRINT);
            }
            // Calculate planned effort
            totalPlannedEffort = 0;
            for (BacklogItem item : committedItems) {
                totalPlannedEffort += item.getEffortEstimate();
            }
            this.state = SprintState.ACTIVE;
        }
    }

    /**
     * Product Owner rejects the proposed list; sprint returns to PROPOSED
     * state so Scrum Master can revise.
     */
    public void reject() {
        if (state == SprintState.PENDING_APPROVAL) {
            this.approved = false;
            this.state    = SprintState.PROPOSED;
        }
    }

    // ------------------------------------------------------------------ //
    //  Sprint lifecycle
    // ------------------------------------------------------------------ //

    /** Start the sprint; locks all committed items.*/
    public void startSprint() {
        if (approved && state == SprintState.ACTIVE) {
            startDate = LocalDate.now();
            burndownData.put(0, totalPlannedEffort);
        }
    }

    /**
     * End the sprint; returns unfinished items to the product backlog.  Req 10s
     */
    public void endSprint(ProductBacklog productBacklog) {
        if (state != SprintState.ACTIVE) return;

        totalCompletedEffort = 0;
        for (BacklogItem item : committedItems) {
            if (item.getStatus() == BacklogItem.Status.COMPLETE) {
                totalCompletedEffort += item.getEffortEstimate();
            } else {
                // Unfinished: re-insert into product backlog (returnUnfinishedItem sets DELAYED)
                // Use actual effort as the revised estimate if available, else keep original
                double revisedEffort = item.getActualEffort() > 0
                        ? item.getEffortEstimate() - item.getActualEffort()
                        : item.getEffortEstimate();
                if (revisedEffort < 0) revisedEffort = 0;
                productBacklog.returnUnfinishedItem(item, revisedEffort);
            }
        }
        this.state = SprintState.COMPLETE;
    }

    // ------------------------------------------------------------------ //
    //  Progress
    // ------------------------------------------------------------------ //

    /** Returns how many days have elapsed since the sprint started, or -1 if not started. */
    public int getCurrentDay() {
        if (startDate == null) return -1;
        return (int) ChronoUnit.DAYS.between(startDate, LocalDate.now());
    }

    /** Calculates remaining effort as planned minus completed effort. */
    public double calculateRemainingEffort() {
        double completed = 0;
        for (BacklogItem item : committedItems) {
            if (item.getStatus() == BacklogItem.Status.COMPLETE)
                completed += item.getEffortEstimate();
        }
        return Math.max(0, totalPlannedEffort - completed);
    }

    /** Record remaining effort for a given day (burndown snapshot). */
    public void recordBurndown(int day, double remainingEffort) {
        burndownData.put(day, remainingEffort);
    }

    /** Returns the burndown data map (day → remaining effort). */
    public Map<Integer, Double> getBurndownData() {
        return new HashMap<>(burndownData);
    }

    /** Calculates completed effort for velocity reporting. */
    public double calculateVelocity() {
        totalCompletedEffort = 0;
        for (BacklogItem item : committedItems) {
            if (item.getStatus() == BacklogItem.Status.COMPLETE) {
                totalCompletedEffort += item.getEffortEstimate();
            }
        }
        return totalCompletedEffort;
    }

    // ------------------------------------------------------------------ //
    //  Persistence helpers (package-private, used by ScrumProject only)
    // ------------------------------------------------------------------ //
    void restoreState(SprintState s, boolean app, double planned, double completed, LocalDate start) {
        this.state = s;
        this.approved = app;
        this.totalPlannedEffort = planned;
        this.totalCompletedEffort = completed;
        this.startDate = start;
    }
    void restoreCommittedItems(List<BacklogItem> items) {
        this.committedItems = new ArrayList<>(items);
    }
    void restoreBurndown(Map<Integer, Double> data) {
        this.burndownData.putAll(data);
    }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //
    public int              getSprintNumber()     { return sprintNumber; }
    public LocalDate        getStartDate()        { return startDate; }
    public SprintState      getState()            { return state; }
    public boolean          isApproved()          { return approved; }
    public double           getCapacityHours()    { return capacityHours; }
    public List<BacklogItem> getCommittedItems()  { return new ArrayList<>(committedItems); }
    public double           getTotalPlannedEffort()    { return totalPlannedEffort; }
    public double           getTotalCompletedEffort()  { return totalCompletedEffort; }
}
