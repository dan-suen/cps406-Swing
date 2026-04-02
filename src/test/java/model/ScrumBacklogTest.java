package model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import static org.junit.jupiter.api.Assertions.*;

import model.BacklogItem;
import model.BacklogItem.Priority;
import model.BacklogItem.Status;

import java.util.List;

/**
 * ============================================================
 * ScrumBacklogTest.java
 * JUnit 5 — Exhaustive non-GUI test suite
 * CPS406 Group 65 — Sprint 1
 * ============================================================
 *
 * Every test is labeled with:
 *   [REQ-N]  — requirement number from the SRS (1–20)
 *   [TC:ID]  — test-case ID from the Sprint 1 test-plan table
 *
 * Requirements covered (all non-GUI):
 *   REQ-1   Single shared Product Backlog
 *   REQ-2   Multiple Sprint Backlogs supported
 *   REQ-3   BacklogItem fields: title, description, priority, time, effort, risk
 *   REQ-4   Add BacklogItem to Product Backlog
 *   REQ-5   Modify BacklogItem (priority, time, effort, risk)
 *   REQ-6   Auto-generate highest-priority items fitting sprint capacity
 *   REQ-7   Scrum Master may modify proposed Sprint list before finalisation
 *   REQ-8   Product Owner approval / rejection of proposed Sprint list
 *   REQ-9   Sprint Backlog items locked once sprint begins
 *   REQ-10  Unfinished items returned to Product Backlog at sprint end
 *   REQ-11  Expand Sprint items into EngineeringTasks
 *   REQ-12  EngineeringTasks inherit parent priority; propagate on change
 *   REQ-13  EngineeringTasks have independent effort estimates
 *   REQ-14  Log actual time / effort on Sprint items
 *   REQ-15  Burndown / Velocity chart data (model layer)
 *   REQ-16  User authentication (login / credential validation)
 *   REQ-17  Role-based routing after login
 *
 * GUI-only requirements intentionally excluded:
 *   REQ-18  Product Owner UI panel  — manual test
 *   REQ-19  Scrum Master UI panel   — manual test
 *   REQ-20  Scrum Team UI panel     — manual test
 *
 * NOTE ON MODEL API ALIGNMENT
 * ===========================
 * These tests are written against the actual model classes as implemented:
 *
 *   BacklogItem(title, desc, priority, timeEst, effortEst, riskLevel)
 *     — 6-arg constructor; pass riskLevel=0 when unused.
 *     — No auto-generated id; items looked up by object reference or title.
 *
 *   ProductBacklog
 *     — addItem(), removeItem(BacklogItem), editItem(), findItemByTitle()
 *     — getAllItems(), getItemsSortedByPriority(), generateSprintProposal(capacity)
 *
 *   SprintBacklog(sprintNumber, capacityHours) — created via ScrumProject.createSprint()
 *     — setProposedItems(List), addProposedItem(), removeProposedItem()
 *     — approve(), reject(), startSprint(), endSprint(ProductBacklog)
 *     — recordBurndown(), getBurndownData(), calculateVelocity()
 *     — getState(), isApproved(), getCapacityHours()
 *
 *   EngineeringTask(title, description, effortEstimate, parentItem)
 *     — priority inherited from parent; syncPriorityFromParent(), logActualEffort()
 *
 *   ScrumProject — login(username, password) returns User or null
 *   User.Role    — PRODUCT_OWNER, SCRUM_MASTER, SCRUM_TEAM
 *
 * DEFAULT CREDENTIALS (seeded in UserStore):
 *   productowner / po123   → PRODUCT_OWNER
 *   scrummaster  / sm123   → SCRUM_MASTER
 *   teamMember   / team123 → SCRUM_TEAM
 */
@TestMethodOrder(OrderAnnotation.class)
public class ScrumBacklogTest {

    // ----------------------------------------------------------------
    // Fixtures — recreated fresh before every test
    // ----------------------------------------------------------------

    private model.ProductBacklog productBacklog;
    private model.SprintBacklog  sprintBacklog;
    private model.ScrumProject   project;

    @BeforeEach
    void setUp() {
        project        = new model.ScrumProject();
        productBacklog = project.getProductBacklog();
        // SprintBacklog(sprintNumber=1, capacityHours=20)
        sprintBacklog  = project.createSprint(20);
    }


    // ================================================================
    // SECTION 1 — DATA / STRUCTURE  (REQ-1, REQ-2, REQ-3)
    // ================================================================

    // ----------------------------------------------------------------
    // REQ-1 : Single shared Product Backlog
    // ----------------------------------------------------------------

    /**
     * [REQ-1]
     * The system shall maintain exactly one shared Product Backlog.
     */
    @Test @Order(10)
    void req1_singleSharedProductBacklogExists() {
        assertNotNull(productBacklog,
            "REQ-1: A ProductBacklog instance must be obtainable.");
    }

    /**
     * [REQ-1]
     * Items added to the shared backlog must persist on the same object reference.
     */
    @Test @Order(11)
    void req1_productBacklogRetainsAddedItems() {
        productBacklog.addItem(new BacklogItem("Story A", "", Priority.HIGH,   3, 3, 0));
        productBacklog.addItem(new BacklogItem("Story B", "", Priority.LOW,    2, 2, 0));
        assertEquals(2, productBacklog.getAllItems().size(),
            "REQ-1: Both items must be present in the single shared Product Backlog.");
    }

    // ----------------------------------------------------------------
    // REQ-2 : Multiple Sprint Backlogs
    // ----------------------------------------------------------------

    /**
     * [REQ-2]
     * The system must support creating more than one Sprint Backlog.
     */
    @Test @Order(20)
    void req2_multipleSprintBacklogsCanBeCreated() {
        model.SprintBacklog sprint1 = project.createSprint(20);
        model.SprintBacklog sprint2 = project.createSprint(15);
        assertNotSame(sprint1, sprint2,
            "REQ-2: Sprint Backlogs must be independent objects.");
        assertEquals(20, sprint1.getCapacityHours(), 0.001);
        assertEquals(15, sprint2.getCapacityHours(), 0.001);
    }

    /**
     * [REQ-2]
     * Items approved into one sprint must not appear in another sprint's proposal.
     */
    @Test @Order(21)
    void req2_sprintBacklogsAreIndependent() {
        BacklogItem item = new BacklogItem("Isolated story", "", Priority.HIGH, 5, 5, 0);
        productBacklog.addItem(item);

        model.SprintBacklog sprint1 = project.createSprint(20);
        model.SprintBacklog sprint2 = project.createSprint(20);

        sprint1.setProposedItems(List.of(item));
        sprint1.approve();

        assertEquals(Status.IN_SPRINT, item.getStatus(),
            "REQ-2: Item should be IN_SPRINT after sprint1 approval.");
        assertTrue(sprint2.getProposedItems().isEmpty(),
            "REQ-2: Item must not appear in sprint2's proposed list — backlogs are independent.");
    }

    // ----------------------------------------------------------------
    // REQ-3 : BacklogItem fields
    // ----------------------------------------------------------------

    /**
     * [REQ-3]
     * BacklogItem must expose title, description, priority, time estimate, effort estimate.
     */
    @Test @Order(30)
    void req3_backlogItemHasAllRequiredFields() {
        BacklogItem item = new BacklogItem(
            "Login screen",
            "Build Swing login form",
            Priority.HIGH,
            5,   // time estimate
            3,   // effort estimate
            0    // risk level
        );
        assertEquals("Login screen",          item.getTitle());
        assertEquals("Build Swing login form", item.getDescription());
        assertEquals(Priority.HIGH,            item.getPriority());
        assertEquals(5, item.getTimeEstimate(),   0.001);
        assertEquals(3, item.getEffortEstimate(), 0.001);
    }

