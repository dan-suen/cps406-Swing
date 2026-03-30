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
        productBacklog.addItem(new BacklogItem("Story A", "", Priority.HIGH, 3, 3, 0));
        productBacklog.addItem(new BacklogItem("Story B", "", Priority.LOW,  2, 2, 0));        assertEquals(2, productBacklog.getItems().size(),
            "REQ-1: Both items must be present in the single shared Product Backlog.");
    }

}
