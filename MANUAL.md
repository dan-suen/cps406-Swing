# User Manual — Scrum Backlog Tool

---

## Logging In

Enter your username and password on the login screen and click **Login**.
Click **Register** to create a new account — choose a username, password, and role.

Default accounts are listed in the README.

---

## Scrum Master

### Product Backlog tab

| Action | How |
|---|---|
| Add a backlog item | Click **Add Item** — fill in title, description, priority, time estimate, effort estimate, and risk |
| Edit an item | Select a row, click **Edit Item** |
| Remove an item | Select a row, click **Remove Item** — confirms before deleting |
| Preview a proposal | Click **Generate Sprint Proposal…** — shows which items fit in a given capacity without committing them |

### Sprint tab

| Action | How |
|---|---|
| Generate a sprint proposal | Click **Auto-Generate Proposal…** — enter capacity in hours; creates a sprint if none exists, then selects the highest-priority items that fit |
| Submit proposal to Product Owner | Click **Submit for Approval** — writes a proposal file to `proposals/`; the PO is notified on next login |
| End the sprint | Click **End Sprint** — confirms first; incomplete items are returned to the product backlog as DELAYED |

### Velocity & Burndown tab

- **Sprint Summary** table shows all sprints with planned effort vs completed effort (velocity)
- **Burndown chart** updates automatically each time the tab is opened — shows remaining effort over days elapsed since the sprint started
- The dashed blue line is the ideal straight-line burndown; the red line is actual

---

## Product Owner

### Proposals menu (top menu bar)

| Action | How |
|---|---|
| Review pending proposals | Click **Proposals → View Pending Proposals** |
| Approve a proposal | Click **Approve** in the proposal dialog — the sprint becomes active immediately |
| Reject a proposal | Click **Reject** — the sprint returns to PROPOSED state; the Scrum Master can revise and resubmit |
| Skip for now | Click **Skip** — the proposal file remains; it will appear again next time |

On login, a notification appears if any proposals are waiting.

### Velocity & Burndown tab

Same view as Scrum Master — read only.

---

## Scrum Team

### Sprint tab

| Action | How |
|---|---|
| Take on an item | Select a committed item, click **Take On Item** — assigns it to your username |
| Mark an item complete | Select a committed item, click **Mark Complete** |
| Log effort on an item | Select a committed item, click **Log Effort…** — enter hours; accumulates across multiple entries |
| Manage sub-tasks for an item | Select a committed item, click **My Tasks for Item…** — opens the task dialog |

### Task dialog (My Tasks for Item…)

| Action | How |
|---|---|
| Add a sub-task | Click **Add Task** — enter a title and effort estimate |
| Update a sub-task's status | Select a task, click **Update Status…** — choose TODO, IN_PROGRESS, or DONE |
| Remove a sub-task | Select a task, click **Remove Task** |

### My Tasks tab

Shows all engineering sub-tasks assigned to you across all committed sprint items in one place.
Use **Update Status…** to update any task's status without opening each item individually.

---

## All Users

- **Logout** — top menu bar → **File → Logout** — returns to the login screen
- **Register** — available on the login screen; any role can be selected
- All data (backlog, sprints, tasks) is saved automatically after every change
