package model;

import java.util.List;

/**
 * The single shared Product Backlog.
 */
public class ProductBacklog {

    // --- Fields ---
    private List<BacklogItem> items;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //
    public ProductBacklog() { }

    public void addItem(BacklogItem item) { }


    public void editItem(BacklogItem item,
                         BacklogItem.Priority priority,
                         double timeEstimate,
                         double effortEstimate,
                         double riskLevel) { }


    public void removeItem(BacklogItem item) { }

    // ------------------------------------------------------------------ //
    //  Sprint proposal generation  (Req 6)
    // ------------------------------------------------------------------ //

    /**
     * Returns the highest-priority items whose total effort fits within
     * the given sprint capacity.
     *
     * @param capacityHours  the team's sprint capacity in hours
     * @return ordered list of proposed BacklogItems
     */
    public List<BacklogItem> generateSprintProposal(double capacityHours) { return null; }

    // ------------------------------------------------------------------ //
    //  Re-ingestion after sprint end
    // ------------------------------------------------------------------ //

    /**
     * Re-adds an unfinished item from a completed sprint back into the
     * product backlog with its original priority and a revised effort estimate.
     * Req 10
     */
    public void returnUnfinishedItem(BacklogItem item, double revisedEffort) { }

    // ------------------------------------------------------------------ //
    //  Queries
    // ------------------------------------------------------------------ //

    /** Returns all items sorted by priority (High → Medium → Low). */
    public List<BacklogItem> getItemsSortedByPriority() { return null; }

    /** Returns a copy of the full item list (unsorted). */
    public List<BacklogItem> getAllItems() { return null; }

    /** Returns the item matching the given title, or null if not found. */
    public BacklogItem findItemByTitle(String title) { return null; }
}