    /**
     * [REQ-3]
     * Risk level must default to zero when passed as 0.
     */
    @Test @Order(31)
    void req3_riskLevelDefaultsToZero() {
        BacklogItem item = new BacklogItem("Story", "desc", Priority.MEDIUM, 4, 4, 0);
        assertEquals(0, item.getRiskLevel(), 0.001);
    }

    /**
     * [REQ-3]
     * Risk level can be set after construction.
     */
    @Test @Order(32)
    void req3_riskLevelCanBeSet() {
        BacklogItem item = new BacklogItem("Story", "desc", Priority.LOW, 2, 2, 0);
        item.setRiskLevel(1);
        assertEquals(1, item.getRiskLevel(), 0.001);
    }

    /**
     * [REQ-3]
     * All three priority levels (HIGH, MEDIUM, LOW) must be valid enum values.
     */
    @Test @Order(33)
    void req3_allThreePriorityLevelsExist() {
        assertDoesNotThrow(() -> {
            new BacklogItem("H", "", Priority.HIGH,   1, 1, 0);
            new BacklogItem("M", "", Priority.MEDIUM, 1, 1, 0);
            new BacklogItem("L", "", Priority.LOW,    1, 1, 0);
        }, "REQ-3: HIGH, MEDIUM, and LOW must all be valid Priority enum values.");
    }


    // ================================================================
    // SECTION 2 — PRODUCT BACKLOG MANAGEMENT  (REQ-4, REQ-5)
    // ================================================================

    // ----------------------------------------------------------------
    // REQ-4 : Add BacklogItem  [TC: addBacklogItem]
    // ----------------------------------------------------------------

    /**
     * [REQ-4] [TC: addBacklogItem]
     * A newly created item with valid fields must appear in the Product Backlog.
     */
    @Test @Order(40)
    void req4_tc_addBacklogItem_validItemAppearsInBacklog() {
        BacklogItem item = new BacklogItem(
            "Implement login screen",
            "Build Swing login form with credential validation",
            Priority.HIGH, 5, 3, 0
        );
        productBacklog.addItem(item);

        List<BacklogItem> items = productBacklog.getAllItems();
        assertEquals(1, items.size(),
            "REQ-4 [TC:addBacklogItem]: Backlog must contain exactly one item.");
        assertEquals("Implement login screen", items.get(0).getTitle(),
            "REQ-4 [TC:addBacklogItem]: Stored item must match the added item's title.");
    }

    /**
     * [REQ-4]
     * Multiple items may be added; all must be stored.
     */
    @Test @Order(41)
    void req4_multipleItemsCanBeAdded() {
        productBacklog.addItem(new BacklogItem("Story 1", "", Priority.HIGH,   3, 3, 0));
        productBacklog.addItem(new BacklogItem("Story 2", "", Priority.MEDIUM, 4, 4, 0));
        productBacklog.addItem(new BacklogItem("Story 3", "", Priority.LOW,    2, 2, 0));
        assertEquals(3, productBacklog.getAllItems().size(),
            "REQ-4: All three added items must be present.");
    }

    /**
     * [REQ-4]
     * Adding an item with riskLevel=0 must succeed.
     */
    @Test @Order(42)
    void req4_addItemWithoutRiskSucceeds() {
        BacklogItem item = new BacklogItem("Minimal item", "desc", Priority.LOW, 1, 1, 0);
        assertDoesNotThrow(() -> productBacklog.addItem(item),
            "REQ-4: Adding a BacklogItem with riskLevel=0 must not throw.");
        assertEquals(1, productBacklog.getAllItems().size());
    }

    // ----------------------------------------------------------------
    // REQ-5 : Modify BacklogItem  [TC: editBacklogItem]
    // ----------------------------------------------------------------

    /**
     * [REQ-5] [TC: editBacklogItem]
     * Priority, time estimate, and effort estimate must all be updatable via editItem().
     */
    @Test @Order(50)
    void req5_tc_editBacklogItem_priorityTimeEffortUpdated() {
        BacklogItem item = new BacklogItem("Design data model", "desc", Priority.HIGH, 8, 5, 0);
        productBacklog.addItem(item);

        productBacklog.editItem(item, Priority.MEDIUM, 6, 4, item.getRiskLevel());

        BacklogItem updated = productBacklog.findItemByTitle("Design data model");
        assertNotNull(updated,
            "REQ-5 [TC:editBacklogItem]: Updated item must be retrievable.");
        assertEquals(Priority.MEDIUM, updated.getPriority(),
            "REQ-5 [TC:editBacklogItem]: Priority must reflect the update.");
        assertEquals(6, updated.getTimeEstimate(),   0.001,
            "REQ-5 [TC:editBacklogItem]: Time estimate must reflect the update.");
        assertEquals(4, updated.getEffortEstimate(), 0.001,
            "REQ-5 [TC:editBacklogItem]: Effort estimate must reflect the update.");
    }

    /**
     * [REQ-5]
     * Risk level must also be updatable.
     */
    @Test @Order(51)
    void req5_editBacklogItem_riskUpdated() {
        BacklogItem item = new BacklogItem("Story", "desc", Priority.HIGH, 5, 5, 0);
        productBacklog.addItem(item);

        productBacklog.editItem(item, item.getPriority(), item.getTimeEstimate(),
                                item.getEffortEstimate(), 2);

        assertEquals(2, productBacklog.findItemByTitle("Story").getRiskLevel(), 0.001);
    }

    /**
     * [REQ-5]
     * Updating one item must not corrupt other items in the backlog.
     */
    @Test @Order(52)
    void req5_editDoesNotCorruptOtherItems() {
        BacklogItem itemA = new BacklogItem("Story A", "", Priority.HIGH,   3, 3, 0);
        BacklogItem itemB = new BacklogItem("Story B", "", Priority.MEDIUM, 4, 4, 0);
        productBacklog.addItem(itemA);
        productBacklog.addItem(itemB);

        productBacklog.editItem(itemA, Priority.LOW,
                                itemA.getTimeEstimate(), itemA.getEffortEstimate(),
                                itemA.getRiskLevel());

        assertEquals(Priority.MEDIUM, productBacklog.findItemByTitle("Story B").getPriority(),
            "REQ-5: Editing itemA must not change itemB's priority.");
    }

    // ----------------------------------------------------------------
    // REQ-4 / REQ-5 : Delete  [TC: deleteBacklogItem]
    // ----------------------------------------------------------------

    /**
     * [REQ-4] [TC: deleteBacklogItem]
     * Removing an item must eliminate it from the Product Backlog list.
     */
    @Test @Order(53)
    void req4_tc_deleteBacklogItem_itemRemovedFromList() {
        BacklogItem item = new BacklogItem("Temp item", "desc", Priority.LOW, 2, 1, 0);
        productBacklog.addItem(item);
        assertFalse(productBacklog.getAllItems().isEmpty(),
            "Pre-condition: item must exist before deletion.");

        productBacklog.removeItem(item);

        assertTrue(productBacklog.getAllItems().isEmpty(),
            "REQ-4 [TC:deleteBacklogItem]: Backlog must be empty after removing the only item.");
    }

