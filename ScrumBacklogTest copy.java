import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import static org.junit.jupiter.api.Assertions.*;

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
 *   REQ-7   Developers may modify proposed Sprint list before finalisation
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
 * Assumed model/controller classes (implement to match these signatures):
 *   BacklogItem(String title, String desc, Priority priority, int timeEst, int effortEst)
 *   ProductBacklog, SprintBacklog(int capacity)
 *   EngineeringTask(String title, String parentId, int effortEst)
 *   UserStore, AuthService(UserStore)
 *   Role     { PRODUCT_OWNER, SCRUM_MASTER, SCRUM_TEAM }
 *   Priority { HIGH, MEDIUM, LOW }
 *   SprintProposalEngine(ProductBacklog, int capacity)
 *   ProgressLog — getActualTime(), getActualEffort(), getRemainingEffort()
 *   ChartDataProvider(SprintBacklog)
 */
@TestMethodOrder(OrderAnnotation.class)
public class ScrumBacklogTest {

    // ----------------------------------------------------------------
    // Fixtures — recreated fresh before every test
    // ----------------------------------------------------------------

    private ProductBacklog  productBacklog;
    private SprintBacklog   sprintBacklog;
    private UserStore       userStore;
    private AuthService     authService;

    @BeforeEach
    void setUp() {
        productBacklog = new ProductBacklog();
        sprintBacklog  = new SprintBacklog(20);   // default 20-hr sprint capacity
        userStore      = new UserStore();
        authService    = new AuthService(userStore);

        userStore.addUser("team_member",   "pass1", Role.SCRUM_TEAM);
        userStore.addUser("scrum_master",  "pass2", Role.SCRUM_MASTER);
        userStore.addUser("product_owner", "pass3", Role.PRODUCT_OWNER);
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
        productBacklog.addItem(new BacklogItem("Story A", "", Priority.HIGH, 3, 3));
        productBacklog.addItem(new BacklogItem("Story B", "", Priority.LOW,  2, 2));
        assertEquals(2, productBacklog.getItems().size(),
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
        SprintBacklog sprint1 = new SprintBacklog(20);
        SprintBacklog sprint2 = new SprintBacklog(15);
        assertNotSame(sprint1, sprint2,
            "REQ-2: Sprint Backlogs must be independent objects.");
        assertEquals(20, sprint1.getCapacity());
        assertEquals(15, sprint2.getCapacity());
    }

    /**
     * [REQ-2]
     * Items approved into one sprint must not appear in another.
     */
    @Test @Order(21)
    void req2_sprintBacklogsAreIndependent() {
        BacklogItem item = new BacklogItem("Isolated story", "", Priority.HIGH, 5, 5);
        productBacklog.addItem(item);

        SprintBacklog sprint1 = new SprintBacklog(20);
        SprintBacklog sprint2 = new SprintBacklog(20);
        sprint1.approve(List.of(item));

        assertTrue(sprint1.contains(item.getId()),
            "REQ-2: Item should be in sprint1.");
        assertFalse(sprint2.contains(item.getId()),
            "REQ-2: Item must not appear in sprint2 — backlogs are independent.");
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
            5,  // time estimate
            3   // effort estimate
        );
        assertEquals("Login screen",          item.getTitle());
        assertEquals("Build Swing login form", item.getDescription());
        assertEquals(Priority.HIGH,            item.getPriority());
        assertEquals(5,                        item.getTimeEstimate());
        assertEquals(3,                        item.getEffortEstimate());
    }

    /**
     * [REQ-3]
     * Risk level is optional — must default to null when not supplied.
     */
    @Test @Order(31)
    void req3_riskLevelDefaultsToZero() {
        BacklogItem item = new BacklogItem("Story", "desc", Priority.MEDIUM, 4, 4, 0);
        assertEquals(0, item.getRiskLevel());
    }

    /**
     * [REQ-3]
     * Risk level can be set after construction.
     */
    @Test @Order(32)
    void req3_riskLevelCanBeSet() {
        BacklogItem item = new BacklogItem("Story", "desc", Priority.LOW, 2, 2, 0);
        item.setRiskLevel(1);
        assertEquals(1, item.getRiskLevel());
    }

