package model;

import java.util.List;

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

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //
    public SprintBacklog(int sprintNumber, double capacityHours) { }

    // ------------------------------------------------------------------ //
    //  Proposal phase 
    // ------------------------------------------------------------------ //

    /** Load a generated proposal into the sprint (before approval). */
    public void setProposedItems(List<BacklogItem> items) { }

    /** Add an item to the proposal (Scrum Master edit).*/
    public void addProposedItem(BacklogItem item) { }

    /** Remove an item from the proposal (Scrum Master edit).*/
    public void removeProposedItem(BacklogItem item) { }

    /** Returns the current proposed list. */
    public List<BacklogItem> getProposedItems() { return null; }

    // ------------------------------------------------------------------ //
    //  Approval
    // ------------------------------------------------------------------ //

    /**
     * Product Owner approves the proposed list; items are locked and
     * sprint transitions to ACTIVE.
     */
    public void approve() { }

    /**
     * Product Owner rejects the proposed list; sprint returns to PROPOSED
     * state so Scrum Master can revise.
     */
    public void reject() { }

    // ------------------------------------------------------------------ //
    //  Sprint lifecycle
    // ------------------------------------------------------------------ //

    /** Start the sprint; locks all committed items.*/
    public void startSprint() { }

    /**
     * End the sprint; returns unfinished items to the product backlog.  Req 10
     *
     * @param productBacklog  the shared product backlog to receive returned items
     */
    public void endSprint(ProductBacklog productBacklog) { }

    // ------------------------------------------------------------------ //
    //  Progress
    // ------------------------------------------------------------------ //

    /**
     * Record remaining effort for the current day (burndown snapshot).  Req 15
     *
     * @param day             sprint day index (1-based)
     * @param remainingEffort remaining effort in hours
     */
    public void recordBurndown(int day, double remainingEffort) { }

    /** Returns the burndown data map (day → remaining effort). */
    public java.util.Map<Integer, Double> getBurndownData() { return null; }

    /** Calculates completed effort for velocity reporting.*/
    public double calculateVelocity() { return 0; }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //
    public int              getSprintNumber()     { return sprintNumber; }
    public SprintState      getState()            { return state; }
    public boolean          isApproved()          { return approved; }
    public double           getCapacityHours()    { return capacityHours; }
    public List<BacklogItem> getCommittedItems()  { return null; }
    public double           getTotalPlannedEffort()    { return totalPlannedEffort; }
    public double           getTotalCompletedEffort()  { return totalCompletedEffort; }
}