    /**
     * [REQ-4]
     * Removing one of several items must leave the remaining items intact.
     */
    @Test @Order(54)
    void req4_deleteOneItemLeavesOthersIntact() {
        BacklogItem a = new BacklogItem("Story A", "", Priority.HIGH,   3, 3, 0);
        BacklogItem b = new BacklogItem("Story B", "", Priority.MEDIUM, 4, 4, 0);
        productBacklog.addItem(a);
        productBacklog.addItem(b);

        productBacklog.removeItem(a);

        assertNull(productBacklog.findItemByTitle("Story A"),
            "Deleted item must not be findable.");
        assertNotNull(productBacklog.findItemByTitle("Story B"),
            "Non-deleted item must still be present.");
        assertEquals(1, productBacklog.getAllItems().size());
    }


    // ================================================================
    // SECTION 3 — SPRINT PROPOSAL  (REQ-6, REQ-7)
    // ================================================================

    /**
     * [REQ-6] [TC: GenerateProposal_CapacityFit]
     * Proposed items' total effort must not exceed sprint capacity (≤ 20 hr).
     */
    @Test @Order(60)
    void req6_tc_generateProposal_capacityFit() {
        productBacklog.addItem(new BacklogItem("Item A", "", Priority.HIGH,   8,  8, 0));
        productBacklog.addItem(new BacklogItem("Item B", "", Priority.HIGH,   7,  7, 0));
        productBacklog.addItem(new BacklogItem("Item C", "", Priority.MEDIUM, 6,  6, 0));
        productBacklog.addItem(new BacklogItem("Item D", "", Priority.LOW,    5,  5, 0));

        double capacity = 20;
        List<BacklogItem> proposal = productBacklog.generateSprintProposal(capacity);

        double totalEffort = proposal.stream().mapToDouble(BacklogItem::getEffortEstimate).sum();
        assertTrue(totalEffort <= capacity,
            "REQ-6 [TC:GenerateProposal_CapacityFit]: Total effort " + totalEffort
                + " must not exceed capacity " + capacity + ".");
    }

    /**
     * [REQ-6] [TC: GenerateProposal_PriorityOrder]
     * HIGH priority items must be selected before MEDIUM and LOW.
     */
    @Test @Order(61)
    void req6_tc_generateProposal_highPrioritySelectedFirst() {
        productBacklog.addItem(new BacklogItem("Low task",    "", Priority.LOW,    3, 3, 0));
        productBacklog.addItem(new BacklogItem("Medium task", "", Priority.MEDIUM, 4, 4, 0));
        productBacklog.addItem(new BacklogItem("High task",   "", Priority.HIGH,   5, 5, 0));

        List<BacklogItem> proposal = productBacklog.generateSprintProposal(20);

        assertFalse(proposal.isEmpty(),
            "REQ-6 [TC:GenerateProposal_PriorityOrder]: Proposal must not be empty.");
        assertEquals(Priority.HIGH, proposal.get(0).getPriority(),
            "REQ-6 [TC:GenerateProposal_PriorityOrder]: First item must have HIGH priority.");
    }

    /**
     * [REQ-6]
     * MEDIUM items must appear before LOW items in the proposal ordering.
     */
    @Test @Order(62)
    void req6_mediumPriorityBeforeLowInProposal() {
        productBacklog.addItem(new BacklogItem("Low task",    "", Priority.LOW,    2, 2, 0));
        productBacklog.addItem(new BacklogItem("Medium task", "", Priority.MEDIUM, 3, 3, 0));

        List<BacklogItem> proposal = productBacklog.generateSprintProposal(20);

        assertEquals(2, proposal.size());
        assertEquals(Priority.MEDIUM, proposal.get(0).getPriority(),
            "REQ-6: MEDIUM must come before LOW in the proposal.");
    }

    /**
     * [REQ-6]
     * A single item whose effort exceeds capacity must be excluded.
     */
    @Test @Order(63)
    void req6_oversizedItemExcludedFromProposal() {
        productBacklog.addItem(new BacklogItem("Giant task", "", Priority.HIGH, 25, 25, 0));

        assertTrue(productBacklog.generateSprintProposal(20).isEmpty(),
            "REQ-6: An item with effort > capacity must not appear in the proposal.");
    }

    /**
     * [REQ-6]
     * Zero-capacity sprint must produce an empty proposal.
     */
    @Test @Order(64)
    void req6_zeroCapacityProducesEmptyProposal() {
        productBacklog.addItem(new BacklogItem("Any task", "", Priority.HIGH, 1, 1, 0));

        assertTrue(productBacklog.generateSprintProposal(0).isEmpty(),
            "REQ-6: Sprint capacity of 0 must produce an empty proposal.");
    }

    /**
     * [REQ-6]
     * Empty Product Backlog must produce an empty proposal.
     */
    @Test @Order(65)
    void req6_emptyBacklogProducesEmptyProposal() {
        assertTrue(productBacklog.generateSprintProposal(20).isEmpty(),
            "REQ-6: Empty Product Backlog must produce an empty proposal.");
    }

    // ----------------------------------------------------------------
    // REQ-7 : Scrum Master can modify proposed list before approval
    // ----------------------------------------------------------------

    /**
     * [REQ-7]
     * A Scrum Master must be able to add an item to the pending Sprint proposal.
     */
    @Test @Order(70)
    void req7_scrumMasterCanAddItemToProposal() {
        BacklogItem item = new BacklogItem("Extra story", "", Priority.MEDIUM, 3, 3, 0);
        productBacklog.addItem(item);

        sprintBacklog.addProposedItem(item);

        assertTrue(sprintBacklog.getProposedItems().contains(item),
            "REQ-7: Scrum Master must be able to add an item to the pending proposal.");
    }

    /**
     * [REQ-7]
     * A Scrum Master must be able to remove an item from the pending Sprint proposal.
     */
    @Test @Order(71)
    void req7_scrumMasterCanRemoveItemFromProposal() {
        BacklogItem item = new BacklogItem("Removable story", "", Priority.HIGH, 5, 5, 0);
        productBacklog.addItem(item);

        sprintBacklog.addProposedItem(item);
        sprintBacklog.removeProposedItem(item);

        assertFalse(sprintBacklog.getProposedItems().contains(item),
            "REQ-7: Scrum Master must be able to remove an item from the pending proposal.");
    }

