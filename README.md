
## Project Overview

A Java Swing desktop app implementing a Scrum backlog management tool for CPS406 Group 65.

### Architecture

```
src/main/java/
├── Main.java                  # Entry point — launches the Swing GUI
└── model/
    ├── ScrumProject.java      # Top-level state: owns the backlog, sprints, and users
    ├── ProductBacklog.java    # Single shared backlog; generates sprint proposals
    ├── SprintBacklog.java     # One sprint: proposal → approval → active → complete
    ├── BacklogItem.java       # A single user story (priority, effort, status, tasks)
    ├── EngineeringTask.java   # Sub-task belonging to a BacklogItem
    └── User.java              # Authenticated user with role (PO / SM / Team)

src/test/java/
└── model/
    └── ScrumBacklogTest.java  # JUnit 5 test suite covering all non-GUI requirements
```

### Key concepts

**`ScrumProject`** is the root object. It owns the product backlog, all sprints, and the user store. The GUI passes this object around rather than holding state directly.

**Backlog item lifecycle:**
`IN_PRODUCT_BACKLOG` → `IN_SPRINT` (sprint approved) → `COMPLETE` or `DELAYED` (sprint ends)
Items marked `DELAYED` are returned to the product backlog with a revised effort estimate.

**Sprint lifecycle:**
`PROPOSED` → `PENDING_APPROVAL` (items added) → `ACTIVE` (Product Owner approves) → `COMPLETE` (sprint ended)

**User roles:** `PRODUCT_OWNER`, `SCRUM_MASTER`, `SCRUM_TEAM`

### Data storage

User accounts are persisted to **`data.txt`** in the project root, one user per line in the format `username:password:ROLE`. The file is read on startup and written whenever a user is added. Backlog and sprint data are held in memory only — they are not saved between sessions.

---

## Running Tests

**From the terminal:**
```bash
mvn test
```

**Run a single test class:**
```bash
mvn test -Dtest=ScrumBacklogTest
```

**Run a single test method:**
```bash
mvn test -Dtest=ScrumBacklogTest#req10_tc_returnUnfinishedItems_toProductBacklog
```

**In VS Code:**
- Open any test file and click the **Run Test** / **Debug Test** button that appears above each `@Test` method
- Or open the **Testing** panel (beaker icon in the sidebar) to run all tests at once
