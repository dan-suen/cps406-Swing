package model;

/**
 * A lower-level engineering task that belongs to a BacklogItem.
 */
public class EngineeringTask {

    public enum TaskStatus { TODO, IN_PROGRESS, DONE }

    // --- Fields ---
    private String              taskId;
    private String              title;
    private String              description;
    private BacklogItem.Priority priority;  
    private double              effortEstimate; 
    private double              actualEffort;
    private TaskStatus          status;
    private BacklogItem         parentItem; 
    private String              assignee;

    // Static counter for generating unique IDs
    private static int idCounter = 1;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    /**
     * Creates an EngineeringTask linked to a parent BacklogItem.
     */
     public EngineeringTask(String title, String description,
                           double effortEstimate, BacklogItem parentItem) {
        this.taskId         = "ET-" + (idCounter++);
        this.title          = title;
        this.description    = description;
        this.effortEstimate = effortEstimate;
        this.parentItem     = parentItem;
        this.actualEffort   = 0;
        this.status         = TaskStatus.TODO;
        // Inherit priority from parent (Req 12)
        this.priority       = (parentItem != null) ? parentItem.getPriority() : null;
    }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //
    public String               getTaskId()        { return taskId; }
    public String               getTitle()         { return title; }
    public String               getDescription()   { return description; }
    public BacklogItem.Priority getPriority()      { return priority; }
    public double               getEffortEstimate(){ return effortEstimate; }
    public double               getActualEffort()  { return actualEffort; }
    public TaskStatus           getStatus()        { return status; }
    public BacklogItem          getParentItem()    { return parentItem; }
    public String               getAssignee()      { return assignee; }

    // ------------------------------------------------------------------ //
    //  Setters
    // ------------------------------------------------------------------ //

    /** Update the independent effort estimate for this task.  Req 13 */
    public void setEffortEstimate(double effortEstimate) {
        this.effortEstimate = effortEstimate;
    }

    public void setTitle(String title)             { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setAssignee(String assignee)       { this.assignee = assignee; }
    public void setStatus(TaskStatus status)       { this.status = status; }

    /**
     * Re-syncs priority from the parent BacklogItem.  Req 12
     */
    public void syncPriorityFromParent() {
        if (parentItem != null) {
            this.priority = parentItem.getPriority();
        }
    }

    /** Log actual effort spent on this task.  Req 14 */
    public void logActualEffort(double effort) {
        this.actualEffort += effort;
    }
}