    /**
     * [REQ-7]
     * Pending proposal edits must not remove items from the Product Backlog itself.
     */
    @Test @Order(72)
    void req7_proposalEditsDoNotMutateProductBacklog() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 4, 4, 0);
        productBacklog.addItem(item);
        int sizeBefore = productBacklog.getAllItems().size();

        sprintBacklog.addProposedItem(item);
        sprintBacklog.removeProposedItem(item);

        assertEquals(sizeBefore, productBacklog.getAllItems().size(),
            "REQ-7: Removing from the pending proposal must not remove the item from the Product Backlog.");
    }


    // ================================================================
    // SECTION 4 — SPRINT APPROVAL  (REQ-8)
    // ================================================================

    /**
     * [REQ-8] [TC: approveSprint]
     * Approved items must move to IN_SPRINT status and sprint transitions to ACTIVE.
     */
    @Test @Order(80)
    void req8_tc_approveSprint_itemsMovedAndLocked() {
        BacklogItem item = new BacklogItem("Sprint story", "", Priority.HIGH, 5, 5, 0);
        productBacklog.addItem(item);

        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        assertEquals(Status.IN_SPRINT, item.getStatus(),
            "REQ-8 [TC:approveSprint]: Approved item must have status IN_SPRINT.");
        assertEquals(model.SprintBacklog.SprintState.ACTIVE, sprintBacklog.getState(),
            "REQ-8 [TC:approveSprint]: Sprint must be ACTIVE after approval.");
    }

    /**
     * [REQ-8] [TC: rejectSprint]
     * Rejected items must remain in Product Backlog; sprint returns to PROPOSED.
     */
    @Test @Order(81)
    void req8_tc_rejectSprint_itemsRemainEditable() {
        BacklogItem item = new BacklogItem("Proposed story", "", Priority.HIGH, 5, 5, 0);
        productBacklog.addItem(item);

        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.reject();

        assertEquals(model.SprintBacklog.SprintState.PROPOSED, sprintBacklog.getState(),
            "REQ-8 [TC:rejectSprint]: Sprint must return to PROPOSED after rejection.");
        assertEquals(Status.IN_PRODUCT_BACKLOG, item.getStatus(),
            "REQ-8 [TC:rejectSprint]: Item status must remain IN_PRODUCT_BACKLOG.");
    }

    /**
     * [REQ-8]
     * After rejection, the sprint must accept new proposed items.
     */
    @Test @Order(82)
    void req8_afterRejectionProposalIsEditable() {
        BacklogItem item = new BacklogItem("Re-proposed story", "", Priority.HIGH, 4, 4, 0);
        productBacklog.addItem(item);

        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.reject();

        BacklogItem newItem = new BacklogItem("New story", "", Priority.MEDIUM, 3, 3, 0);
        productBacklog.addItem(newItem);
        assertDoesNotThrow(() -> sprintBacklog.addProposedItem(newItem),
            "REQ-8: After rejection the pending proposal must return to an editable state.");
    }

    /**
     * [REQ-8]
     * Approving multiple items must set all of them to IN_SPRINT.
     */
    @Test @Order(83)
    void req8_multipleItemsAllLockedOnApproval() {
        BacklogItem i1 = new BacklogItem("Story 1", "", Priority.HIGH,   4, 4, 0);
        BacklogItem i2 = new BacklogItem("Story 2", "", Priority.MEDIUM, 3, 3, 0);
        productBacklog.addItem(i1);
        productBacklog.addItem(i2);

        sprintBacklog.setProposedItems(List.of(i1, i2));
        sprintBacklog.approve();

        assertEquals(Status.IN_SPRINT, i1.getStatus(), "REQ-8: Story 1 must be IN_SPRINT.");
        assertEquals(Status.IN_SPRINT, i2.getStatus(), "REQ-8: Story 2 must be IN_SPRINT.");
    }


    // ================================================================
    // SECTION 5 — SPRINT BACKLOG MANAGEMENT  (REQ-9, REQ-10)
    // ================================================================

    /**
     * [REQ-9]
     * Sprint state must be ACTIVE after approval.
     */
    @Test @Order(90)
    void req9_sprintIsActiveAfterApproval() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 3, 3, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        assertEquals(model.SprintBacklog.SprintState.ACTIVE, sprintBacklog.getState(),
            "REQ-9: Sprint must be ACTIVE once items are approved.");
    }

    /**
     * [REQ-9]
     * A fresh Sprint Backlog must not be active.
     */
    @Test @Order(91)
    void req9_freshSprintIsNotActive() {
        assertNotEquals(model.SprintBacklog.SprintState.ACTIVE, sprintBacklog.getState(),
            "REQ-9: A newly created Sprint Backlog must not be active.");
    }

    /**
     * [REQ-9]
     * Attempting to add proposed items after approval must be ignored (sprint locked).
     */
    @Test @Order(92)
    void req9_sprintItemsLockedAfterApproval() {
        BacklogItem item = new BacklogItem("Locked story", "", Priority.HIGH, 5, 5, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        BacklogItem intruder = new BacklogItem("Intruder", "", Priority.LOW, 1, 1, 0);
        productBacklog.addItem(intruder);
        sprintBacklog.addProposedItem(intruder); // must be a no-op in ACTIVE state

        assertFalse(sprintBacklog.getProposedItems().contains(intruder),
            "REQ-9: Proposed-item additions must be ignored once the sprint is ACTIVE.");
    }

    // ----------------------------------------------------------------
    // REQ-10 : Unfinished items returned at sprint end
    // ----------------------------------------------------------------

    /**
     * [REQ-10] [TC: returnUnfinishedItems]
     * Unfinished items must be marked DELAYED and returned to Product Backlog at sprint end.
     */
    @Test @Order(100)
    void req10_tc_returnUnfinishedItems_toProductBacklog() {
        BacklogItem item = new BacklogItem("Unfinished story", "", Priority.HIGH, 6, 6, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        item.logActualEffort(2); // partial work; item stays IN_SPRINT (not COMPLETE)
        productBacklog.removeItem(item); // simulate it was moved out at sprint start
        sprintBacklog.endSprint(productBacklog);

        assertEquals(Status.DELAYED, item.getStatus(),
            "REQ-10: Unfinished item must be marked DELAYED.");
        assertNotNull(productBacklog.findItemByTitle("Unfinished story"),
            "REQ-10 [TC:returnUnfinishedItems]: Unfinished item must be back in Product Backlog.");
        assertEquals(Priority.HIGH, item.getPriority(),
            "REQ-10 [TC:returnUnfinishedItems]: Original priority must be preserved.");
    }

    /**
     * [REQ-10]
     * Completed items must NOT be returned to the Product Backlog at sprint end.
     */
    @Test @Order(101)
    void req10_finishedItemsNotReturnedAtSprintEnd() {
        BacklogItem item = new BacklogItem("Finished story", "", Priority.HIGH, 5, 5, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        item.setStatus(Status.COMPLETE);
        productBacklog.removeItem(item); // remove before endSprint to mirror real flow
        sprintBacklog.endSprint(productBacklog);

        assertNull(productBacklog.findItemByTitle("Finished story"),
            "REQ-10: Completed items must not be re-added to the Product Backlog.");
    }

    /**
     * [REQ-10]
     * Sprint must be COMPLETE after endSprint() is called.
     */
    @Test @Order(102)
    void req10_sprintCompleteAfterEnd() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 3, 3, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();
        item.setStatus(Status.COMPLETE);
        sprintBacklog.endSprint(productBacklog);

        assertEquals(model.SprintBacklog.SprintState.COMPLETE, sprintBacklog.getState(),
            "REQ-10: Sprint must be COMPLETE after endSprint().");
    }

    /**
     * [REQ-10]
     * In a mixed sprint, only unfinished items return to the Product Backlog.
     */
    @Test @Order(103)
    void req10_mixedSprintOnlyUnfinishedReturned() {
        BacklogItem done   = new BacklogItem("Done",     "", Priority.HIGH,   4, 4, 0);
        BacklogItem undone = new BacklogItem("Not done", "", Priority.MEDIUM, 3, 3, 0);
        productBacklog.addItem(done);
        productBacklog.addItem(undone);
        sprintBacklog.setProposedItems(List.of(done, undone));
        sprintBacklog.approve();

        done.setStatus(Status.COMPLETE);
        productBacklog.removeItem(done);
        productBacklog.removeItem(undone);
        sprintBacklog.endSprint(productBacklog);

        assertNull(productBacklog.findItemByTitle("Done"),
            "REQ-10: Completed item must not reappear in Product Backlog.");
        assertNotNull(productBacklog.findItemByTitle("Not done"),
            "REQ-10: Unfinished item must be returned to Product Backlog.");
    }


    // ================================================================
    // SECTION 6 — ENGINEERING TASKS  (REQ-11, REQ-12, REQ-13)
    // ================================================================

    /**
     * [REQ-11] [TC: createETasks]
     * An engineering task must be linked to its parent BacklogItem.
     */
    @Test @Order(110)
    void req11_tc_createETasks_linkedToParent() {
        BacklogItem parent = new BacklogItem("Login feature", "", Priority.HIGH, 5, 5, 0);
        productBacklog.addItem(parent);
        sprintBacklog.setProposedItems(List.of(parent));
        sprintBacklog.approve();

        model.EngineeringTask task =
            new model.EngineeringTask("Build login form", "desc", 3, parent);

        assertSame(parent, task.getParentItem(),
            "REQ-11 [TC:createETasks]: Engineering task must reference its parent BacklogItem.");
        assertEquals("Build login form", task.getTitle(),
            "REQ-11 [TC:createETasks]: Task title must match.");
    }

    /**
     * [REQ-11]
     * Multiple engineering tasks may be linked to one Sprint Backlog item.
     */
    @Test @Order(111)
    void req11_multipleTasksPerSprintItem() {
        BacklogItem parent = new BacklogItem("Complex feature", "", Priority.HIGH, 10, 10, 0);
        productBacklog.addItem(parent);
        sprintBacklog.setProposedItems(List.of(parent));
        sprintBacklog.approve();

        model.EngineeringTask t1 =
            new model.EngineeringTask("Sub-task 1", "", 3, parent);
        model.EngineeringTask t2 =
            new model.EngineeringTask("Sub-task 2", "", 3, parent);
        model.EngineeringTask t3 =
            new model.EngineeringTask("Sub-task 3", "", 4, parent);

        assertSame(parent, t1.getParentItem());
        assertSame(parent, t2.getParentItem());
        assertSame(parent, t3.getParentItem());
        assertNotEquals(t1.getTaskId(), t2.getTaskId(),
            "REQ-11: Each engineering task must have a unique ID.");
    }

    /**
     * [REQ-11]
     * Engineering tasks for different parents must be independent objects.
     */
    @Test @Order(112)
    void req11_tasksDoNotCrossParents() {
        BacklogItem parentA = new BacklogItem("Feature A", "", Priority.HIGH,   5, 5, 0);
        BacklogItem parentB = new BacklogItem("Feature B", "", Priority.MEDIUM, 5, 5, 0);
        productBacklog.addItem(parentA);
        productBacklog.addItem(parentB);
        sprintBacklog.setProposedItems(List.of(parentA, parentB));
        sprintBacklog.approve();

        model.EngineeringTask taskA =
            new model.EngineeringTask("Task for A", "", 2, parentA);

        assertSame(parentA, taskA.getParentItem());
        assertNotSame(parentB, taskA.getParentItem(),
            "REQ-11: Task for parentA must not be linked to parentB.");
    }

    /**
     * [REQ-12]
     * A newly created engineering task must inherit the priority of its parent.
     */
    @Test @Order(120)
    void req12_newTaskInheritsParentPriority() {
        BacklogItem parent = new BacklogItem("High-pri story", "", Priority.HIGH, 4, 4, 0);
        productBacklog.addItem(parent);
        sprintBacklog.setProposedItems(List.of(parent));
        sprintBacklog.approve();

        model.EngineeringTask task =
            new model.EngineeringTask("Sub-task", "desc", 2, parent);

        assertEquals(Priority.HIGH, task.getPriority(),
            "REQ-12: New engineering task must inherit HIGH priority from parent.");
    }

    /**
     * [REQ-12]
     * When the parent's priority changes, syncPriorityFromParent() must propagate it.
     */
    @Test @Order(121)
    void req12_priorityChangePropagatestoAllChildren() {
        BacklogItem parent = new BacklogItem("Propagation story", "", Priority.LOW, 4, 4, 0);
        productBacklog.addItem(parent);
        sprintBacklog.setProposedItems(List.of(parent));
        sprintBacklog.approve();

        model.EngineeringTask t1 =
            new model.EngineeringTask("Child 1", "", 2, parent);
        model.EngineeringTask t2 =
            new model.EngineeringTask("Child 2", "", 2, parent);

        parent.setPriority(Priority.HIGH);
        t1.syncPriorityFromParent();
        t2.syncPriorityFromParent();

        assertEquals(Priority.HIGH, t1.getPriority(),
            "REQ-12: Child 1 must reflect the updated priority.");
        assertEquals(Priority.HIGH, t2.getPriority(),
            "REQ-12: Child 2 must reflect the updated priority.");
    }

    /**
     * [REQ-12]
     * Priority propagation must work in all directions (HIGH→LOW, LOW→MEDIUM, etc.).
     */
    @Test @Order(122)
    void req12_priorityPropagationAllDirections() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.HIGH, 3, 3, 0);
        productBacklog.addItem(parent);
        sprintBacklog.setProposedItems(List.of(parent));
        sprintBacklog.approve();

        model.EngineeringTask task =
            new model.EngineeringTask("Task", "", 1, parent);

        parent.setPriority(Priority.LOW);
        task.syncPriorityFromParent();
        assertEquals(Priority.LOW, task.getPriority(), "REQ-12: HIGH→LOW propagation must work.");

        parent.setPriority(Priority.MEDIUM);
        task.syncPriorityFromParent();
        assertEquals(Priority.MEDIUM, task.getPriority(), "REQ-12: LOW→MEDIUM propagation must work.");
    }

    /**
     * [REQ-13] [TC: editETasks]
     * An engineering task's title and effort estimate must be independently updatable.
     */
    @Test @Order(130)
    void req13_tc_editETasks_titleAndEffortUpdated() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.MEDIUM, 5, 5, 0);
        productBacklog.addItem(parent);
        sprintBacklog.setProposedItems(List.of(parent));
        sprintBacklog.approve();

        model.EngineeringTask task =
            new model.EngineeringTask("Original title", "", 3, parent);

        task.setTitle("Updated title");
        task.setEffortEstimate(2);

        assertEquals("Updated title", task.getTitle(),
            "REQ-13 [TC:editETasks]: Task title must be updated.");
        assertEquals(2, task.getEffortEstimate(), 0.001,
            "REQ-13 [TC:editETasks]: Effort estimate must be updated.");
    }

    /**
     * [REQ-13]
     * Two sibling tasks under the same parent may carry different effort estimates.
     */
    @Test @Order(131)
    void req13_siblingTasksHaveIndependentEffort() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.HIGH, 10, 10, 0);
        productBacklog.addItem(parent);
        sprintBacklog.setProposedItems(List.of(parent));
        sprintBacklog.approve();

        model.EngineeringTask t1 =
            new model.EngineeringTask("Task 1", "", 3, parent);
        model.EngineeringTask t2 =
            new model.EngineeringTask("Task 2", "", 5, parent);

        assertNotEquals(t1.getEffortEstimate(), t2.getEffortEstimate(), 0.001,
            "REQ-13: Sibling tasks must be able to carry independent effort estimates.");
    }

    /**
     * [REQ-13] [TC: removeETasks]
     * Discarding an engineering task must not affect its parent BacklogItem.
     */
    @Test @Order(132)
    void req13_tc_removeETasks_taskCanBeDiscarded() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.LOW, 3, 3, 0);
        productBacklog.addItem(parent);
        sprintBacklog.setProposedItems(List.of(parent));
        sprintBacklog.approve();

        model.EngineeringTask task =
            new model.EngineeringTask("Task to remove", "", 1, parent);
        String removedId = task.getTaskId();
        task = null; // de-reference

        assertNotNull(productBacklog.findItemByTitle("Story"),
            "REQ-13 [TC:removeETasks]: Discarding a task must not affect its parent BacklogItem.");
        assertNotNull(removedId, "REQ-13: Task had a valid ID before being discarded.");
    }

    /**
     * [REQ-13]
     * Discarding one sibling task must not affect another sibling task.
     */
    @Test @Order(133)
    void req13_removeOneTaskLeavesOthersIntact() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.HIGH, 8, 8, 0);
        productBacklog.addItem(parent);
        sprintBacklog.setProposedItems(List.of(parent));
        sprintBacklog.approve();

        model.EngineeringTask t1 =
            new model.EngineeringTask("Keep me",   "", 2, parent);
        model.EngineeringTask t2 =
            new model.EngineeringTask("Remove me", "", 3, parent);
        String t1Id = t1.getTaskId();

        t2 = null; // de-reference t2

        assertEquals(t1Id, t1.getTaskId(),
            "REQ-13: Discarding t2 must not affect t1's identity.");
        assertSame(parent, t1.getParentItem(),
            "REQ-13: t1's parent reference must be unchanged.");
    }

    // ----------------------------------------------------------------
    // REQ-11 : Complete / Delayed item status
    // ----------------------------------------------------------------

    /**
     * [REQ-11] [TC: completeBacklogItem]
     * Marking a Sprint Backlog item complete must set its status to COMPLETE.
     */
    @Test @Order(140)
    void req11_tc_completeBacklogItem_statusSetComplete() {
        BacklogItem item = new BacklogItem("Done story", "", Priority.HIGH, 4, 4, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        item.setStatus(Status.COMPLETE);

        assertEquals(Status.COMPLETE, item.getStatus(),
            "REQ-11 [TC:completeBacklogItem]: Item must have status COMPLETE.");
    }

    /**
     * [REQ-11] [TC: completeBacklogItem — delayed variant]
     * Marking a Sprint Backlog item delayed must set its status to DELAYED.
     */
    @Test @Order(141)
    void req11_tc_completeBacklogItem_statusSetDelayed() {
        BacklogItem item = new BacklogItem("Delayed story", "", Priority.MEDIUM, 5, 5, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        item.setStatus(Status.DELAYED);

        assertEquals(Status.DELAYED, item.getStatus(),
            "REQ-11 [TC:completeBacklogItem(delayed)]: Item must have status DELAYED.");
    }

    /**
     * [REQ-11]
     * A completed item must not simultaneously be flagged as delayed.
     */
    @Test @Order(142)
    void req11_completedItemNotAlsoDelayed() {
        BacklogItem item = new BacklogItem("Done story", "", Priority.HIGH, 4, 4, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        item.setStatus(Status.COMPLETE);

        assertNotEquals(Status.DELAYED, item.getStatus(),
            "REQ-11: A completed item must not also be flagged as delayed.");
    }


    // ================================================================
    // SECTION 7 — PROGRESS TRACKING  (REQ-14)
    // ================================================================

    /**
     * [REQ-14] [TC: logTime]
     * Logging actual time and effort must accumulate on the BacklogItem.
     */
    @Test @Order(150)
    void req14_tc_logTime_actualTimeAndEffortUpdated() {
        BacklogItem item = new BacklogItem("Tracked story", "", Priority.HIGH, 8, 8, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        item.logActualTime(3);
        item.logActualEffort(3);

        assertEquals(3, item.getActualTime(),   0.001, "REQ-14 [TC:logTime]: Actual time must be 3.");
        assertEquals(3, item.getActualEffort(), 0.001, "REQ-14 [TC:logTime]: Actual effort must be 3.");
    }

    /**
     * [REQ-14]
     * Multiple log calls must accumulate correctly.
     */
    @Test @Order(151)
    void req14_logTimeAccumulatesAcrossMultipleCalls() {
        BacklogItem item = new BacklogItem("Multi-log story", "", Priority.MEDIUM, 10, 10, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        item.logActualTime(2);   item.logActualTime(3);
        item.logActualEffort(2); item.logActualEffort(3);

        assertEquals(5, item.getActualTime(),   0.001, "REQ-14: Accumulated actual time must be 5.");
        assertEquals(5, item.getActualEffort(), 0.001, "REQ-14: Accumulated actual effort must be 5.");
    }

    /**
     * [REQ-14]
     * Before any progress is logged, actual time and effort must be zero.
     */
    @Test @Order(152)
    void req14_initialActualTimeAndEffortAreZero() {
        BacklogItem item = new BacklogItem("Fresh story", "", Priority.HIGH, 6, 6, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();

        assertEquals(0, item.getActualTime(),   0.001, "REQ-14: Initial actual time must be 0.");
        assertEquals(0, item.getActualEffort(), 0.001, "REQ-14: Initial actual effort must be 0.");
    }

    /**
     * [REQ-14]
     * Logging time on one item must not affect another item.
     */
    @Test @Order(153)
    void req14_logTimeDoesNotCrossItems() {
        BacklogItem a = new BacklogItem("Story A", "", Priority.HIGH,   5, 5, 0);
        BacklogItem b = new BacklogItem("Story B", "", Priority.MEDIUM, 4, 4, 0);
        productBacklog.addItem(a);
        productBacklog.addItem(b);
        sprintBacklog.setProposedItems(List.of(a, b));
        sprintBacklog.approve();

        a.logActualTime(3);
        a.logActualEffort(3);

        assertEquals(0, b.getActualTime(),   0.001, "REQ-14: Story B actual time must be unaffected.");
        assertEquals(0, b.getActualEffort(), 0.001, "REQ-14: Story B actual effort must be unaffected.");
    }


    // ================================================================
    // SECTION 8 — CHARTS  (REQ-15)
    // ================================================================

    /**
     * [REQ-15] [TC: displayVelocity]
     * Velocity must equal the sum of completed items' effort.
     */
    @Test @Order(160)
    void req15_tc_displayVelocity_equalsCompletedEffort() {
        BacklogItem i1 = new BacklogItem("Story 1", "", Priority.HIGH,   5, 5, 0);
        BacklogItem i2 = new BacklogItem("Story 2", "", Priority.MEDIUM, 4, 4, 0);
        productBacklog.addItem(i1);
        productBacklog.addItem(i2);
        sprintBacklog.setProposedItems(List.of(i1, i2));
        sprintBacklog.approve();

        i1.setStatus(Status.COMPLETE);
        i2.setStatus(Status.COMPLETE);
        sprintBacklog.endSprint(productBacklog);

        assertEquals(9, sprintBacklog.calculateVelocity(), 0.001,
            "REQ-15 [TC:displayVelocity]: Velocity must be 5+4=9.");
    }

    /**
     * [REQ-15]
     * Velocity must be 0 when no items were completed.
     */
    @Test @Order(161)
    void req15_velocityZeroWhenNothingCompleted() {
        BacklogItem item = new BacklogItem("Incomplete", "", Priority.HIGH, 6, 6, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();
        sprintBacklog.endSprint(productBacklog);

        assertEquals(0, sprintBacklog.calculateVelocity(), 0.001,
            "REQ-15: Velocity must be 0 when no items were completed.");
    }

    /**
     * [REQ-15]
     * Velocity must count only COMPLETE items, not delayed or unfinished ones.
     */
    @Test @Order(162)
    void req15_velocityExcludesDelayedAndUnfinishedItems() {
        BacklogItem done    = new BacklogItem("Done",       "", Priority.HIGH,   5, 5, 0);
        BacklogItem delayed = new BacklogItem("Delayed",    "", Priority.MEDIUM, 3, 3, 0);
        BacklogItem undone  = new BacklogItem("Unfinished", "", Priority.LOW,    4, 4, 0);
        productBacklog.addItem(done);
        productBacklog.addItem(delayed);
        productBacklog.addItem(undone);
        sprintBacklog.setProposedItems(List.of(done, delayed, undone));
        sprintBacklog.approve();

        done.setStatus(Status.COMPLETE);
        delayed.setStatus(Status.DELAYED);
        // undone stays IN_SPRINT
        sprintBacklog.endSprint(productBacklog);

        assertEquals(5, sprintBacklog.calculateVelocity(), 0.001,
            "REQ-15: Only the completed item (effort=5) should count toward velocity.");
    }

    /**
     * [REQ-15]
     * Burndown data must be recordable and retrievable day-by-day.
     */
    @Test @Order(163)
    void req15_burndownDataCanBeRecordedAndRetrieved() {
        BacklogItem item = new BacklogItem("Burndown story", "", Priority.HIGH, 10, 10, 0);
        productBacklog.addItem(item);
        sprintBacklog.setProposedItems(List.of(item));
        sprintBacklog.approve();
        sprintBacklog.startSprint(); // initialises day-0 burndown = totalPlannedEffort

        sprintBacklog.recordBurndown(1, 10);
        sprintBacklog.recordBurndown(2,  6);
        sprintBacklog.recordBurndown(3,  2);

        java.util.Map<Integer, Double> data = sprintBacklog.getBurndownData();
        assertEquals(10, data.get(0), 0.001, "REQ-15: Day-0 must equal planned effort.");
        assertEquals(6,  data.get(2), 0.001, "REQ-15: Day-2 remaining effort must be 6.");
        assertEquals(2,  data.get(3), 0.001, "REQ-15: Day-3 remaining effort must be 2.");
    }

    /**
     * [REQ-15]
     * Burndown total at sprint start must equal the sum of all committed items' effort.
     */
    @Test @Order(164)
    void req15_burndownTotalAtSprintStartEqualsAllEffort() {
        BacklogItem i1 = new BacklogItem("Story 1", "", Priority.HIGH,   5, 5, 0);
        BacklogItem i2 = new BacklogItem("Story 2", "", Priority.MEDIUM, 3, 3, 0);
        productBacklog.addItem(i1);
        productBacklog.addItem(i2);
        sprintBacklog.setProposedItems(List.of(i1, i2));
        sprintBacklog.approve();
        sprintBacklog.startSprint();

        java.util.Map<Integer, Double> data = sprintBacklog.getBurndownData();
        assertEquals(8, data.get(0), 0.001,
            "REQ-15: Burndown starting total must be 5+3=8.");
    }


    // ================================================================
    // SECTION 9 — AUTHENTICATION  (REQ-16)
    // ================================================================

    /**
     * [REQ-16] [TC: loginUserDashboard]
     * teamMember credentials must resolve to SCRUM_TEAM role.
     */
    @Test @Order(170)
    void req16_tc_loginUserDashboard_scrumTeamRole() {
        model.User user = project.login("teamMember", "team123");
        assertNotNull(user, "REQ-16: teamMember must authenticate.");
        assertEquals(model.User.Role.SCRUM_TEAM, user.getRole(),
            "REQ-16 [TC:loginUserDashboard]: Must resolve to SCRUM_TEAM.");
    }

    /**
     * [REQ-16] [TC: loginScrumMasterDashboard]
     * scrummaster credentials must resolve to SCRUM_MASTER role.
     */
    @Test @Order(171)
    void req16_tc_loginScrumMasterDashboard_scrumMasterRole() {
        model.User user = project.login("scrummaster", "sm123");
        assertNotNull(user, "REQ-16: scrummaster must authenticate.");
        assertEquals(model.User.Role.SCRUM_MASTER, user.getRole(),
            "REQ-16 [TC:loginScrumMasterDashboard]: Must resolve to SCRUM_MASTER.");
    }

    /**
     * [REQ-16] [TC: loginProductOwnerDashboard]
     * productowner credentials must resolve to PRODUCT_OWNER role.
     */
    @Test @Order(172)
    void req16_tc_loginProductOwnerDashboard_productOwnerRole() {
        model.User user = project.login("productowner", "po123");
        assertNotNull(user, "REQ-16: productowner must authenticate.");
        assertEquals(model.User.Role.PRODUCT_OWNER, user.getRole(),
            "REQ-16 [TC:loginProductOwnerDashboard]: Must resolve to PRODUCT_OWNER.");
    }

    /**
     * [REQ-16]
     * Wrong password must return null.
     */
    @Test @Order(173)
    void req16_wrongPasswordReturnsNull() {
        assertNull(project.login("productowner", "wrong_password"),
            "REQ-16: Wrong password must return null.");
    }

    /**
     * [REQ-16]
     * Unknown username must return null.
     */
    @Test @Order(174)
    void req16_unknownUsernameReturnsNull() {
        assertNull(project.login("nobody", "pass1"),
            "REQ-16: Unknown username must return null.");
    }

    /**
     * [REQ-16]
     * Empty username must return null.
     */
    @Test @Order(175)
    void req16_emptyUsernameReturnsNull() {
        assertNull(project.login("", "pass1"),
            "REQ-16: Empty username must return null.");
    }

    /**
     * [REQ-16]
     * Empty password must return null.
     */
    @Test @Order(176)
    void req16_emptyPasswordReturnsNull() {
        assertNull(project.login("teamMember", ""),
            "REQ-16: Empty password must return null.");
    }

    /**
     * [REQ-16]
     * Credentials must be case-sensitive.
     */
    @Test @Order(177)
    void req16_credentialsAreCaseSensitive() {
        assertNull(project.login("TeamMember", "team123"),
            "REQ-16: Authentication must be case-sensitive.");
    }


    // ================================================================
    // SECTION 10 — ROLE-BASED ROUTING  (REQ-17)
    // ================================================================

    /**
     * [REQ-17]
     * SCRUM_TEAM role must be returned for teamMember login.
     */
    @Test @Order(180)
    void req17_scrumTeamRoleCorrectlyResolved() {
        model.User user = project.login("teamMember", "team123");
        assertNotNull(user);
        assertEquals(model.User.Role.SCRUM_TEAM, user.getRole(),
            "REQ-17: SCRUM_TEAM role must be resolvable for role-based routing.");
    }

    /**
     * [REQ-17]
     * SCRUM_MASTER role must be returned for scrummaster login.
     */
    @Test @Order(181)
    void req17_scrumMasterRoleCorrectlyResolved() {
        model.User user = project.login("scrummaster", "sm123");
        assertNotNull(user);
        assertEquals(model.User.Role.SCRUM_MASTER, user.getRole(),
            "REQ-17: SCRUM_MASTER role must be resolvable for role-based routing.");
    }

    /**
     * [REQ-17]
     * PRODUCT_OWNER role must be returned for productowner login.
     */
    @Test @Order(182)
    void req17_productOwnerRoleCorrectlyResolved() {
        model.User user = project.login("productowner", "po123");
        assertNotNull(user);
        assertEquals(model.User.Role.PRODUCT_OWNER, user.getRole(),
            "REQ-17: PRODUCT_OWNER role must be resolvable for role-based routing.");
    }

    /**
     * [REQ-17]
     * A failed login must return null — the caller routes to the login screen.
     */
    @Test @Order(183)
    void req17_failedLoginReturnsNull() {
        model.User user = project.login("nobody", "badpass");
        assertNull(user,
            "REQ-17: Unauthenticated login must return null so the caller routes to LOGIN_VIEW.");
    }


    // ================================================================
    // SECTION 11 — USER REGISTRATION
    // ================================================================

    /**
     * A newly registered user must be authenticatable immediately.
     */
    @Test @Order(190)
    void reg_registeredUserCanLogin() {
        boolean ok = project.registerUser("newdev", "pass123", model.User.Role.SCRUM_TEAM);
        assertTrue(ok, "registerUser must return true for a fresh username.");
        assertNotNull(project.login("newdev", "pass123"),
            "Newly registered user must be able to log in.");
    }

    /**
     * Registered user must have the role specified at registration.
     */
    @Test @Order(191)
    void reg_registeredUserHasCorrectRole() {
        project.registerUser("newowner", "pw", model.User.Role.PRODUCT_OWNER);
        model.User user = project.login("newowner", "pw");
        assertNotNull(user);
        assertEquals(model.User.Role.PRODUCT_OWNER, user.getRole(),
            "Registered user must have the role specified during registration.");
    }

    /**
     * Registering a duplicate username must return false.
     */
    @Test @Order(192)
    void reg_duplicateUsernameReturnsFalse() {
        boolean first  = project.registerUser("dupuser", "pass1", model.User.Role.SCRUM_TEAM);
        boolean second = project.registerUser("dupuser", "pass2", model.User.Role.SCRUM_MASTER);
        assertTrue(first,   "First registration must succeed.");
        assertFalse(second, "Duplicate username registration must return false.");
    }

    /**
     * A duplicate registration attempt must not overwrite the original password.
     */
    @Test @Order(193)
    void reg_duplicateAttemptDoesNotOverwritePassword() {
        project.registerUser("dupuser2", "original", model.User.Role.SCRUM_TEAM);
        project.registerUser("dupuser2", "overwrite", model.User.Role.SCRUM_MASTER);
        assertNotNull(project.login("dupuser2", "original"),
            "Original password must still work after a failed duplicate registration.");
        assertNull(project.login("dupuser2", "overwrite"),
            "Overwrite password must not authenticate.");
    }

    /**
     * Registering a username that is already seeded as a default must fail.
     */
    @Test @Order(194)
    void reg_cannotRegisterExistingDefaultUsername() {
        assertFalse(project.registerUser("productowner", "newpass", model.User.Role.SCRUM_TEAM),
            "Registering a default account username must fail.");
    }


    // ================================================================
    // SECTION 12 — BACKLOGITEM ASSIGNEE & TASK MANAGEMENT
    // ================================================================

    /**
     * A new BacklogItem must have no assignee.
     */
    @Test @Order(200)
    void backlog_newItemHasNoAssignee() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 3, 3, 0);
        assertNull(item.getAssignee(), "A new BacklogItem must have no assignee.");
    }

    /**
     * Setting an assignee must be retrievable via getAssignee.
     */
    @Test @Order(201)
    void backlog_assigneeCanBeSetAndRetrieved() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 3, 3, 0);
        item.setAssignee("teamMember");
        assertEquals("teamMember", item.getAssignee(),
            "Assignee must be retrievable after setAssignee.");
    }

    /**
     * Assignee can be reassigned to a different user.
     */
    @Test @Order(202)
    void backlog_assigneeCanBeReassigned() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 3, 3, 0);
        item.setAssignee("alice");
        item.setAssignee("bob");
        assertEquals("bob", item.getAssignee(),
            "Reassigning must overwrite the previous assignee.");
    }

    /**
     * A new BacklogItem must have an empty task list.
     */
    @Test @Order(210)
    void backlog_newItemHasEmptyTaskList() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 3, 3, 0);
        assertTrue(item.getTasks().isEmpty(),
            "A new BacklogItem must have no engineering tasks.");
    }

    /**
     * Adding a task must make it retrievable via getTasks.
     */
    @Test @Order(211)
    void backlog_taskCanBeAddedAndRetrieved() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 5, 5, 0);
        model.EngineeringTask task = new model.EngineeringTask("Write tests", "", 2, item);
        item.addTask(task);
        assertTrue(item.getTasks().contains(task),
            "Added task must be retrievable from the item.");
    }

    /**
     * Adding the same task twice must not create a duplicate.
     */
    @Test @Order(212)
    void backlog_duplicateTaskNotAdded() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 5, 5, 0);
        model.EngineeringTask task = new model.EngineeringTask("Write tests", "", 2, item);
        item.addTask(task);
        item.addTask(task);
        assertEquals(1, item.getTasks().size(),
            "Adding the same task twice must not create a duplicate.");
    }

    /**
     * Multiple distinct tasks can be added to one item.
     */
    @Test @Order(213)
    void backlog_multipleTasksCanBeAdded() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 10, 10, 0);
        item.addTask(new model.EngineeringTask("Task A", "", 3, item));
        item.addTask(new model.EngineeringTask("Task B", "", 3, item));
        item.addTask(new model.EngineeringTask("Task C", "", 4, item));
        assertEquals(3, item.getTasks().size(),
            "Three distinct tasks must all be stored.");
    }

    /**
     * Removing a task must remove it from the list.
     */
    @Test @Order(214)
    void backlog_taskCanBeRemoved() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 5, 5, 0);
        model.EngineeringTask task = new model.EngineeringTask("Remove me", "", 2, item);
        item.addTask(task);
        item.removeTask(task);
        assertFalse(item.getTasks().contains(task),
            "Removed task must no longer appear in the task list.");
    }

    /**
     * Removing one task must not affect other tasks on the same item.
     */
    @Test @Order(215)
    void backlog_removingOneTaskLeavesOthersIntact() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 8, 8, 0);
        model.EngineeringTask keep   = new model.EngineeringTask("Keep",   "", 3, item);
        model.EngineeringTask remove = new model.EngineeringTask("Remove", "", 5, item);
        item.addTask(keep);
        item.addTask(remove);
        item.removeTask(remove);
        assertTrue(item.getTasks().contains(keep),
            "Removing one task must not affect other tasks on the same item.");
        assertEquals(1, item.getTasks().size());
    }

    /**
     * getTasks must return a defensive copy — modifying it must not affect the item's internal list.
     */
    @Test @Order(216)
    void backlog_getTasksReturnsDefensiveCopy() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 5, 5, 0);
        model.EngineeringTask task = new model.EngineeringTask("Task", "", 2, item);
        item.addTask(task);
        item.getTasks().clear();
        assertEquals(1, item.getTasks().size(),
            "getTasks must return a defensive copy — clearing it must not affect the item.");
    }

    /**
     * A task added to one item must not appear in another item's task list.
     */
    @Test @Order(217)
    void backlog_tasksDoNotLeakBetweenItems() {
        BacklogItem itemA = new BacklogItem("Story A", "", Priority.HIGH,   5, 5, 0);
        BacklogItem itemB = new BacklogItem("Story B", "", Priority.MEDIUM, 5, 5, 0);
        model.EngineeringTask task = new model.EngineeringTask("Task", "", 2, itemA);
        itemA.addTask(task);
        assertFalse(itemB.getTasks().contains(task),
            "Task added to itemA must not appear in itemB's task list.");
    }
}
