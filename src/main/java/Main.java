import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import model.*;
import model.BacklogItem.Priority;
import model.BacklogItem.Status;
import model.User.Role;

public class Main {

    private static final File PROPOSALS_DIR = new File("proposals");

    public static void main(String[] args) {
        PROPOSALS_DIR.mkdirs(); // create proposals/ folder if it doesn't exist
        SwingUtilities.invokeLater(() -> {
            ScrumProject project = new ScrumProject();
            project.load();
            // DEMO ONLY: seed sample backlog items on first run so the app is not empty
            if (project.getProductBacklog().getAllItems().isEmpty()) seedDemoData(project);
            showLogin(project);
        });
    }

    // -------------------------------------------------------------------------
    // DEMO ONLY: seed sample backlog items so the app is not empty on first run.
    // This method is called once when no backlog data exists (data/backlog.txt absent).
    // Remove or disable this method once real data has been entered.
    // -------------------------------------------------------------------------
    private static void seedDemoData(ScrumProject project) {
        ProductBacklog pb = project.getProductBacklog();

        // DEMO: High-priority backlog items — will be committed into Sprint 1
        BacklogItem login = new BacklogItem("User Login",
            "As a user I can log in with username and password.",
            Priority.HIGH, 4.0, 5.0, 2.0);
        BacklogItem backlogView = new BacklogItem("Product Backlog View",
            "As a SM I can view and manage all backlog items.",
            Priority.HIGH, 6.0, 8.0, 1.5);
        BacklogItem sprintGen = new BacklogItem("Sprint Proposal Generation",
            "As a SM I can auto-generate a sprint proposal from the backlog.",
            Priority.HIGH, 5.0, 6.0, 2.0);

        // DEMO: Medium-priority backlog items — remain in product backlog for Sprint 2
        pb.addItem(new BacklogItem("PO Approval Workflow",
            "As a PO I can approve or reject sprint proposals via file notifications.",
            Priority.MEDIUM, 4.0, 5.0, 1.0));
        pb.addItem(new BacklogItem("Team Task Breakdown",
            "As a team member I can break sprint items into engineering sub-tasks.",
            Priority.MEDIUM, 3.0, 4.0, 1.0));
        pb.addItem(new BacklogItem("Velocity Tracking",
            "As a SM I can view planned vs completed effort across all sprints.",
            Priority.MEDIUM, 3.0, 3.0, 0.5));

        // DEMO: Low-priority backlog items — remain in product backlog
        pb.addItem(new BacklogItem("Burndown Chart",
            "As a SM I can view a burndown chart for the active sprint.",
            Priority.LOW, 4.0, 4.0, 1.0));
        pb.addItem(new BacklogItem("User Registration",
            "As an admin I can register new users with a role.",
            Priority.LOW, 2.0, 2.0, 0.5));

        // DEMO: Create Sprint 1 and add the high-priority items as proposed
        pb.addItem(login);
        pb.addItem(backlogView);
        pb.addItem(sprintGen);
        SprintBacklog sprint1 = project.createSprint(30.0);
        sprint1.setProposedItems(java.util.Arrays.asList(login, backlogView, sprintGen));

        // DEMO: Approve and start Sprint 1 — moves items to committed, sets start date to today
        sprint1.approve();
        sprint1.startSprint();

        // DEMO: Mark "User Login" complete so velocity > 0
        login.setStatus(Status.COMPLETE);

        // DEMO: Inject burndown history for past days so the chart has a visible trend.
        // Day 0 = sprint start (full planned effort = 19h), declining each day.
        sprint1.recordBurndown(0, 19.0);
        sprint1.recordBurndown(1, 16.0);
        sprint1.recordBurndown(2, 13.5);
        sprint1.recordBurndown(3, 10.0);
        sprint1.recordBurndown(4, 8.0);
        sprint1.recordBurndown(5, 5.0);

        // DEMO: persist all seeded data immediately
        project.save();
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------
    private static void showLogin(ScrumProject project) {
        JFrame frame = new JFrame("Scrum Tool — Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 280);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill = GridBagConstraints.HORIZONTAL;

        //Login objects
        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JButton loginBtn    = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        JLabel errLabel = new JLabel(" ");
        errLabel.setForeground(Color.RED);

        //Add login objects to panel
        g.gridx = 0; g.gridy = 0; panel.add(new JLabel("Username:"), g);
        g.gridx = 1; panel.add(userField, g);
        g.gridx = 0; g.gridy = 1; panel.add(new JLabel("Password:"), g);
        g.gridx = 1; panel.add(passField, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; panel.add(loginBtn, g);
        g.gridy = 3; panel.add(registerBtn, g);
        g.gridy = 4; panel.add(errLabel, g);

        //Login actions
        ActionListener doLogin = e -> {
            User u = project.login(userField.getText().trim(), new String(passField.getPassword()));
            if (u != null) { frame.dispose(); showMain(project); }
            else { errLabel.setText("Invalid credentials."); passField.setText(""); }
        };
        loginBtn.addActionListener(doLogin);
        passField.addActionListener(doLogin);
        registerBtn.addActionListener(e -> showRegisterDialog(frame, project));

        frame.setContentPane(panel);
        frame.setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Register dialog
    // -------------------------------------------------------------------------
    private static void showRegisterDialog(JFrame parent, ScrumProject project) {
        JTextField newUserField     = new JTextField(15);
        JPasswordField newPassField = new JPasswordField(15);
        JPasswordField confirmField = new JPasswordField(15);
        JComboBox<User.Role> roleBox = new JComboBox<>(User.Role.values());

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        form.add(new JLabel("Username:"));         form.add(newUserField);
        form.add(new JLabel("Password:"));         form.add(newPassField);
        form.add(new JLabel("Confirm password:")); form.add(confirmField);
        form.add(new JLabel("Role:"));             form.add(roleBox);

        int result = JOptionPane.showConfirmDialog(parent, form, "Register New User",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String username = newUserField.getText().trim();
        String password = new String(newPassField.getPassword());
        String confirm  = new String(confirmField.getPassword());

        if (username.isEmpty())        { msg(parent, "Username is required.");   return; }
        if (password.isEmpty())        { msg(parent, "Password is required.");   return; }
        if (!password.equals(confirm)) { msg(parent, "Passwords do not match."); return; }

        boolean ok = project.registerUser(username, password, (User.Role) roleBox.getSelectedItem());
        if (ok) msg(parent, "User \"" + username + "\" registered. You can now log in.");
        else    msg(parent, "Username \"" + username + "\" is already taken.");
    }

    // -------------------------------------------------------------------------
    // Main window
    // -------------------------------------------------------------------------
    private static void showMain(ScrumProject project) {
        User u = project.getCurrentUser();
        JFrame frame = new JFrame("Scrum Tool  —  " + u.getUsername() + "  [" + u.getRole() + "]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 660);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Product Backlog", buildProductTab(project));
        tabs.addTab("Sprint",          buildSprintTab(project));
        tabs.addTab("Velocity & Burndown", buildVelocityTab(project));
        if (u.getRole() == Role.SCRUM_TEAM) {
            tabs.addTab("My Tasks", buildMyTasksTab(project));
        }

        tabs.addChangeListener(e -> SwingUtilities.invokeLater(() -> {
            int i = tabs.getSelectedIndex();
            if (i == 1) { tabs.setComponentAt(1, buildSprintTab(project));   tabs.revalidate(); }
            if (i == 2) { tabs.setComponentAt(2, buildVelocityTab(project)); tabs.revalidate(); }
            if (i == 3 && u.getRole() == Role.SCRUM_TEAM) {
                tabs.setComponentAt(3, buildMyTasksTab(project)); tabs.revalidate();
            }
        }));

        JMenuBar mb = new JMenuBar();

        // Proposals menu — Product Owner only
        if (u.getRole() == Role.PRODUCT_OWNER) {
            JMenu proposalsMenu = new JMenu("Proposals");
            JMenuItem viewItem = new JMenuItem("View Pending Proposals");
            viewItem.addActionListener(e -> showProposalsDialog(frame, project, tabs));
            proposalsMenu.add(viewItem);
            mb.add(proposalsMenu);
        }

        JMenu userMenu = new JMenu(u.getUsername() + "  [" + u.getRole() + "]");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> { project.logout(); frame.dispose(); showLogin(project); });
        userMenu.add(logoutItem);
        mb.add(Box.createHorizontalGlue());
        mb.add(userMenu);
        frame.setJMenuBar(mb);

        frame.setContentPane(tabs);
        frame.setVisible(true);

        // Notify Product Owner of pending proposals on login
        if (u.getRole() == Role.PRODUCT_OWNER) {
            File[] pending = pendingProposalFiles();
            if (pending.length > 0) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(frame,
                        pending.length + " sprint proposal(s) are awaiting your approval.\n"
                        + "Go to  Proposals  →  View Pending Proposals.",
                        "Pending Proposals", JOptionPane.INFORMATION_MESSAGE));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Product Backlog tab
    // -------------------------------------------------------------------------
    private static JPanel buildProductTab(ScrumProject project) {
        Role role = project.getCurrentUser().getRole();
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = { "Title", "Description", "Priority", "Time Est. (h)", "Effort Est.", "Risk", "Status" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);

        //Rebuilds table on change
        Runnable refresh = () -> {
            model.setRowCount(0);
            for (BacklogItem item : project.getProductBacklog().getAllItems()) {
                model.addRow(new Object[]{
                    item.getTitle(), item.getDescription(), item.getPriority(),
                    item.getTimeEstimate(), item.getEffortEstimate(), item.getRiskLevel(), item.getStatus()
                });
            }
        };
        refresh.run();

        JButton addBtn    = new JButton("Add Item");
        JButton editBtn   = new JButton("Edit Item");
        JButton removeBtn = new JButton("Remove Item");
        JButton propBtn   = new JButton("Generate Sprint Proposal…");

        addBtn.addActionListener(e -> {
            BacklogItem item = itemDialog(panel, null);
            if (item != null) { project.getProductBacklog().addItem(item); project.save(); refresh.run(); }
        });

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { msg(panel, "Select an item first."); return; }
            BacklogItem existing = project.getProductBacklog().getAllItems().get(row);
            BacklogItem updated = itemDialog(panel, existing);
            if (updated != null) {
                project.getProductBacklog().editItem(existing,
                    updated.getPriority(), updated.getTimeEstimate(),
                    updated.getEffortEstimate(), updated.getRiskLevel());
                project.save();
                refresh.run();
            }
        });

        removeBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { msg(panel, "Select an item first."); return; }
            BacklogItem item = project.getProductBacklog().getAllItems().get(row);
            if (confirm(panel, "Remove \"" + item.getTitle() + "\"?")) {
                project.getProductBacklog().removeItem(item);
                project.save();
                refresh.run();
            }
        });

        propBtn.addActionListener(e -> {
            String in = JOptionPane.showInputDialog(panel, "Sprint capacity (hours):", "40");
            if (in == null) return;
            try {
                double cap = Double.parseDouble(in.trim());
                List<BacklogItem> proposal = project.getProductBacklog().generateSprintProposal(cap);
                if (proposal.isEmpty()) { msg(panel, "No eligible items fit within " + cap + " hours."); return; }
                StringBuilder sb = new StringBuilder("<html><b>Proposed items:</b><br><br>");
                double total = 0;
                for (BacklogItem i : proposal) {
                    sb.append("&nbsp;&nbsp;• ").append(i.getTitle())
                      .append("&nbsp;&nbsp;(effort: ").append(i.getEffortEstimate()).append(")<br>");
                    total += i.getEffortEstimate();
                }
                sb.append("<br><b>Total effort: ").append(total).append("</b></html>");
                JOptionPane.showMessageDialog(panel, sb.toString(), "Sprint Proposal", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) { msg(panel, "Enter a valid number."); }
        });

        // Only Scrum Master can modify the backlog
        boolean isSM = role == Role.SCRUM_MASTER;
        addBtn.setVisible(isSM);
        editBtn.setVisible(isSM);
        removeBtn.setVisible(isSM);
        propBtn.setVisible(isSM);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JTextField searchField = new JTextField(15);
            btns.add(new JLabel("Search:"));
            btns.add(searchField);

            searchField.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                String text = searchField.getText().toLowerCase();
                model.setRowCount(0);

            for (BacklogItem item : project.getProductBacklog().getAllItems()) {
                if (text.isEmpty() || item.getTitle().toLowerCase().contains(text)) {
                    model.addRow(new Object[]{
                        item.getTitle(),
                        item.getDescription(),
                        item.getPriority(),
                        item.getTimeEstimate(),
                        item.getEffortEstimate(),
                        item.getRiskLevel(),
                        item.getStatus()
                            });
                        }
                    }
                }
            });