    /**
     * [REQ-3]
     * All three priority levels (HIGH, MEDIUM, LOW) must be valid enum values.
     */
    @Test @Order(33)
    void req3_allThreePriorityLevelsExist() {
        assertDoesNotThrow(() -> {
            new BacklogItem("H", "", Priority.HIGH,   1, 1);
            new BacklogItem("M", "", Priority.MEDIUM, 1, 1);
            new BacklogItem("L", "", Priority.LOW,    1, 1);
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
            Priority.HIGH, 5, 3
        );
        productBacklog.addItem(item);

        List<BacklogItem> items = productBacklog.getItems();
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
        productBacklog.addItem(new BacklogItem("Story 1", "", Priority.HIGH,   3, 3));
        productBacklog.addItem(new BacklogItem("Story 2", "", Priority.MEDIUM, 4, 4));
        productBacklog.addItem(new BacklogItem("Story 3", "", Priority.LOW,    2, 2));
        assertEquals(3, productBacklog.getItems().size(),
            "REQ-4: All three added items must be present.");
    }

    /**
     * [REQ-4]
     * Adding an item without a risk level (optional) must succeed.
     */
    @Test @Order(42)
    void req4_addItemWithoutRiskSucceeds() {
    BacklogItem item = new BacklogItem("Minimal item", "desc", Priority.LOW, 1, 1, 0);
        assertDoesNotThrow(() -> productBacklog.addItem(item),
            "REQ-4: Adding a BacklogItem without a risk level must not throw.");
        assertEquals(1, productBacklog.getItems().size());
    }

    // ----------------------------------------------------------------
    // REQ-5 : Modify BacklogItem  [TC: editBacklogItem]
    // ----------------------------------------------------------------

    /**
     * [REQ-5] [TC: editBacklogItem]
     * Priority, time estimate, and effort estimate must all be updatable.
     */
    @Test @Order(50)
    void req5_tc_editBacklogItem_priorityTimeEffortUpdated() {
        BacklogItem item = new BacklogItem("Design data model", "desc", Priority.HIGH, 8, 5);
        productBacklog.addItem(item);

        item.setPriority(Priority.MEDIUM);
        item.setTimeEstimate(6);
        item.setEffortEstimate(4);
        productBacklog.updateItem(item);

        BacklogItem updated = productBacklog.findById(item.getId());
        assertNotNull(updated,
            "REQ-5 [TC:editBacklogItem]: Updated item must be retrievable.");
        assertEquals(Priority.MEDIUM, updated.getPriority(),
            "REQ-5 [TC:editBacklogItem]: Priority must reflect the update.");
        assertEquals(6, updated.getTimeEstimate(),
            "REQ-5 [TC:editBacklogItem]: Time estimate must reflect the update.");
        assertEquals(4, updated.getEffortEstimate(),
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

        item.setRiskLevel(2);
        productBacklog.updateItem(item);

        assertEquals(2, productBacklog.findById(item.getId()).getRiskLevel());
    }

    /**
     * [REQ-5]
     * Updating one item must not corrupt other items in the backlog.
     */
    @Test @Order(52)
    void req5_editDoesNotCorruptOtherItems() {
        BacklogItem itemA = new BacklogItem("Story A", "", Priority.HIGH,   3, 3);
        BacklogItem itemB = new BacklogItem("Story B", "", Priority.MEDIUM, 4, 4);
        productBacklog.addItem(itemA);
        productBacklog.addItem(itemB);

        itemA.setPriority(Priority.LOW);
        productBacklog.updateItem(itemA);

        assertEquals(Priority.MEDIUM, productBacklog.findById(itemB.getId()).getPriority(),
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
        BacklogItem item = new BacklogItem("Temp item", "desc", Priority.LOW, 2, 1);
        productBacklog.addItem(item);
        assertFalse(productBacklog.getItems().isEmpty(),
            "Pre-condition: item must exist before deletion.");

        productBacklog.removeItem(item.getId());

        assertTrue(productBacklog.getItems().isEmpty(),
            "REQ-4 [TC:deleteBacklogItem]: Backlog must be empty after removing the only item.");
    }

    /**
     * [REQ-4]
     * Removing one of several items must leave the remaining items intact.
     */
    @Test @Order(54)
    void req4_deleteOneItemLeavesOthersIntact() {
        BacklogItem a = new BacklogItem("Story A", "", Priority.HIGH,   3, 3);
        BacklogItem b = new BacklogItem("Story B", "", Priority.MEDIUM, 4, 4);
        productBacklog.addItem(a);
        productBacklog.addItem(b);

        productBacklog.removeItem(a.getId());

        assertNull(productBacklog.findById(a.getId()),
            "Deleted item must not be findable.");
        assertNotNull(productBacklog.findById(b.getId()),
            "Non-deleted item must still be present.");
        assertEquals(1, productBacklog.getItems().size());
    }


    // ================================================================
    // SECTION 3 — SPRINT PROPOSAL  (REQ-6, REQ-7)
    // ================================================================

    // ----------------------------------------------------------------
    // REQ-6 : Generate proposal  [TC: GenerateProposal_CapacityFit]
    //                             [TC: GenerateProposal_PriorityOrder]
    // ----------------------------------------------------------------

    /**
     * [REQ-6] [TC: GenerateProposal_CapacityFit]
     * Proposed items' total effort must not exceed sprint capacity (≤ 20 hr).
     */
    @Test @Order(60)
    void req6_tc_generateProposal_capacityFit() {
        productBacklog.addItem(new BacklogItem("Item A", "", Priority.HIGH,   8,  8));
        productBacklog.addItem(new BacklogItem("Item B", "", Priority.HIGH,   7,  7));
        productBacklog.addItem(new BacklogItem("Item C", "", Priority.MEDIUM, 6,  6));
        productBacklog.addItem(new BacklogItem("Item D", "", Priority.LOW,    5,  5));

        int capacity = 20;
        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, capacity);
        List<BacklogItem> proposal = engine.generateProposal();

        int totalEffort = proposal.stream().mapToInt(BacklogItem::getEffortEstimate).sum();
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
        // Insert in intentionally reversed order to verify the engine sorts correctly
        productBacklog.addItem(new BacklogItem("Low task",    "", Priority.LOW,    3, 3));
        productBacklog.addItem(new BacklogItem("Medium task", "", Priority.MEDIUM, 4, 4));
        productBacklog.addItem(new BacklogItem("High task",   "", Priority.HIGH,   5, 5));

        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, 20);
        List<BacklogItem> proposal = engine.generateProposal();

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
        productBacklog.addItem(new BacklogItem("Low task",    "", Priority.LOW,    2, 2));
        productBacklog.addItem(new BacklogItem("Medium task", "", Priority.MEDIUM, 3, 3));

        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, 20);
        List<BacklogItem> proposal = engine.generateProposal();

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
        productBacklog.addItem(new BacklogItem("Giant task", "", Priority.HIGH, 25, 25));

        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, 20);
        List<BacklogItem> proposal = engine.generateProposal();

