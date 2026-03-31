
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

### Default accounts

| Username | Password | Role |
|---|---|---|
| `productowner` | `po123` | PRODUCT_OWNER |
| `scrummaster` | `sm123` | SCRUM_MASTER |
| `teamMember` | `team123` | SCRUM_TEAM |

These are seeded automatically on startup and persisted to `data/users.txt`.

### Data storage

All persistent data is stored in the **`data/`** folder in the project root:

| File | Contents |
|---|---|
| `data/users.txt` | User accounts — one per line: `username:password:ROLE` |
| `data/backlog.txt` | Product backlog items, their fields, assignees, and engineering sub-tasks |
| `data/sprints.txt` | Sprint history — state, capacity, proposed/committed item titles, burndown entries |

The `data/` folder is created automatically on first run. All three files are written whenever data changes and read back in full on startup. Proposal notification files remain in `proposals/` until the Product Owner acts on them.

---

## Running the Application

**From the terminal:**
```bash
mvn compile exec:java -Dexec.mainClass=Main
```

**From VS Code:**
- Open `src/main/java/Main.java` and click the **Run** button above `main()`
- Or run the **Launch Scrum Tool** configuration from the Run & Debug panel if configured

**First run:** The `data/` and `proposals/` folders are created automatically. The app starts with an empty product backlog. Log in with one of the default accounts above to get started.

**Typical workflow to exercise all features:**
1. Log in as `scrummaster` — add several backlog items via the Product Backlog tab
2. Log in as `scrummaster` — go to Sprint tab, click Auto-Generate Proposal, then Submit for Approval
3. Log in as `productowner` — go to Proposals → View Pending Proposals, approve the sprint
4. Log in as `teamMember` — go to Sprint tab, take on a committed item, add sub-tasks via My Tasks for Item
5. Log in as `teamMember` — mark items complete, log effort
6. Log in as `scrummaster` — end the sprint, check Velocity & Burndown tab for history

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
