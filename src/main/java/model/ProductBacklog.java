package model;

import java.util.ArrayList;
import java.util.Comparator;
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
    public ProductBacklog() {
        this.items = new ArrayList<>();
    }

    public void addItem(BacklogItem item) {
        if (item != null && !items.contains(item)) {
            item.setStatus(BacklogItem.Status.IN_PRODUCT_BACKLOG);
            items.add(item);
        }
    }

    public void editItem(BacklogItem item,
                         BacklogItem.Priority priority,
                         double timeEstimate,
                         double effortEstimate,
                         double riskLevel) {
        if (item != null && items.contains(item)) {
            item.setPriority(priority);
            item.setTimeEstimate(timeEstimate);
            item.setEffortEstimate(effortEstimate);
            item.setRiskLevel(riskLevel);
        }
    }

    public void removeItem(BacklogItem item) {
        items.remove(item);
    }


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
    public List<BacklogItem> generateSprintProposal(double capacityHours) {
        List<BacklogItem> sorted = getItemsSortedByPriority();
        List<BacklogItem> proposal = new ArrayList<>();
        double totalEffort = 0;

        for (BacklogItem item : sorted) {
            // Only consider items that are still in the product backlog
            if (item.getStatus() != BacklogItem.Status.IN_PRODUCT_BACKLOG
                    && item.getStatus() != BacklogItem.Status.DELAYED) {
                continue;
            }
            double effort = item.getEffortEstimate();
            if (totalEffort + effort <= capacityHours) {
                proposal.add(item);
                totalEffort += effort;
            }
        }
        return proposal;
    }

    // ------------------------------------------------------------------ //
    //  Re-ingestion after sprint end
    // ------------------------------------------------------------------ //

    /**
     * Re-adds an unfinished item from a completed sprint back into the
     * product backlog with its original priority and a revised effort estimate.
     * Req 10
     */
    public void returnUnfinishedItem(BacklogItem item, double revisedEffort) {
        if (item == null) return;
        item.setEffortEstimate(revisedEffort);
        item.setStatus(BacklogItem.Status.DELAYED);
        if (!items.contains(item)) {
            items.add(item);
        }
    }


    // ------------------------------------------------------------------ //
    //  Queries
    // ------------------------------------------------------------------ //

    /** Returns all items sorted by priority (High → Medium → Low). */
    public List<BacklogItem> getItemsSortedByPriority() {
        List<BacklogItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingInt(item -> {
            switch (item.getPriority()) {
                case HIGH:   return 0;
                case MEDIUM: return 1;
                case LOW:    return 2;
                default:     return 3;
            }
        }));
        return sorted;
    }

    /** Returns a copy of the full item list (unsorted). */
    public List<BacklogItem> getAllItems() {
        return new ArrayList<>(items);
    }

    /** Returns the item matching the given title, or null if not found. */
    public BacklogItem findItemByTitle(String title) {
        if (title == null) return null;
        for (BacklogItem item : items) {
            if (title.equals(item.getTitle())) {
                return item;
            }
        }
        return null;
    }
}