        assertTrue(proposal.isEmpty(),
            "REQ-6: An item with effort > capacity must not appear in the proposal.");
    }

    /**
     * [REQ-6]
     * Zero-capacity sprint must produce an empty proposal.
     */
    @Test @Order(64)
    void req6_zeroCapacityProducesEmptyProposal() {
        productBacklog.addItem(new BacklogItem("Any task", "", Priority.HIGH, 1, 1));

        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, 0);
        assertTrue(engine.generateProposal().isEmpty(),
            "REQ-6: Sprint capacity of 0 must produce an empty proposal.");
    }

    /**
     * [REQ-6]
     * Empty Product Backlog must produce an empty proposal.
     */
    @Test @Order(65)
    void req6_emptyBacklogProducesEmptyProposal() {
        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, 20);
        assertTrue(engine.generateProposal().isEmpty(),
            "REQ-6: Empty Product Backlog must produce an empty proposal.");
    }

    // ----------------------------------------------------------------
    // REQ-7 : Developer can modify proposed list before finalisation
    // ----------------------------------------------------------------

    /**
     * [REQ-7]
     * A developer must be able to add an item to the pending Sprint proposal.
     */
    @Test @Order(70)
    void req7_developerCanAddItemToProposal() {
        BacklogItem item = new BacklogItem("Extra story", "", Priority.MEDIUM, 3, 3);
        productBacklog.addItem(item);

        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, 20);
        engine.generateProposal();
        engine.addToPendingProposal(item.getId());

        assertTrue(engine.getPendingProposal().stream()
                .anyMatch(i -> i.getId().equals(item.getId())),
            "REQ-7: Developer must be able to add an item to the pending proposal.");
    }

    /**
     * [REQ-7]
     * A developer must be able to remove an item from the pending Sprint proposal.
     */
    @Test @Order(71)
    void req7_developerCanRemoveItemFromProposal() {
        BacklogItem item = new BacklogItem("Removable story", "", Priority.HIGH, 5, 5);
        productBacklog.addItem(item);

        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, 20);
        engine.generateProposal();
        engine.removeFromPendingProposal(item.getId());

        assertFalse(engine.getPendingProposal().stream()
                .anyMatch(i -> i.getId().equals(item.getId())),
            "REQ-7: Developer must be able to remove an item from the pending proposal.");
    }

    /**
     * [REQ-7]
     * Pending proposal edits must not remove items from the Product Backlog itself.
     */
    @Test @Order(72)
    void req7_proposalEditsDoNotMutateProductBacklog() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 4, 4);
        productBacklog.addItem(item);
        int sizeBefore = productBacklog.getItems().size();

        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, 20);
        engine.generateProposal();
        engine.removeFromPendingProposal(item.getId());

        assertEquals(sizeBefore, productBacklog.getItems().size(),
            "REQ-7: Removing from the pending proposal must not remove the item from the Product Backlog.");
    }


    // ================================================================
    // SECTION 4 — SPRINT APPROVAL  (REQ-8)
    // ================================================================

    /**
     * [REQ-8] [TC: approveSprint]
     * Approved items must move into the Sprint Backlog and become locked.
     */
    @Test @Order(80)
    void req8_tc_approveSprint_itemsMovedAndLocked() {
        BacklogItem item = new BacklogItem("Sprint story", "", Priority.HIGH, 5, 5);
        productBacklog.addItem(item);

        sprintBacklog.approve(List.of(item));

        assertTrue(sprintBacklog.contains(item.getId()),
            "REQ-8 [TC:approveSprint]: Approved item must be in the Sprint Backlog.");
        assertTrue(sprintBacklog.isLocked(item.getId()),
            "REQ-8 [TC:approveSprint]: Approved item must be locked.");
    }

    /**
     * [REQ-8] [TC: rejectSprint]
     * Rejected items must remain in the Product Backlog and stay editable.
     */
    @Test @Order(81)
    void req8_tc_rejectSprint_itemsRemainEditable() {
        BacklogItem item = new BacklogItem("Proposed story", "", Priority.HIGH, 5, 5);
        productBacklog.addItem(item);

        sprintBacklog.reject(List.of(item));

        assertFalse(sprintBacklog.contains(item.getId()),
            "REQ-8 [TC:rejectSprint]: Rejected item must not enter the Sprint Backlog.");
        assertTrue(productBacklog.contains(item.getId()),
            "REQ-8 [TC:rejectSprint]: Rejected item must remain in the Product Backlog.");
        assertFalse(productBacklog.isLocked(item.getId()),
            "REQ-8 [TC:rejectSprint]: Rejected item must remain editable (not locked).");
    }

    /**
     * [REQ-8]
     * After rejection the proposal must return to an editable state.
     */
    @Test @Order(82)
    void req8_afterRejectionProposalIsEditable() {
        BacklogItem item = new BacklogItem("Re-proposed story", "", Priority.HIGH, 4, 4);
        productBacklog.addItem(item);

        SprintProposalEngine engine = new SprintProposalEngine(productBacklog, 20);
        engine.generateProposal();
        sprintBacklog.reject(engine.getPendingProposal());

        assertTrue(engine.isEditable(),
            "REQ-8: After rejection the pending proposal must return to an editable state.");
    }

    /**
     * [REQ-8]
     * Approving multiple items must lock every one of them.
     */
    @Test @Order(83)
    void req8_multipleItemsAllLockedOnApproval() {
        BacklogItem i1 = new BacklogItem("Story 1", "", Priority.HIGH,   4, 4);
        BacklogItem i2 = new BacklogItem("Story 2", "", Priority.MEDIUM, 3, 3);
        productBacklog.addItem(i1);
        productBacklog.addItem(i2);

        sprintBacklog.approve(List.of(i1, i2));

        assertTrue(sprintBacklog.isLocked(i1.getId()), "REQ-8: Story 1 must be locked.");
        assertTrue(sprintBacklog.isLocked(i2.getId()), "REQ-8: Story 2 must be locked.");
    }


    // ================================================================
    // SECTION 5 — SPRINT BACKLOG MANAGEMENT  (REQ-9, REQ-10)
    // ================================================================

    // ----------------------------------------------------------------
    // REQ-9 : Items locked during active sprint
    // ----------------------------------------------------------------

    /**
     * [REQ-9]
     * Attempting to modify a locked Sprint Backlog item must fail.
     */
    @Test @Order(90)
    void req9_sprintItemsLockedDuringActiveSprint() {
        BacklogItem item = new BacklogItem("Locked story", "", Priority.HIGH, 5, 5);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        boolean edited = sprintBacklog.tryUpdateItem(item.getId(), Priority.LOW, 2, 2);

        assertFalse(edited,
            "REQ-9: Editing a Sprint Backlog item while the sprint is active must be rejected.");
    }

    /**
     * [REQ-9]
     * The sprint-active flag must be true after approval.
     */
    @Test @Order(91)
    void req9_sprintIsActiveAfterApproval() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 3, 3);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        assertTrue(sprintBacklog.isActive(),
            "REQ-9: Sprint must be flagged active once items are approved.");
    }

    /**
     * [REQ-9]
     * A fresh Sprint Backlog must not be active.
     */
    @Test @Order(92)
    void req9_freshSprintIsNotActive() {
        assertFalse(sprintBacklog.isActive(),
            "REQ-9: A newly created Sprint Backlog must not be active.");
    }

    // ----------------------------------------------------------------
    // REQ-10 : Unfinished items returned at sprint end  [TC: returnUnfinishedItems]
    // ----------------------------------------------------------------

    /**
     * [REQ-10] [TC: returnUnfinishedItems]
     * Unfinished items must return to the Product Backlog at sprint end,
     * retaining their original priority and receiving a revised effort estimate.
     */
    @Test @Order(100)
    void req10_tc_returnUnfinishedItems_toProductBacklog() {
        BacklogItem item = new BacklogItem("Unfinished story", "", Priority.HIGH, 6, 6);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        sprintBacklog.markUnfinished(item.getId(), 4 /* revised effort */);
        sprintBacklog.endSprint(productBacklog);

        assertTrue(productBacklog.contains(item.getId()),
            "REQ-10 [TC:returnUnfinishedItems]: Unfinished item must be back in Product Backlog.");
        BacklogItem returned = productBacklog.findById(item.getId());
        assertEquals(Priority.HIGH, returned.getPriority(),
            "REQ-10 [TC:returnUnfinishedItems]: Original priority must be preserved.");
        assertEquals(4, returned.getEffortEstimate(),
            "REQ-10 [TC:returnUnfinishedItems]: Revised effort estimate must be applied.");
        assertFalse(sprintBacklog.contains(item.getId()),
            "REQ-10 [TC:returnUnfinishedItems]: Item must be removed from the Sprint Backlog.");
    }

    /**
     * [REQ-10]
     * Completed items must NOT be returned to the Product Backlog at sprint end.
     */
    @Test @Order(101)
    void req10_finishedItemsNotReturnedAtSprintEnd() {
        BacklogItem item = new BacklogItem("Finished story", "", Priority.HIGH, 5, 5);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        sprintBacklog.markComplete(item.getId());
        sprintBacklog.endSprint(productBacklog);

        assertFalse(productBacklog.contains(item.getId()),
            "REQ-10: Completed items must not be re-added to the Product Backlog.");
    }

    /**
     * [REQ-10]
     * Sprint must be inactive after endSprint() is called.
     */
    @Test @Order(102)
    void req10_sprintInactiveAfterEnd() {
        BacklogItem item = new BacklogItem("Story", "", Priority.HIGH, 3, 3);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));
        sprintBacklog.markComplete(item.getId());
        sprintBacklog.endSprint(productBacklog);

        assertFalse(sprintBacklog.isActive(),
            "REQ-10: Sprint must be inactive after endSprint().");
    }

    /**
     * [REQ-10]
     * In a mixed sprint, only unfinished items return to the Product Backlog.
     */
    @Test @Order(103)
    void req10_mixedSprintOnlyUnfinishedReturned() {
        BacklogItem done   = new BacklogItem("Done",       "", Priority.HIGH,   4, 4);
        BacklogItem undone = new BacklogItem("Not done",   "", Priority.MEDIUM, 3, 3);
        productBacklog.addItem(done);
        productBacklog.addItem(undone);
        sprintBacklog.approve(List.of(done, undone));

        sprintBacklog.markComplete(done.getId());
        sprintBacklog.markUnfinished(undone.getId(), 2);
        sprintBacklog.endSprint(productBacklog);

        assertFalse(productBacklog.contains(done.getId()),
            "REQ-10: Completed item must not reappear in Product Backlog.");
        assertTrue(productBacklog.contains(undone.getId()),
            "REQ-10: Unfinished item must be returned to Product Backlog.");
    }


    // ================================================================
    // SECTION 6 — ENGINEERING TASKS  (REQ-11, REQ-12, REQ-13)
    // ================================================================

    // ----------------------------------------------------------------
    // REQ-11 : Expand Sprint items into EngineeringTasks  [TC: createETasks]
    // ----------------------------------------------------------------

    /**
     * [REQ-11] [TC: createETasks]
     * An engineering task must be linked to its parent Sprint Backlog item.
     */
    @Test @Order(110)
    void req11_tc_createETasks_linkedToParent() {
        BacklogItem parent = new BacklogItem("Login feature", "", Priority.HIGH, 5, 5);
        productBacklog.addItem(parent);
        sprintBacklog.approve(List.of(parent));

        EngineeringTask task = new EngineeringTask("Build login form", parent.getId(), 3);
        sprintBacklog.addEngineeringTask(task);

        List<EngineeringTask> tasks = sprintBacklog.getEngineeringTasksFor(parent.getId());
        assertEquals(1, tasks.size(),
            "REQ-11 [TC:createETasks]: One engineering task must be linked to the parent.");
        assertEquals("Build login form", tasks.get(0).getTitle(),
            "REQ-11 [TC:createETasks]: Task title must match.");
    }

    /**
     * [REQ-11]
     * Multiple engineering tasks may be linked to one Sprint Backlog item.
     */
    @Test @Order(111)
    void req11_multipleTasksPerSprintItem() {
        BacklogItem parent = new BacklogItem("Complex feature", "", Priority.HIGH, 10, 10);
        productBacklog.addItem(parent);
        sprintBacklog.approve(List.of(parent));

        sprintBacklog.addEngineeringTask(new EngineeringTask("Sub-task 1", parent.getId(), 3));
        sprintBacklog.addEngineeringTask(new EngineeringTask("Sub-task 2", parent.getId(), 3));
        sprintBacklog.addEngineeringTask(new EngineeringTask("Sub-task 3", parent.getId(), 4));

        assertEquals(3, sprintBacklog.getEngineeringTasksFor(parent.getId()).size(),
            "REQ-11: Three engineering tasks must all be linked to the same parent.");
    }

    /**
     * [REQ-11]
     * Engineering tasks for one parent must not appear under a different parent.
     */
    @Test @Order(112)
    void req11_tasksDoNotCrossParents() {
        BacklogItem parentA = new BacklogItem("Feature A", "", Priority.HIGH,   5, 5);
        BacklogItem parentB = new BacklogItem("Feature B", "", Priority.MEDIUM, 5, 5);
        productBacklog.addItem(parentA);
        productBacklog.addItem(parentB);
        sprintBacklog.approve(List.of(parentA, parentB));

        sprintBacklog.addEngineeringTask(new EngineeringTask("Task for A", parentA.getId(), 2));

        assertTrue(sprintBacklog.getEngineeringTasksFor(parentB.getId()).isEmpty(),
            "REQ-11: Tasks belonging to parentA must not appear under parentB.");
    }

    // ----------------------------------------------------------------
    // REQ-12 : Priority inheritance and propagation
    // ----------------------------------------------------------------

    /**
     * [REQ-12]
     * A newly created engineering task must inherit the priority of its parent.
     */
    @Test @Order(120)
    void req12_newTaskInheritsParentPriority() {
        BacklogItem parent = new BacklogItem("High-pri story", "", Priority.HIGH, 4, 4);
        productBacklog.addItem(parent);
        sprintBacklog.approve(List.of(parent));

        EngineeringTask task = new EngineeringTask("Sub-task", parent.getId(), 2);
        sprintBacklog.addEngineeringTask(task);

        assertEquals(Priority.HIGH, task.getPriority(),
            "REQ-12: New engineering task must inherit HIGH priority from parent.");
    }

    /**
     * [REQ-12]
     * When the parent BacklogItem's priority changes, all child tasks must be updated.
     */
    @Test @Order(121)
    void req12_priorityChangePropagatestoAllChildren() {
        BacklogItem parent = new BacklogItem("Propagation story", "", Priority.LOW, 4, 4);
        productBacklog.addItem(parent);
        sprintBacklog.approve(List.of(parent));

        EngineeringTask t1 = new EngineeringTask("Child 1", parent.getId(), 2);
        EngineeringTask t2 = new EngineeringTask("Child 2", parent.getId(), 2);
        sprintBacklog.addEngineeringTask(t1);
        sprintBacklog.addEngineeringTask(t2);

        parent.setPriority(Priority.HIGH);
        sprintBacklog.propagatePriority(parent);

        assertEquals(Priority.HIGH,
            sprintBacklog.findEngineeringTaskById(t1.getId()).getPriority(),
            "REQ-12: Child 1 must reflect the updated priority.");
        assertEquals(Priority.HIGH,
            sprintBacklog.findEngineeringTaskById(t2.getId()).getPriority(),
            "REQ-12: Child 2 must reflect the updated priority.");
    }

    /**
     * [REQ-12]
     * Priority propagation must work in all directions (HIGH→LOW, LOW→MEDIUM, etc.).
     */
    @Test @Order(122)
    void req12_priorityPropagationAllDirections() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.HIGH, 3, 3);
        productBacklog.addItem(parent);
        sprintBacklog.approve(List.of(parent));
        EngineeringTask task = new EngineeringTask("Task", parent.getId(), 1);
        sprintBacklog.addEngineeringTask(task);

        parent.setPriority(Priority.LOW);
        sprintBacklog.propagatePriority(parent);
        assertEquals(Priority.LOW,
            sprintBacklog.findEngineeringTaskById(task.getId()).getPriority(),
            "REQ-12: HIGH→LOW propagation must work.");

        parent.setPriority(Priority.MEDIUM);
        sprintBacklog.propagatePriority(parent);
        assertEquals(Priority.MEDIUM,
            sprintBacklog.findEngineeringTaskById(task.getId()).getPriority(),
            "REQ-12: LOW→MEDIUM propagation must work.");
    }

    // ----------------------------------------------------------------
    // REQ-13 : Independent effort estimates  [TC: editETasks, removeETasks]
    // ----------------------------------------------------------------

    /**
     * [REQ-13] [TC: editETasks]
     * An engineering task's title and effort estimate must be independently updatable.
     */
    @Test @Order(130)
    void req13_tc_editETasks_titleAndEffortUpdated() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.MEDIUM, 5, 5);
        productBacklog.addItem(parent);
        sprintBacklog.approve(List.of(parent));

        EngineeringTask task = new EngineeringTask("Original title", parent.getId(), 3);
        sprintBacklog.addEngineeringTask(task);

        task.setTitle("Updated title");
        task.setEffortEstimate(2);
        sprintBacklog.updateEngineeringTask(task);

        EngineeringTask updated = sprintBacklog.findEngineeringTaskById(task.getId());
        assertEquals("Updated title", updated.getTitle(),
            "REQ-13 [TC:editETasks]: Task title must be updated.");
        assertEquals(2, updated.getEffortEstimate(),
            "REQ-13 [TC:editETasks]: Effort estimate must be updated.");
    }

    /**
     * [REQ-13]
     * Two sibling tasks under the same parent may carry different effort estimates.
     */
    @Test @Order(131)
    void req13_siblingTasksHaveIndependentEffort() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.HIGH, 10, 10);
        productBacklog.addItem(parent);
        sprintBacklog.approve(List.of(parent));

        EngineeringTask t1 = new EngineeringTask("Task 1", parent.getId(), 3);
        EngineeringTask t2 = new EngineeringTask("Task 2", parent.getId(), 5);
        sprintBacklog.addEngineeringTask(t1);
        sprintBacklog.addEngineeringTask(t2);

        assertNotEquals(t1.getEffortEstimate(), t2.getEffortEstimate(),
            "REQ-13: Sibling tasks must be able to carry independent effort estimates.");
    }

    /**
     * [REQ-13] [TC: removeETasks]
     * Removing an engineering task must delete it from the parent item's task list.
     */
    @Test @Order(132)
    void req13_tc_removeETasks_taskDeleted() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.LOW, 3, 3);
        productBacklog.addItem(parent);
        sprintBacklog.approve(List.of(parent));

        EngineeringTask task = new EngineeringTask("Task to remove", parent.getId(), 1);
        sprintBacklog.addEngineeringTask(task);
        assertFalse(sprintBacklog.getEngineeringTasksFor(parent.getId()).isEmpty(),
            "Pre-condition: task must exist before removal.");

        sprintBacklog.removeEngineeringTask(task.getId());

        assertTrue(sprintBacklog.getEngineeringTasksFor(parent.getId()).isEmpty(),
            "REQ-13 [TC:removeETasks]: Task list must be empty after removal.");
    }

    /**
     * [REQ-13]
     * Removing one task must not affect sibling tasks under the same parent.
     */
    @Test @Order(133)
    void req13_removeOneTaskLeavesOthersIntact() {
        BacklogItem parent = new BacklogItem("Story", "", Priority.HIGH, 8, 8);
        productBacklog.addItem(parent);
        sprintBacklog.approve(List.of(parent));

        EngineeringTask t1 = new EngineeringTask("Keep me",   parent.getId(), 2);
        EngineeringTask t2 = new EngineeringTask("Remove me", parent.getId(), 3);
        sprintBacklog.addEngineeringTask(t1);
        sprintBacklog.addEngineeringTask(t2);

        sprintBacklog.removeEngineeringTask(t2.getId());

        List<EngineeringTask> remaining = sprintBacklog.getEngineeringTasksFor(parent.getId());
        assertEquals(1, remaining.size(),
            "REQ-13: Only one task should remain after removing t2.");
        assertEquals(t1.getId(), remaining.get(0).getId(),
            "REQ-13: The surviving task must be t1.");
    }

    // ----------------------------------------------------------------
    // REQ-11 : Complete / Delayed status  [TC: completeBacklogItem]
    // ----------------------------------------------------------------

    /**
     * [REQ-11] [TC: completeBacklogItem]
     * Marking a Sprint Backlog item complete must set its status to COMPLETE.
     */
    @Test @Order(140)
    void req11_tc_completeBacklogItem_statusSetComplete() {
        BacklogItem item = new BacklogItem("Done story", "", Priority.HIGH, 4, 4);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        sprintBacklog.markComplete(item.getId());

        assertTrue(sprintBacklog.isComplete(item.getId()),
            "REQ-11 [TC:completeBacklogItem]: Item must be flagged COMPLETE.");
    }

    /**
     * [REQ-11] [TC: completeBacklogItem — delayed variant]
     * Marking a Sprint Backlog item delayed must set its status to DELAYED.
     */
    @Test @Order(141)
    void req11_tc_completeBacklogItem_statusSetDelayed() {
        BacklogItem item = new BacklogItem("Delayed story", "", Priority.MEDIUM, 5, 5);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        sprintBacklog.markDelayed(item.getId());

        assertTrue(sprintBacklog.isDelayed(item.getId()),
            "REQ-11 [TC:completeBacklogItem(delayed)]: Item must be flagged DELAYED.");
    }

    /**
     * [REQ-11]
     * A completed item must not simultaneously be flagged as delayed.
     */
    @Test @Order(142)
    void req11_completedItemNotAlsoDelayed() {
        BacklogItem item = new BacklogItem("Done story", "", Priority.HIGH, 4, 4);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));
        sprintBacklog.markComplete(item.getId());

        assertFalse(sprintBacklog.isDelayed(item.getId()),
            "REQ-11: A completed item must not also be flagged as delayed.");
    }


    // ================================================================
    // SECTION 7 — PROGRESS TRACKING  (REQ-14)
    // ================================================================

    /**
     * [REQ-14] [TC: logTime]
     * Logging actual time and effort must be stored and remaining effort updated correctly.
     */
    @Test @Order(150)
    void req14_tc_logTime_remainingEffortUpdated() {
        BacklogItem item = new BacklogItem("Tracked story", "", Priority.HIGH, 8, 8);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        sprintBacklog.logProgress(item.getId(), 3 /* actual hours */, 3 /* actual effort */);

        ProgressLog log = sprintBacklog.getProgressLog(item.getId());
        assertEquals(3, log.getActualTime(),
            "REQ-14 [TC:logTime]: Actual time must be 3.");
        assertEquals(3, log.getActualEffort(),
            "REQ-14 [TC:logTime]: Actual effort must be 3.");
        assertEquals(5, log.getRemainingEffort(),
            "REQ-14 [TC:logTime]: Remaining effort must be 8 − 3 = 5.");
    }

    /**
     * [REQ-14]
     * Multiple logProgress calls must accumulate actual time and effort correctly.
     */
    @Test @Order(151)
    void req14_logTimeAccumulatesAcrossMultipleCalls() {
        BacklogItem item = new BacklogItem("Multi-log story", "", Priority.MEDIUM, 10, 10);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        sprintBacklog.logProgress(item.getId(), 2, 2);
        sprintBacklog.logProgress(item.getId(), 3, 3);

        ProgressLog log = sprintBacklog.getProgressLog(item.getId());
        assertEquals(5, log.getActualTime(),   "REQ-14: Accumulated actual time must be 5.");
        assertEquals(5, log.getActualEffort(), "REQ-14: Accumulated actual effort must be 5.");
        assertEquals(5, log.getRemainingEffort(), "REQ-14: Remaining effort must be 10−5=5.");
    }

    /**
     * [REQ-14]
     * Before any progress is logged, remaining effort must equal the original estimate.
     */
    @Test @Order(152)
    void req14_initialRemainingEffortEqualsEstimate() {
        BacklogItem item = new BacklogItem("Fresh story", "", Priority.HIGH, 6, 6);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        ProgressLog log = sprintBacklog.getProgressLog(item.getId());
        assertEquals(6, log.getRemainingEffort(),
            "REQ-14: Before any logging, remaining effort must equal the original estimate.");
    }

    /**
     * [REQ-14]
     * Logging time on one item must not affect the progress log of another item.
     */
    @Test @Order(153)
    void req14_logTimeDoesNotCrossItems() {
        BacklogItem a = new BacklogItem("Story A", "", Priority.HIGH,   5, 5);
        BacklogItem b = new BacklogItem("Story B", "", Priority.MEDIUM, 4, 4);
        productBacklog.addItem(a);
        productBacklog.addItem(b);
        sprintBacklog.approve(List.of(a, b));

        sprintBacklog.logProgress(a.getId(), 3, 3);

        ProgressLog logB = sprintBacklog.getProgressLog(b.getId());
        assertEquals(0, logB.getActualTime(),
            "REQ-14: Logging time on Story A must not affect Story B's actual time.");
        assertEquals(4, logB.getRemainingEffort(),
            "REQ-14: Story B's remaining effort must be unchanged.");
    }


    // ================================================================
    // SECTION 8 — CHARTS  (REQ-15)
    // ================================================================

    /**
     * [REQ-15] [TC: displayVelocity]
     * Velocity must equal the sum of completed items' effort for the sprint.
     */
    @Test @Order(160)
    void req15_tc_displayVelocity_equalsCompletedEffort() {
        BacklogItem i1 = new BacklogItem("Story 1", "", Priority.HIGH,   5, 5);
        BacklogItem i2 = new BacklogItem("Story 2", "", Priority.MEDIUM, 4, 4);
        productBacklog.addItem(i1);
        productBacklog.addItem(i2);
        sprintBacklog.approve(List.of(i1, i2));

        sprintBacklog.markComplete(i1.getId());
        sprintBacklog.markComplete(i2.getId());
        sprintBacklog.endSprint(productBacklog);

        ChartDataProvider chart = new ChartDataProvider(sprintBacklog);
        assertEquals(9, chart.getVelocityForLastSprint(),
            "REQ-15 [TC:displayVelocity]: Velocity must be 5+4=9.");
    }

    /**
     * [REQ-15]
     * Velocity must be 0 when no items were completed.
     */
    @Test @Order(161)
    void req15_velocityZeroWhenNothingCompleted() {
        BacklogItem item = new BacklogItem("Incomplete", "", Priority.HIGH, 6, 6);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));
        sprintBacklog.endSprint(productBacklog);

        ChartDataProvider chart = new ChartDataProvider(sprintBacklog);
        assertEquals(0, chart.getVelocityForLastSprint(),
            "REQ-15: Velocity must be 0 when no items were completed.");
    }

    /**
     * [REQ-15]
     * Velocity must count only COMPLETED items, not delayed or unfinished ones.
     */
    @Test @Order(162)
    void req15_velocityExcludesDelayedAndUnfinishedItems() {
        BacklogItem done    = new BacklogItem("Done",       "", Priority.HIGH,   5, 5);
        BacklogItem delayed = new BacklogItem("Delayed",    "", Priority.MEDIUM, 3, 3);
        BacklogItem undone  = new BacklogItem("Unfinished", "", Priority.LOW,    4, 4);
        productBacklog.addItem(done);
        productBacklog.addItem(delayed);
        productBacklog.addItem(undone);
        sprintBacklog.approve(List.of(done, delayed, undone));

        sprintBacklog.markComplete(done.getId());
        sprintBacklog.markDelayed(delayed.getId());
        sprintBacklog.markUnfinished(undone.getId(), 2);
        sprintBacklog.endSprint(productBacklog);

        ChartDataProvider chart = new ChartDataProvider(sprintBacklog);
        assertEquals(5, chart.getVelocityForLastSprint(),
            "REQ-15: Only the completed item (effort=5) should count toward velocity.");
    }

    /**
     * [REQ-15]
     * Burndown data must decrease as work is logged against sprint items.
     */
    @Test @Order(163)
    void req15_burndownDataDecreasesAsWorkLogged() {
        BacklogItem item = new BacklogItem("Burndown story", "", Priority.HIGH, 10, 10);
        productBacklog.addItem(item);
        sprintBacklog.approve(List.of(item));

        ChartDataProvider chart = new ChartDataProvider(sprintBacklog);
        int before = chart.getTotalRemainingEffort();

        sprintBacklog.logProgress(item.getId(), 4, 4);

        int after = chart.getTotalRemainingEffort();
        assertTrue(after < before,
            "REQ-15: Total remaining effort must decrease after logging progress.");
        assertEquals(6, after,
            "REQ-15: Remaining effort after logging 4 of 10 must be 6.");
    }

    /**
     * [REQ-15]
     * Burndown total at sprint start must equal the sum of all items' effort estimates.
     */
    @Test @Order(164)
    void req15_burndownTotalAtSprintStartEqualsAllEffort() {
        BacklogItem i1 = new BacklogItem("Story 1", "", Priority.HIGH,   5, 5);
        BacklogItem i2 = new BacklogItem("Story 2", "", Priority.MEDIUM, 3, 3);
        productBacklog.addItem(i1);
        productBacklog.addItem(i2);
        sprintBacklog.approve(List.of(i1, i2));

        ChartDataProvider chart = new ChartDataProvider(sprintBacklog);
        assertEquals(8, chart.getTotalRemainingEffort(),
            "REQ-15: Burndown starting total must be 5+3=8.");
    }


    // ================================================================
    // SECTION 9 — AUTHENTICATION  (REQ-16)
    // ================================================================

    /**
     * [REQ-16] [TC: loginUserDashboard]
     * Scrum Team member credentials must resolve to SCRUM_TEAM role.
     */
    @Test @Order(170)
    void req16_tc_loginUserDashboard_scrumTeamRole() {
        assertEquals(Role.SCRUM_TEAM, authService.login("team_member", "pass1"),
            "REQ-16 [TC:loginUserDashboard]: Must resolve to SCRUM_TEAM.");
    }

    /**
     * [REQ-16] [TC: loginScrumMasterDashboard]
     * Scrum Master credentials must resolve to SCRUM_MASTER role.
     */
    @Test @Order(171)
    void req16_tc_loginScrumMasterDashboard_scrumMasterRole() {
        assertEquals(Role.SCRUM_MASTER, authService.login("scrum_master", "pass2"),
            "REQ-16 [TC:loginScrumMasterDashboard]: Must resolve to SCRUM_MASTER.");
    }

    /**
     * [REQ-16] [TC: loginProductOwnerDashboard]
     * Product Owner credentials must resolve to PRODUCT_OWNER role.
     */
    @Test @Order(172)
    void req16_tc_loginProductOwnerDashboard_productOwnerRole() {
        assertEquals(Role.PRODUCT_OWNER, authService.login("product_owner", "pass3"),
            "REQ-16 [TC:loginProductOwnerDashboard]: Must resolve to PRODUCT_OWNER.");
    }

    /**
     * [REQ-16]
     * Wrong password must return null (authentication failure).
     */
    @Test @Order(173)
    void req16_wrongPasswordReturnsNull() {
        assertNull(authService.login("product_owner", "wrong_password"),
            "REQ-16: Wrong password must return null.");
    }

    /**
     * [REQ-16]
     * Unknown username must return null.
     */
    @Test @Order(174)
    void req16_unknownUsernameReturnsNull() {
        assertNull(authService.login("nobody", "pass1"),
            "REQ-16: Unknown username must return null.");
    }

    /**
     * [REQ-16]
     * Empty username must return null.
     */
    @Test @Order(175)
    void req16_emptyUsernameReturnsNull() {
        assertNull(authService.login("", "pass1"),
            "REQ-16: Empty username must return null.");
    }

    /**
     * [REQ-16]
     * Empty password must return null.
     */
    @Test @Order(176)
    void req16_emptyPasswordReturnsNull() {
        assertNull(authService.login("team_member", ""),
            "REQ-16: Empty password must return null.");
    }

    /**
     * [REQ-16]
     * Credentials must be case-sensitive — wrong-case username must not authenticate.
     */
    @Test @Order(177)
    void req16_credentialsAreCaseSensitive() {
        assertNull(authService.login("Team_Member", "pass1"),
            "REQ-16: Authentication must be case-sensitive.");
    }


    // ================================================================
    // SECTION 10 — ROLE-BASED ROUTING  (REQ-17)
    // ================================================================

    /**
     * [REQ-17]
     * SCRUM_TEAM role must route to the Scrum Team view.
     */
    @Test @Order(180)
    void req17_scrumTeamRoutedToCorrectView() {
        Role role = authService.login("team_member", "pass1");
        assertEquals("SCRUM_TEAM_VIEW", authService.resolveView(role),
            "REQ-17: SCRUM_TEAM role must route to SCRUM_TEAM_VIEW.");
    }

    /**
     * [REQ-17]
     * SCRUM_MASTER role must route to the Scrum Master view.
     */
    @Test @Order(181)
    void req17_scrumMasterRoutedToCorrectView() {
        Role role = authService.login("scrum_master", "pass2");
        assertEquals("SCRUM_MASTER_VIEW", authService.resolveView(role),
            "REQ-17: SCRUM_MASTER role must route to SCRUM_MASTER_VIEW.");
    }

    /**
     * [REQ-17]
     * PRODUCT_OWNER role must route to the Product Owner view.
     */
    @Test @Order(182)
    void req17_productOwnerRoutedToCorrectView() {
        Role role = authService.login("product_owner", "pass3");
        assertEquals("PRODUCT_OWNER_VIEW", authService.resolveView(role),
            "REQ-17: PRODUCT_OWNER role must route to PRODUCT_OWNER_VIEW.");
    }

    /**
     * [REQ-17]
     * A null role (unauthenticated) must route back to the login screen, not any dashboard.
     */
    @Test @Order(183)
    void req17_nullRoleRoutedToLoginView() {
        assertEquals("LOGIN_VIEW", authService.resolveView(null),
            "REQ-17: Unauthenticated (null) role must route to LOGIN_VIEW.");
    }
}
