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

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    /**
     * Creates an EngineeringTask linked to a parent BacklogItem.
     */
    public EngineeringTask(String title, String description,
                           double effortEstimate, BacklogItem parentItem) { }

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
    public void setEffortEstimate(double effortEstimate) { }

    public void setTitle(String title)           { }
    public void setDescription(String description) { }
    public void setAssignee(String assignee)     { }
    public void setStatus(TaskStatus status)     { }

    /**
     * Re-syncs priority from the parent BacklogItem.
     */
    public void syncPriorityFromParent() { }

    /** Log actual effort spent on this task. */
    public void logActualEffort(double effort) { }
}
