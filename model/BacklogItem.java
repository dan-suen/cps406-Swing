package model;

/**
 * Represents a single item in the Product Backlog or Sprint Backlog.
 */
public class BacklogItem {

    public enum Priority { HIGH, MEDIUM, LOW }
    public enum Status   { IN_PRODUCT_BACKLOG, IN_SPRINT, COMPLETE, DELAYED }

    //Fields 
    private String   title;
    private String   description;
    private Priority priority;
    private double   timeEstimate;    // hours
    private double   effortEstimate;  // story points or hours
    private double   riskLevel;       // optional; 0 = none
    private Status   status;

    // Logging
    private double actualTime;
    private double actualEffort;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //
    public BacklogItem(String title, String description,
                       Priority priority, double timeEstimate,
                       double effortEstimate, double riskLevel) { }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //
    public String   getTitle()          { return title; }
    public String   getDescription()    { return description; }
    public Priority getPriority()       { return priority; }
    public double   getTimeEstimate()   { return timeEstimate; }
    public double   getEffortEstimate() { return effortEstimate; }
    public double   getRiskLevel()      { return riskLevel; }
    public Status   getStatus()         { return status; }
    public double   getActualTime()     { return actualTime; }
    public double   getActualEffort()   { return actualEffort; }

    // ------------------------------------------------------------------ //
    //  Setters
    // ------------------------------------------------------------------ //
    public void setPriority(Priority priority)           { }
    public void setTimeEstimate(double timeEstimate)     { }
    public void setEffortEstimate(double effortEstimate) { }
    public void setRiskLevel(double riskLevel)           { }
    public void setStatus(Status status)                 { }
}