        btns.add(addBtn); btns.add(editBtn); btns.add(removeBtn);
        btns.add(new JSeparator(SwingConstants.VERTICAL));
        btns.add(propBtn);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        return panel;
        }
    

    // -------------------------------------------------------------------------
    // Sprint tab
    // -------------------------------------------------------------------------
    private static JPanel buildSprintTab(ScrumProject project) {
        Role role = project.getCurrentUser().getRole();
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        SprintBacklog sprint = project.getCurrentSprint();
        JLabel info = new JLabel(sprint == null
            ? "No sprint yet — create one below."
            : sprintInfo(sprint));
        info.setFont(info.getFont().deriveFont(Font.BOLD, 13f));
        info.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        panel.add(info, BorderLayout.NORTH);

        String[] cols = { "Title", "Priority", "Effort Est.", "Status" };
        DefaultTableModel proposedModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        DefaultTableModel committedModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable proposedTable  = new JTable(proposedModel);
        JTable committedTable = new JTable(committedModel);
        proposedTable.setRowHeight(22);
        committedTable.setRowHeight(22);
        proposedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        committedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        Runnable refresh = () -> {
            proposedModel.setRowCount(0);
            committedModel.setRowCount(0);
            SprintBacklog s = project.getCurrentSprint();
            if (s == null) return;
            info.setText(sprintInfo(s));
            for (BacklogItem i : s.getProposedItems())
                proposedModel.addRow(new Object[]{ i.getTitle(), i.getPriority(), i.getEffortEstimate(), i.getStatus() });
            for (BacklogItem i : s.getCommittedItems())
                committedModel.addRow(new Object[]{ i.getTitle(), i.getPriority(), i.getEffortEstimate(), i.getStatus() });
        };
        refresh.run();

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Proposed Items"));
        leftPanel.add(new JScrollPane(proposedTable));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Committed Items"));
        rightPanel.add(new JScrollPane(committedTable));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setResizeWeight(0.5);
        panel.add(split, BorderLayout.CENTER);

        // Scrum Master buttons
        JButton genBtn    = new JButton("Auto-Generate Proposal…");
        JButton submitBtn = new JButton("Submit for Approval");
        JButton endBtn    = new JButton("End Sprint");

        // Scrum Team buttons
        JButton doneBtn    = new JButton("Mark Complete");
        JButton logBtn     = new JButton("Log Effort…");
        JButton takeOnBtn  = new JButton("Take On Item");
        JButton manageBtn  = new JButton("My Tasks for Item…");

        genBtn.addActionListener(e -> {
            SprintBacklog s = project.getCurrentSprint();
            double defaultCap = s != null ? s.getCapacityHours() : 40.0;
            String cap = JOptionPane.showInputDialog(panel, "Capacity (hours):", String.valueOf(defaultCap));
            if (cap == null) return;
            try {
                double hours = Double.parseDouble(cap.trim());
                if (s == null) s = project.createSprint(hours);
                List<BacklogItem> proposal = project.getProductBacklog().generateSprintProposal(hours);
                if (proposal.isEmpty()) { msg(panel, "No eligible items fit in that capacity."); return; }
                s.setProposedItems(proposal);
                project.save();
                refresh.run();
            } catch (NumberFormatException ex) { msg(panel, "Enter a valid number."); }
        });

        submitBtn.addActionListener(e -> {
            SprintBacklog s = project.getCurrentSprint();
            if (s == null) { msg(panel, "No sprint."); return; }
            if (s.getProposedItems().isEmpty()) { msg(panel, "Generate a proposal first."); return; }
            saveProposalFile(s);
            msg(panel, "Proposal for Sprint #" + s.getSprintNumber()
                + " submitted. The Product Owner will be notified on next login.");
        });

        endBtn.addActionListener(e -> {
            SprintBacklog s = project.getCurrentSprint();
            if (s == null) { msg(panel, "No sprint."); return; }
            if (!confirm(panel, "End sprint? Unfinished items will be returned to the product backlog.")) return;
            s.endSprint(project.getProductBacklog());
            project.save();
            refresh.run();
        });

        doneBtn.addActionListener(e -> {
            SprintBacklog s = project.getCurrentSprint();
            if (s == null) { msg(panel, "No sprint."); return; }
            int row = committedTable.getSelectedRow();
            if (row < 0) { msg(panel, "Select a committed item."); return; }
            s.getCommittedItems().get(row).setStatus(Status.COMPLETE);
            project.save();
            refresh.run();
        });

        logBtn.addActionListener(e -> {
            SprintBacklog s = project.getCurrentSprint();
            if (s == null) { msg(panel, "No sprint."); return; }
            int row = committedTable.getSelectedRow();
            if (row < 0) { msg(panel, "Select a committed item."); return; }
            String val = JOptionPane.showInputDialog(panel, "Actual effort to log:", "1.0");
            if (val == null) return;
            try { s.getCommittedItems().get(row).logActualEffort(Double.parseDouble(val.trim())); project.save(); }
            catch (NumberFormatException ex) { msg(panel, "Enter a valid number."); }
        });

        takeOnBtn.addActionListener(e -> {
            SprintBacklog s = project.getCurrentSprint();
            if (s == null) { msg(panel, "No sprint."); return; }
            int row = committedTable.getSelectedRow();
            if (row < 0) { msg(panel, "Select a committed item."); return; }
            BacklogItem item = s.getCommittedItems().get(row);
            item.setAssignee(project.getCurrentUser().getUsername());
            project.save();
            msg(panel, "You have taken on \"" + item.getTitle() + "\".");
        });

        manageBtn.addActionListener(e -> {
            SprintBacklog s = project.getCurrentSprint();
            if (s == null) { msg(panel, "No sprint."); return; }
            int row = committedTable.getSelectedRow();
            if (row < 0) { msg(panel, "Select a committed item."); return; }
            BacklogItem item = s.getCommittedItems().get(row);
            showTaskDialog(panel, item, project.getCurrentUser().getUsername());
        });

        // Role-based visibility
        boolean isSM   = role == Role.SCRUM_MASTER;
        boolean isTeam = role == Role.SCRUM_TEAM;
        genBtn.setVisible(isSM);
        submitBtn.setVisible(isSM);
        endBtn.setVisible(isSM);
        doneBtn.setVisible(isTeam);
        logBtn.setVisible(isTeam);
        takeOnBtn.setVisible(isTeam);
        manageBtn.setVisible(isTeam);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        for (JButton b : new JButton[]{ genBtn, submitBtn, endBtn, doneBtn, logBtn, takeOnBtn, manageBtn })
            btns.add(b);

        panel.add(btns, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Velocity & Burndown tab
    // -------------------------------------------------------------------------
    private static JPanel buildVelocityTab(ScrumProject project) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel sprintModel = new DefaultTableModel(
            new String[]{ "Sprint #", "State", "Planned Effort", "Velocity (Completed)" }, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (SprintBacklog s : project.getAllSprints())
            sprintModel.addRow(new Object[]{ s.getSprintNumber(), s.getState(), s.getTotalPlannedEffort(), s.calculateVelocity() });

        JTable sprintTable = new JTable(sprintModel);
        sprintTable.setRowHeight(22);
        JPanel sprintPanel = new JPanel(new BorderLayout());
        sprintPanel.setBorder(BorderFactory.createTitledBorder("Sprint Summary"));
        sprintPanel.add(new JScrollPane(sprintTable));

        SprintBacklog cur = project.getCurrentSprint();

        // Auto-record today's burndown snapshot when the tab is loaded
        if (cur != null && cur.getState() == SprintBacklog.SprintState.ACTIVE) {
            cur.recordBurndown(cur.getCurrentDay(), cur.calculateRemainingEffort());
            project.save();
        }

        BurndownChart chart = new BurndownChart();
        chart.setPreferredSize(new Dimension(0, 200));
        if (cur != null) chart.setData(cur.getBurndownData());

        JPanel bdPanel = new JPanel(new BorderLayout(4, 4));
        bdPanel.setBorder(BorderFactory.createTitledBorder("Current Sprint Burndown"));
        bdPanel.add(chart, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sprintPanel, bdPanel);
        split.setResizeWeight(0.55);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Proposals dialog (Product Owner)
    // -------------------------------------------------------------------------
    private static void showProposalsDialog(JFrame frame, ScrumProject project, JTabbedPane tabs) {
        File[] files = pendingProposalFiles();
        if (files.length == 0) { msg(frame, "No pending proposals."); return; }

        for (File f : files) {
            List<String> lines = readProposalFile(f);
            if (lines == null) continue;

            int sprintNum    = -1;
            double capacity  = 0;
            List<String> items = new ArrayList<>();
            for (String line : lines) {
                if      (line.startsWith("sprint:"))   sprintNum = Integer.parseInt(line.substring(7).trim());
                else if (line.startsWith("capacity:")) capacity  = Double.parseDouble(line.substring(9).trim());
                else if (!line.isEmpty())              items.add(line);
            }
            if (sprintNum < 0) continue;

            StringBuilder sb = new StringBuilder(
                "<html><b>Sprint #" + sprintNum + "</b>  (Capacity: " + capacity + " h)<br><br>"
                + "<b>Proposed items:</b><br>");
            for (String item : items) sb.append("&nbsp;&nbsp;• ").append(item).append("<br>");
            sb.append("</html>");

            Object[] options = { "Approve", "Reject", "Skip" };
            int choice = JOptionPane.showOptionDialog(frame, sb.toString(),
                "Sprint #" + sprintNum + " Proposal",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

            if (choice == 0) {       // Approve
                SprintBacklog s = findSprint(project, sprintNum);
                if (s != null) { s.approve(); s.startSprint(); }
                f.delete();
                project.save();
                msg(frame, "Sprint #" + sprintNum + " approved and started.");
                tabs.setComponentAt(1, buildSprintTab(project));
                tabs.revalidate();
            } else if (choice == 1) { // Reject
                SprintBacklog s = findSprint(project, sprintNum);
                if (s != null) s.reject();
                f.delete();
                project.save();
                msg(frame, "Sprint #" + sprintNum + " rejected. The Scrum Master can revise the proposal.");
                tabs.setComponentAt(1, buildSprintTab(project));
                tabs.revalidate();
            }
            // Skip — leave file, do nothing
        }
    }

    // -------------------------------------------------------------------------
    // Proposal file I/O
    // -------------------------------------------------------------------------
    private static void saveProposalFile(SprintBacklog sprint) {
        File f = new File(PROPOSALS_DIR, "sprint_" + sprint.getSprintNumber() + ".txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("sprint:"   + sprint.getSprintNumber());
            pw.println("capacity:" + sprint.getCapacityHours());
            for (BacklogItem item : sprint.getProposedItems())
                pw.println(item.getTitle());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save proposal file", ex);
        }
    }

    private static List<String> readProposalFile(File f) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        } catch (IOException ex) { return null; }
        return lines;
    }

    private static File[] pendingProposalFiles() {
        File[] files = PROPOSALS_DIR.listFiles((d, name) -> name.endsWith(".txt"));
        return files != null ? files : new File[0];
    }

    private static SprintBacklog findSprint(ScrumProject project, int sprintNumber) {
        for (SprintBacklog s : project.getAllSprints())
            if (s.getSprintNumber() == sprintNumber) return s;
        return null;
    }

    // -------------------------------------------------------------------------
    // My Tasks tab (Scrum Team only)
    // -------------------------------------------------------------------------
    private static JPanel buildMyTasksTab(ScrumProject project) {
        String username = project.getCurrentUser().getUsername();
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = { "Parent Item", "Task", "Effort Est.", "Status" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);

        // Collect all EngineeringTasks assigned to this user across all sprint items
        List<EngineeringTask> myTasks = new ArrayList<>();
        Runnable refresh = () -> {
            myTasks.clear();
            model.setRowCount(0);
            SprintBacklog s = project.getCurrentSprint();
            if (s == null) return;
            for (BacklogItem item : s.getCommittedItems()) {
                for (EngineeringTask t : item.getTasks()) {
                    if (username.equals(t.getAssignee())) {
                        myTasks.add(t);
                        model.addRow(new Object[]{
                            item.getTitle(), t.getTitle(), t.getEffortEstimate(), t.getStatus()
                        });
                    }
                }
            }
        };
        refresh.run();

        JButton markBtn = new JButton("Update Status…");
        markBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { msg(panel, "Select a task first."); return; }
            EngineeringTask task = myTasks.get(row);
            EngineeringTask.TaskStatus[] statuses = EngineeringTask.TaskStatus.values();
            EngineeringTask.TaskStatus chosen = (EngineeringTask.TaskStatus) JOptionPane.showInputDialog(
                panel, "Set status for \"" + task.getTitle() + "\":",
                "Update Status", JOptionPane.PLAIN_MESSAGE,
                null, statuses, task.getStatus());
            if (chosen != null) { task.setStatus(chosen); refresh.run(); }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.add(markBtn);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Task management dialog — break a sprint item into sub-tasks
    // -------------------------------------------------------------------------
    private static void showTaskDialog(Component parent, BacklogItem item, String username) {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(parent),
            "My Tasks — " + item.getTitle(), true);
        dialog.setSize(600, 380);
        dialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = { "Task", "Effort Est.", "Status" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        List<EngineeringTask> myTasks = new ArrayList<>();
        Runnable refresh = () -> {
            myTasks.clear();
            model.setRowCount(0);
            for (EngineeringTask t : item.getTasks()) {
                if (username.equals(t.getAssignee())) {
                    myTasks.add(t);
                    model.addRow(new Object[]{ t.getTitle(), t.getEffortEstimate(), t.getStatus() });
                }
            }
        };
        refresh.run();

        JButton addBtn    = new JButton("Add Task");
        JButton statusBtn = new JButton("Update Status…");
        JButton removeBtn = new JButton("Remove Task");

        addBtn.addActionListener(e -> {
            JTextField titleField  = new JTextField(20);
            JSpinner effortSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 999.0, 0.5));
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.add(new JLabel("Task title:"));      form.add(titleField);
            form.add(new JLabel("Effort estimate:")); form.add(effortSpinner);
            int result = JOptionPane.showConfirmDialog(dialog, form, "Add Task",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            String title = titleField.getText().trim();
            if (title.isEmpty()) { msg(dialog, "Title is required."); return; }
            EngineeringTask task = new EngineeringTask(title, "",
                ((Number) effortSpinner.getValue()).doubleValue(), item);
            task.setAssignee(username);
            item.addTask(task);
            refresh.run();
        });

        statusBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { msg(dialog, "Select a task first."); return; }
            EngineeringTask task = myTasks.get(row);
            EngineeringTask.TaskStatus[] statuses = EngineeringTask.TaskStatus.values();
            EngineeringTask.TaskStatus chosen = (EngineeringTask.TaskStatus) JOptionPane.showInputDialog(
                dialog, "Set status:", "Update Status", JOptionPane.PLAIN_MESSAGE,
                null, statuses, task.getStatus());
            if (chosen != null) { task.setStatus(chosen); refresh.run(); }
        });

        removeBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { msg(dialog, "Select a task first."); return; }
            if (confirm(dialog, "Remove this task?")) {
                item.removeTask(myTasks.get(row));
                refresh.run();
            }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.add(addBtn); btns.add(statusBtn); btns.add(removeBtn);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Add / Edit item dialog
    // -------------------------------------------------------------------------
    private static BacklogItem itemDialog(Component parent, BacklogItem ex) {
        JTextField titleField = new JTextField(ex != null ? ex.getTitle() : "", 20);
        JTextField descField  = new JTextField(ex != null ? ex.getDescription() : "", 20);
        JComboBox<Priority> priBox = new JComboBox<>(Priority.values());
        if (ex != null) priBox.setSelectedItem(ex.getPriority());
        JSpinner timeSpin   = new JSpinner(new SpinnerNumberModel(ex != null ? ex.getTimeEstimate()   : 1.0, 0.0, 999.0, 0.5));
        JSpinner effortSpin = new JSpinner(new SpinnerNumberModel(ex != null ? ex.getEffortEstimate() : 1.0, 0.0, 999.0, 0.5));
        JSpinner riskSpin   = new JSpinner(new SpinnerNumberModel(ex != null ? ex.getRiskLevel()      : 0.0, 0.0,  10.0, 0.5));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        form.add(new JLabel("Title:"));             form.add(titleField);
        form.add(new JLabel("Description:"));       form.add(descField);
        form.add(new JLabel("Priority:"));          form.add(priBox);
        form.add(new JLabel("Time estimate (h):")); form.add(timeSpin);
        form.add(new JLabel("Effort estimate:"));   form.add(effortSpin);
        form.add(new JLabel("Risk (0–10):"));       form.add(riskSpin);

        int result = JOptionPane.showConfirmDialog(parent, form,
            ex == null ? "Add Backlog Item" : "Edit Backlog Item",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;

        String title = titleField.getText().trim();
        if (title.isEmpty()) { msg(parent, "Title is required."); return null; }
        return new BacklogItem(title, descField.getText().trim(),
            (Priority) priBox.getSelectedItem(),
            ((Number) timeSpin.getValue()).doubleValue(),
            ((Number) effortSpin.getValue()).doubleValue(),
            ((Number) riskSpin.getValue()).doubleValue());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private static String sprintInfo(SprintBacklog s) {
        return "Sprint #" + s.getSprintNumber()
            + "   |   State: " + s.getState()
            + "   |   Capacity: " + s.getCapacityHours() + " h"
            + "   |   Planned effort: " + s.getTotalPlannedEffort();
    }

    // -------------------------------------------------------------------------
    // Burndown chart
    // -------------------------------------------------------------------------
    private static class BurndownChart extends JPanel {
        private TreeMap<Integer, Double> data = new TreeMap<>();

        void setData(Map<Integer, Double> d) {
            data = new TreeMap<>(d);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 55, padR = 20, padT = 20, padB = 35;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            // Background
            g2.setColor(Color.WHITE);
            g2.fillRect(padL, padT, chartW, chartH);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawRect(padL, padT, chartW, chartH);

            if (data.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("No data yet", padL + chartW / 2 - 30, padT + chartH / 2);
                return;
            }

            int maxDay = data.lastKey();
            double maxEffort = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            if (maxEffort == 0) maxEffort = 1;

            // Grid lines
            g2.setColor(new Color(220, 220, 220));
            for (int i = 1; i <= 4; i++) {
                int y = padT + (int)(chartH * i / 4.0);
                g2.drawLine(padL, y, padL + chartW, y);
            }

            // Ideal burndown line (dashed)
            g2.setColor(new Color(180, 180, 255));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{6f}, 0f));
            int x0 = padL, y0 = padT;
            int x1 = padL + chartW, y1 = padT + chartH;
            g2.drawLine(x0, y0, x1, y1);
            g2.setStroke(new BasicStroke(1.5f));

            // Actual burndown line
            g2.setColor(new Color(220, 60, 60));
            g2.setStroke(new BasicStroke(2f));
            int[] days = data.keySet().stream().mapToInt(Integer::intValue).toArray();
            int prevX = -1, prevY = -1;
            for (int day : days) {
                double effort = data.get(day);
                int px = padL + (maxDay == 0 ? 0 : (int)(day * chartW / (double) maxDay));
                int py = padT + chartH - (int)(effort * chartH / maxEffort);
                if (prevX >= 0) g2.drawLine(prevX, prevY, px, py);
                g2.fillOval(px - 3, py - 3, 7, 7);
                prevX = px; prevY = py;
            }

            // Y-axis labels
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(g2.getFont().deriveFont(10f));
            for (int i = 0; i <= 4; i++) {
                double val = maxEffort * (4 - i) / 4.0;
                int y = padT + (int)(chartH * i / 4.0);
                g2.drawString(String.format("%.0f", val), 2, y + 4);
            }

            // X-axis labels
            for (int day : days) {
                int px = padL + (maxDay == 0 ? 0 : (int)(day * chartW / (double) maxDay));
                g2.drawString(String.valueOf(day), px - 3, padT + chartH + 14);
            }

            // Axis titles
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
            g2.drawString("Day", padL + chartW / 2 - 10, h - 2);
            // Rotated Y label
            Graphics2D g2r = (Graphics2D) g2.create();
            g2r.rotate(-Math.PI / 2, 10, padT + chartH / 2);
            g2r.drawString("Effort (h)", 10 - 20, padT + chartH / 2);
            g2r.dispose();
        }
    }

    private static void msg(Component parent, String text) {
        JOptionPane.showMessageDialog(parent, text);
    }

    private static boolean confirm(Component parent, String text) {
        return JOptionPane.showConfirmDialog(parent, text, "Confirm",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
