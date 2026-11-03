import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class AmbulanceDispatchApp extends JFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/AmbulanceDispatchDB";
    private static final String USER = "root";
    private static final String PASSWORD = "changeme"; // Change to your MySQL password if needed

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private HomeDashboardPanel homePanel;
    private IncidentWizardPanel wizardPanel;
    private TableManagementPanel tableManagementPanel;

    // Theme Palette
    private static final Color ADMINLTE_BG = new Color(244, 246, 249);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color HEADER_DARK = new Color(52, 58, 64);
    private static final Color BORDER_SUBTLE = new Color(218, 225, 233);
    private static final Color ACCENT_RED = new Color(220, 53, 69);
    private static final Color ADMINLTE_BLUE = new Color(13, 110, 253);
    private static final Color LIGHT_PINK_HOVER = new Color(255, 228, 232);

    public AmbulanceDispatchApp() {
        setTitle("Ambulance Dispatch Management System");
        setSize(1350, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top Navbar
        JPanel topNavbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 15));
        topNavbar.setBackground(HEADER_DARK);
        topNavbar.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, ACCENT_RED));

        JLabel logoLabel = new JLabel("🚑 AMBULANCE DISPATCH MANAGEMENT SYSTEM");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        topNavbar.add(logoLabel);

        topNavbar.add(Box.createHorizontalStrut(20));

        JButton homeBtn = createNavButton("🏠 HOME / OVERVIEW");
        JButton wizardBtn = createNavButton("🚨 REPORT INCIDENT WIZARD");
        JButton tablesBtn = createNavButton("🗂️ DATABASE EXPLORER");

        topNavbar.add(homeBtn);
        topNavbar.add(wizardBtn);
        topNavbar.add(tablesBtn);

        add(topNavbar, BorderLayout.NORTH);

        // Main Content Area
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(ADMINLTE_BG);

        homePanel = new HomeDashboardPanel(this);
        wizardPanel = new IncidentWizardPanel(this);
        tableManagementPanel = new TableManagementPanel();

        mainContentPanel.add(homePanel, "HOME");
        mainContentPanel.add(wizardPanel, "WIZARD");
        mainContentPanel.add(tableManagementPanel, "TABLES");

        add(mainContentPanel, BorderLayout.CENTER);

        // Bottom Status Bar with Live Clock & Background Auto-Reset Worker
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(HEADER_DARK);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(70, 75, 80)));
        statusBar.setBorder(new EmptyBorder(6, 15, 6, 15));

        JLabel statusTicker = new JLabel("  🔴 SYSTEM STATUS: SECURE & CONNECTED TO MySQL  |  AUTOMATED FLEET RESET ACTIVE");
        statusTicker.setForeground(new Color(255, 193, 7));
        statusTicker.setFont(new Font("SansSerif", Font.BOLD, 11));

        JLabel liveClock = new JLabel();
        liveClock.setForeground(Color.WHITE);
        liveClock.setFont(new Font("SansSerif", Font.BOLD, 11));

        javax.swing.Timer clockTimer = new javax.swing.Timer(1000, e -> {
            liveClock.setText(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new java.util.Date()) + "  ");
        });
        clockTimer.start();

        // Background Worker Timer: Checks every 15 seconds to free up arrived ambulances automatically
        javax.swing.Timer autoResetTimer = new javax.swing.Timer(15000, e -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE Dispatch d JOIN Ambulance a ON d.Ambulance_ID = a.Ambulance_ID SET d.Status = 'Completed', a.Status = 'Available' WHERE d.Status = 'In Progress' AND d.Arrival_Time <= NOW()");
            } catch (Exception ignored) {}
        });
        autoResetTimer.start();

        statusBar.add(statusTicker, BorderLayout.WEST);
        statusBar.add(liveClock, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        homeBtn.addActionListener(e -> cardLayout.show(mainContentPanel, "HOME"));
        wizardBtn.addActionListener(e -> {
            cardLayout.show(mainContentPanel, "WIZARD");
            wizardPanel.initWizardData();
        });
        tablesBtn.addActionListener(e -> {
            cardLayout.show(mainContentPanel, "TABLES");
            tableManagementPanel.refreshTableData();
        });
    }

    public void showPanel(String name) {
        cardLayout.show(mainContentPanel, name);
        if (name.equals("WIZARD")) {
            wizardPanel.initWizardData();
        } else if (name.equals("TABLES")) {
            tableManagementPanel.refreshTableData();
        }
    }

    public static void showToast(JFrame parent, String message) {
        JWindow toast = new JWindow(parent);
        toast.setLayout(new BorderLayout());
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(33, 37, 41, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 22, 12, 22));
        JLabel l = new JLabel("✨ " + message);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        p.add(l);
        toast.add(p);
        toast.pack();

        try {
            Point loc = parent.getLocationOnScreen();
            toast.setLocation(loc.x + parent.getWidth() - toast.getWidth() - 35, loc.y + parent.getHeight() - toast.getHeight() - 70);
        } catch (Exception e) {
            toast.setLocation(500, 500);
        }
        toast.setVisible(true);

        javax.swing.Timer timer = new javax.swing.Timer(2500, e -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_RED);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setForeground(HEADER_DARK);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(LIGHT_PINK_HOVER);
                btn.setForeground(ACCENT_RED);
            }
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(Color.WHITE);
                btn.setForeground(HEADER_DARK);
            }
        });
        return btn;
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found: " + e.getMessage());
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static class ComboItem {
        private int id;
        private String label;
        public ComboItem(int id, String label) {
            this.id = id;
            this.label = label;
        }
        public int getId() { return id; }
        @Override
        public String toString() { return label; }
    }

    // --- 1. HOME DASHBOARD PANEL ---
    static class HomeDashboardPanel extends JPanel {
        public HomeDashboardPanel(AmbulanceDispatchApp parentApp) {
            setLayout(new BorderLayout(20, 20));
            setBackground(ADMINLTE_BG);
            setBorder(new EmptyBorder(40, 40, 40, 40));

            JPanel hero = new JPanel();
            hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
            hero.setBackground(CARD_BG);
            hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
            ));

            JLabel title = new JLabel("Welcome to Ambulance Dispatch Command Center");
            title.setFont(new Font("SansSerif", Font.BOLD, 26));
            title.setForeground(ACCENT_RED);

            JLabel subtitle = new JLabel("<html><br>This desktop database management system handles emergency calls, live incident reporting, ambulance fleet tracking, partner hospital coordination, and real-time dispatches using your relational MySQL database (<b>AmbulanceDispatchDB</b>).<br><br>Choose an action below to get started:</html>");
            subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
            subtitle.setForeground(HEADER_DARK);

            hero.add(title);
            hero.add(subtitle);
            add(hero, BorderLayout.NORTH);

            JPanel actionsPanel = new JPanel(new GridLayout(1, 2, 25, 0));
            actionsPanel.setOpaque(false);

            JPanel reportCard = createActionCard("🚨 REPORT AN INCIDENT", "Launch the interactive wizard to report a new emergency, choose city, filter hospitals, and dispatch available ambulances.", e -> parentApp.showPanel("WIZARD"));
            JPanel dbCard = createActionCard("🗂️ EXPLORE DATABASE", "Inspect underlying relational tables (Caller, Incident, Ambulance, Hospital, Dispatch), run keyword searches, add, edit, or delete records.", e -> parentApp.showPanel("TABLES"));

            actionsPanel.add(reportCard);
            actionsPanel.add(dbCard);
            add(actionsPanel, BorderLayout.CENTER);
        }

        private JPanel createActionCard(String title, String desc, java.awt.event.ActionListener action) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(CARD_BG);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
            ));

            JLabel tLbl = new JLabel(title);
            tLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
            tLbl.setForeground(HEADER_DARK);

            JLabel dLbl = new JLabel("<html><br>" + desc + "</html>");
            dLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            dLbl.setForeground(new Color(108, 117, 125));

            JButton btn = createStyledRoundedButton("🚀 GET STARTED");
            btn.addActionListener(action);

            card.add(tLbl);
            card.add(Box.createRigidArea(new Dimension(0, 10)));
            card.add(dLbl);
            card.add(Box.createRigidArea(new Dimension(0, 25)));
            card.add(btn);

            card.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ADMINLTE_BLUE, 2),
                        BorderFactory.createEmptyBorder(29, 29, 29, 29)
                    ));
                }
                public void mouseExited(MouseEvent evt) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                        BorderFactory.createEmptyBorder(30, 30, 30, 30)
                    ));
                }
            });

            return card;
        }
    }

    // --- 2. INTERACTIVE INCIDENT REPORTING WIZARD ---
    static class IncidentWizardPanel extends JPanel {
        private JTextField callerNameField, phoneField, locationField;
        private JComboBox<String> cityBox, incidentTypeBox, severityBox;
        private JComboBox<ComboItem> hospitalBox, ambulanceBox;
        private boolean isUpdating = false;

        public IncidentWizardPanel(AmbulanceDispatchApp parentApp) {
            setLayout(new BorderLayout(15, 15));
            setBackground(ADMINLTE_BG);
            setBorder(new EmptyBorder(25, 25, 25, 25));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            JLabel title = new JLabel("🚨 Emergency Incident Reporting & Dispatch Wizard");
            title.setFont(new Font("SansSerif", Font.BOLD, 22));
            title.setForeground(HEADER_DARK);
            header.add(title, BorderLayout.WEST);
            add(header, BorderLayout.NORTH);

            JPanel formCard = new JPanel();
            formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
            formCard.setBackground(CARD_BG);
            formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)
            ));

            callerNameField = addFormRow(formCard, "Caller Full Name *");
            phoneField = addFormRow(formCard, "Phone Number *");
            locationField = addFormRow(formCard, "Incident Location / Street *");

            // City Dropdown (Chennai, Bangalore, Hyderabad)
            JPanel cityRow = new JPanel(new BorderLayout(5, 5));
            cityRow.setOpaque(false);
            cityRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
            JLabel cityLbl = new JLabel("Select City *");
            cityLbl.setForeground(HEADER_DARK);
            cityLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            cityBox = new JComboBox<>(new String[]{"Chennai", "Bangalore", "Hyderabad"});
            cityBox.setBackground(Color.WHITE);
            cityRow.add(cityLbl, BorderLayout.NORTH);
            cityRow.add(cityBox, BorderLayout.CENTER);
            formCard.add(cityRow);
            formCard.add(Box.createRigidArea(new Dimension(0, 10)));

            // Incident Type Dropdown
            JPanel typeRow = new JPanel(new BorderLayout(5, 5));
            typeRow.setOpaque(false);
            typeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
            JLabel typeLbl = new JLabel("What kind of incident happened? *");
            typeLbl.setForeground(HEADER_DARK);
            typeLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            incidentTypeBox = new JComboBox<>(new String[]{"Accident / Collision", "Medical / Cardiac Emergency", "Trauma / Severe Injury", "Fire / Rescue", "Other Critical Emergency"});
            incidentTypeBox.setBackground(Color.WHITE);
            typeRow.add(typeLbl, BorderLayout.NORTH);
            typeRow.add(incidentTypeBox, BorderLayout.CENTER);
            formCard.add(typeRow);
            formCard.add(Box.createRigidArea(new Dimension(0, 10)));

            // Severity Dropdown
            JPanel sevRow = new JPanel(new BorderLayout(5, 5));
            sevRow.setOpaque(false);
            sevRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
            JLabel sevLbl = new JLabel("What kind of emergency level / response do you need? *");
            sevLbl.setForeground(HEADER_DARK);
            sevLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            severityBox = new JComboBox<>(new String[]{"Critical", "High", "Medium", "Low"});
            severityBox.setBackground(Color.WHITE);
            sevRow.add(sevLbl, BorderLayout.NORTH);
            sevRow.add(severityBox, BorderLayout.CENTER);
            formCard.add(sevRow);
            formCard.add(Box.createRigidArea(new Dimension(0, 10)));

            // Cascading Hospital & Ambulance Dropdowns
            JPanel resRow = new JPanel(new GridLayout(1, 2, 15, 0));
            resRow.setOpaque(false);
            resRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

            hospitalBox = new JComboBox<>();
            ambulanceBox = new JComboBox<>();
            hospitalBox.setBackground(Color.WHITE);
            ambulanceBox.setBackground(Color.WHITE);

            // City selection cascading listener
            cityBox.addActionListener(e -> {
                if (!isUpdating) {
                    String selectedCity = (String) cityBox.getSelectedItem();
                    if (selectedCity != null) {
                        loadHospitalsForCity(selectedCity);
                    }
                }
            });

            // Hospital selection cascading listener
            hospitalBox.addActionListener(e -> {
                if (!isUpdating) {
                    ComboItem selectedHosp = (ComboItem) hospitalBox.getSelectedItem();
                    if (selectedHosp != null) {
                        loadAmbulancesForHospital(selectedHosp.getId());
                    } else {
                        ambulanceBox.removeAllItems();
                    }
                }
            });

            JPanel hospPanel = new JPanel(new BorderLayout(3, 3));
            hospPanel.setOpaque(false);
            JLabel hospLbl = new JLabel("Shortlisted Partner Hospital Name *");
            hospLbl.setForeground(HEADER_DARK);
            hospLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            hospPanel.add(hospLbl, BorderLayout.NORTH);
            hospPanel.add(hospitalBox, BorderLayout.CENTER);

            JPanel ambPanel = new JPanel(new BorderLayout(3, 3));
            ambPanel.setOpaque(false);
            JLabel ambLbl = new JLabel("Shortlisted Available Ambulance *");
            ambLbl.setForeground(HEADER_DARK);
            ambLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            ambPanel.add(ambLbl, BorderLayout.NORTH);
            ambPanel.add(ambulanceBox, BorderLayout.CENTER);

            resRow.add(hospPanel);
            resRow.add(ambPanel);
            formCard.add(resRow);
            formCard.add(Box.createRigidArea(new Dimension(0, 15)));

            JScrollPane scroll = new JScrollPane(formCard);
            scroll.getViewport().setBackground(ADMINLTE_BG);
            add(scroll, BorderLayout.CENTER);

            JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
            btnBar.setOpaque(false);

            JButton refreshResBtn = createStyledRoundedButton("🔄 REFRESH LIST");
            refreshResBtn.addActionListener(e -> initWizardData());

            JButton submitBtn = createStyledRoundedButton("💾 SUBMIT & DISPATCH TO DATABASE");
            submitBtn.addActionListener(e -> processIncidentSubmission(parentApp));

            btnBar.add(refreshResBtn);
            btnBar.add(submitBtn);
            add(btnBar, BorderLayout.SOUTH);

            initWizardData();
        }

        private JTextField addFormRow(JPanel parent, String labelText) {
            JPanel row = new JPanel(new BorderLayout(5, 5));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

            JLabel lbl = new JLabel(labelText);
            lbl.setForeground(HEADER_DARK);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));

            JTextField txt = new JTextField();
            txt.setBackground(CARD_BG);
            txt.setForeground(HEADER_DARK);
            txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
            ));

            row.add(lbl, BorderLayout.NORTH);
            row.add(txt, BorderLayout.CENTER);
            parent.add(row);
            parent.add(Box.createRigidArea(new Dimension(0, 10)));
            return txt;
        }

        public void initWizardData() {
            isUpdating = true;
            if (cityBox.getItemCount() > 0) {
                cityBox.setSelectedIndex(0);
                loadHospitalsForCity((String) cityBox.getSelectedItem());
            }
            isUpdating = false;
        }

        public void loadHospitalsForCity(String city) {
            isUpdating = true;
            hospitalBox.removeAllItems();
            ambulanceBox.removeAllItems();
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT Hospital_ID, Hospital_Name, Address FROM Hospital WHERE City = ? ORDER BY Hospital_Name ASC")) {
                pstmt.setString(1, city);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        String name = rs.getString(2);
                        String address = rs.getString(3);
                        hospitalBox.addItem(new ComboItem(id, name + " (" + address + ")"));
                    }
                }
                if (hospitalBox.getItemCount() > 0) {
                    hospitalBox.setSelectedIndex(0);
                    ComboItem firstHosp = (ComboItem) hospitalBox.getSelectedItem();
                    if (firstHosp != null) {
                        loadAmbulancesForHospital(firstHosp.getId());
                    }
                }
            } catch (Exception e) {
                // Handled gracefully
            }
            isUpdating = false;
        }

        private void loadAmbulancesForHospital(int hospitalId) {
            ambulanceBox.removeAllItems();
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT Ambulance_ID, License_Plate, Status FROM Ambulance WHERE Hospital_ID = ? AND Status = 'Available'")) {
                pstmt.setInt(1, hospitalId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    boolean found = false;
                    while (rs.next()) {
                        found = true;
                        int ambId = rs.getInt(1);
                        String licensePlate = rs.getString(2);
                        String status = rs.getString(3);
                        ambulanceBox.addItem(new ComboItem(ambId, "Plate: " + licensePlate + " [" + status + "]"));
                    }
                    if (!found) {
                        ambulanceBox.addItem(new ComboItem(-1, "-- No Available Ambulances --"));
                    }
                }
            } catch (Exception e) {
                // Handled gracefully
            }
        }

        private void processIncidentSubmission(AmbulanceDispatchApp parentApp) {
            String name = callerNameField.getText().trim();
            String phone = phoneField.getText().trim();
            String location = locationField.getText().trim();
            String city = (String) cityBox.getSelectedItem();

            if (name.isEmpty() || phone.isEmpty() || location.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required caller and location fields.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ComboItem selectedHosp = (ComboItem) hospitalBox.getSelectedItem();
            ComboItem selectedAmb = (ComboItem) ambulanceBox.getSelectedItem();

            if (selectedHosp == null || selectedAmb == null || selectedAmb.getId() == -1) {
                JOptionPane.showMessageDialog(this, "Please select a valid Hospital with an available Ambulance.", "Resource Missing", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);

                // 1. Insert Caller
                String callerSql = "INSERT INTO Caller (Name, Phone_Number, Location, City, Time_of_Call) VALUES (?, ?, ?, ?, NOW())";
                int callerId = -1;
                try (PreparedStatement pstmt = conn.prepareStatement(callerSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, phone);
                    pstmt.setString(3, location);
                    pstmt.setString(4, city);
                    pstmt.executeUpdate();
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) callerId = rs.getInt(1);
                    }
                }

                // 2. Insert Incident
                String incidentType = (String) incidentTypeBox.getSelectedItem();
                String severity = (String) severityBox.getSelectedItem();
                String description = "Emergency Type: " + incidentType + " at " + location + ", " + city;
                
                String incidentSql = "INSERT INTO Incident (Caller_ID, Severity, Priority, Incident_Date, Description) VALUES (?, ?, ?, CURDATE(), ?)";
                int incidentId = -1;
                try (PreparedStatement pstmt = conn.prepareStatement(incidentSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, callerId);
                    pstmt.setString(2, severity);
                    pstmt.setString(3, severity.equals("Critical") ? "High" : "Normal");
                    pstmt.setString(4, description);
                    pstmt.executeUpdate();
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) incidentId = rs.getInt(1);
                    }
                }

                // 3. Insert Dispatch Record & update Ambulance Status to 'On Route'
                int ambId = selectedAmb.getId();
                int hospId = selectedHosp.getId();
                String dispatchSql = "INSERT INTO Dispatch (Incident_ID, Ambulance_ID, Hospital_ID, Dispatch_Time, Arrival_Time, Status) VALUES (?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 15 MINUTE), 'In Progress')";
                try (PreparedStatement pstmt = conn.prepareStatement(dispatchSql)) {
                    pstmt.setInt(1, incidentId);
                    pstmt.setInt(2, ambId);
                    pstmt.setInt(3, hospId);
                    pstmt.executeUpdate();
                }

                // Update ambulance status
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE Ambulance SET Status = 'On Route' WHERE Ambulance_ID = ?")) {
                    pstmt.setInt(1, ambId);
                    pstmt.executeUpdate();
                }

                conn.commit();
                showToast(parentApp, "Incident successfully reported & Ambulance dispatched!");
                parentApp.showPanel("TABLES");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database Error during Dispatch: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- 3. DATABASE EXPLORER & CRUD PANEL ---
    static class TableManagementPanel extends JPanel {
        private JComboBox<String> tableComboBox;
        private JTable dataTable;
        private DefaultTableModel tableModel;
        private JTextField searchField;
        private JComboBox<String> searchColumnBox;
        private String currentTable;

        public static class ColumnMeta {
            public String name;
            public String typeName;
            public boolean isNullable;
            public boolean isAutoIncrement;

            public ColumnMeta(String name, String typeName, boolean isNullable, boolean isAutoIncrement) {
                this.name = name;
                this.typeName = typeName;
                this.isNullable = isNullable;
                this.isAutoIncrement = isAutoIncrement;
            }
        }

        private List<ColumnMeta> currentColumns = new ArrayList<>();
        private List<String> primaryKeys = new ArrayList<>();

        public TableManagementPanel() {
            setLayout(new BorderLayout(10, 10));
            setBackground(ADMINLTE_BG);
            setBorder(new EmptyBorder(15, 15, 15, 15));

            JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            topBar.setBackground(CARD_BG);
            topBar.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE, 1));

            JLabel lbl1 = new JLabel("TABLE:");
            lbl1.setForeground(HEADER_DARK);
            lbl1.setFont(new Font("SansSerif", Font.BOLD, 12));
            topBar.add(lbl1);

            tableComboBox = createBorderlessComboBox();
            loadTableList();
            topBar.add(tableComboBox);

            JLabel lbl2 = new JLabel("SEARCH COL:");
            lbl2.setForeground(HEADER_DARK);
            lbl2.setFont(new Font("SansSerif", Font.BOLD, 12));
            topBar.add(lbl2);

            searchColumnBox = createBorderlessComboBox();
            topBar.add(searchColumnBox);

            JLabel lbl3 = new JLabel("KEYWORD:");
            lbl3.setForeground(HEADER_DARK);
            lbl3.setFont(new Font("SansSerif", Font.BOLD, 12));
            topBar.add(lbl3);

            searchField = new JTextField(12);
            searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
            searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
            ));
            topBar.add(searchField);

            JButton searchBtn = createStyledRoundedButton("🔍 SEARCH");
            searchBtn.addActionListener(e -> refreshTableData());
            topBar.add(searchBtn);

            add(topBar, BorderLayout.NORTH);

            tableModel = new DefaultTableModel() {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            dataTable = new JTable(tableModel);
            dataTable.setRowHeight(30);
            dataTable.setBackground(CARD_BG);
            dataTable.setForeground(HEADER_DARK);
            dataTable.getTableHeader().setBackground(HEADER_DARK);
            dataTable.getTableHeader().setForeground(Color.WHITE);
            dataTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

            dataTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (value != null && !isSelected) {
                        String valStr = value.toString().toLowerCase();
                        if (valStr.equals("completed") || valStr.equals("available") || valStr.equals("active")) {
                            setForeground(new Color(40, 167, 69));
                            setFont(new Font("SansSerif", Font.BOLD, 12));
                        } else if (valStr.equals("in progress") || valStr.equals("dispatched") || valStr.equals("busy") || valStr.equals("on route")) {
                            setForeground(new Color(255, 140, 0));
                            setFont(new Font("SansSerif", Font.BOLD, 12));
                        } else {
                            setForeground(HEADER_DARK);
                            setFont(new Font("SansSerif", Font.PLAIN, 12));
                        }
                        setBackground(CARD_BG);
                    } else if (isSelected) {
                        setBackground(ADMINLTE_BLUE);
                        setForeground(Color.WHITE);
                    }
                    return c;
                }
            });

            JScrollPane scrollPane = new JScrollPane(dataTable);
            scrollPane.getViewport().setBackground(ADMINLTE_BG);
            scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE, 1));
            add(scrollPane, BorderLayout.CENTER);

            JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
            bottomBar.setBackground(ADMINLTE_BG);

            JButton addBtn = createStyledRoundedButton("➕ ADD RECORD");
            JButton editBtn = createStyledRoundedButton("✏️ EDIT SELECTED");
            JButton deleteBtn = createStyledRoundedButton("🗑️ DELETE RECORD");
            JButton refreshBtn = createStyledRoundedButton("🔄 REFRESH");

            addBtn.addActionListener(e -> handleAdd());
            editBtn.addActionListener(e -> handleEdit());
            deleteBtn.addActionListener(e -> handleDelete());
            refreshBtn.addActionListener(e -> refreshTableData());

            bottomBar.add(addBtn);
            bottomBar.add(editBtn);
            bottomBar.add(deleteBtn);
            bottomBar.add(refreshBtn);
            add(bottomBar, BorderLayout.SOUTH);

            tableComboBox.addActionListener(e -> {
                String selected = (String) tableComboBox.getSelectedItem();
                if (selected != null && !selected.equals(currentTable)) {
                    currentTable = selected;
                    onTableChanged();
                }
            });

            if (tableComboBox.getItemCount() > 0) tableComboBox.setSelectedIndex(0);
        }

        private JComboBox<String> createBorderlessComboBox() {
            JComboBox<String> combo = new JComboBox<>();
            combo.setFont(new Font("SansSerif", Font.BOLD, 13));
            combo.setBackground(CARD_BG);
            combo.setForeground(ACCENT_RED);
            combo.setPreferredSize(new Dimension(160, 32));
            combo.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
                    lbl.setBorder(new EmptyBorder(6, 10, 6, 10));
                    if (isSelected) {
                        lbl.setBackground(LIGHT_PINK_HOVER);
                        lbl.setForeground(ACCENT_RED);
                    } else {
                        lbl.setBackground(CARD_BG);
                        lbl.setForeground(HEADER_DARK);
                    }
                    return lbl;
                }
            });
            return combo;
        }

        private void loadTableList() {
            try (Connection conn = getConnection();
                 ResultSet rs = conn.getMetaData().getTables("AmbulanceDispatchDB", null, "%", new String[]{"TABLE"})) {
                tableComboBox.removeAllItems();
                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (!tableName.equalsIgnoreCase("Ambulance_Equipment") &&
                        !tableName.equalsIgnoreCase("Emergency") &&
                        !tableName.equalsIgnoreCase("Equipment") &&
                        !tableName.equalsIgnoreCase("Hospital_service") &&
                        !tableName.equalsIgnoreCase("Doctor")) {
                        tables.add(tableName);
                    }
                }
                Collections.sort(tables);
                for (String t : tables) tableComboBox.addItem(t);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error loading tables: " + e.getMessage());
            }
        }

        private void onTableChanged() {
            try (Connection conn = getConnection()) {
                currentColumns.clear();
                primaryKeys.clear();
                searchColumnBox.removeAllItems();

                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, currentTable, "%")) {
                    while (rs.next()) {
                        String col = rs.getString("COLUMN_NAME");
                        String type = rs.getString("TYPE_NAME");
                        boolean nullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                        boolean autoInc = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));

                        currentColumns.add(new ColumnMeta(col, type, nullable, autoInc));
                        searchColumnBox.addItem(col);
                    }
                }
                try (ResultSet rs = meta.getPrimaryKeys(conn.getCatalog(), null, currentTable)) {
                    while (rs.next()) {
                        primaryKeys.add(rs.getString("COLUMN_NAME"));
                    }
                }
                refreshTableData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error reading schema: " + e.getMessage());
            }
        }

        public void refreshTableData() {
            if (currentTable == null) return;
            String searchCol = (String) searchColumnBox.getSelectedItem();
            String keyword = searchField.getText().trim();

            StringBuilder query = new StringBuilder("SELECT * FROM `").append(currentTable).append("`");
            boolean filter = searchCol != null && !keyword.isEmpty();
            if (filter) query.append(" WHERE `").append(searchCol).append("` LIKE ?");

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
                if (filter) pstmt.setString(1, "%" + keyword + "%");

                try (ResultSet rs = pstmt.executeQuery()) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    Vector<String> colNames = new Vector<>();
                    for (int i = 1; i <= cols; i++) colNames.add(md.getColumnName(i));

                    Vector<Vector<Object>> rows = new Vector<>();
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        for (int i = 1; i <= cols; i++) row.add(rs.getObject(i));
                        rows.add(row);
                    }
                    tableModel.setDataVector(rows, colNames);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Query error: " + e.getMessage());
            }
        }

        private void handleAdd() {
            if (currentTable == null) return;

            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Record to " + currentTable, true);
            dialog.setSize(450, 520);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());

            JPanel formPanel = new JPanel();
            formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
            formPanel.setBackground(CARD_BG);
            formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

            Map<String, JTextField> fieldMap = new HashMap<>();

            for (ColumnMeta col : currentColumns) {
                if (col.isAutoIncrement) continue;

                JPanel row = new JPanel(new BorderLayout(5, 5));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

                JLabel lbl = new JLabel((col.name + (col.isNullable ? "" : " *") + " [" + col.typeName + "]").toUpperCase());
                lbl.setForeground(HEADER_DARK);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 12));

                JTextField txt = new JTextField();
                txt.setBackground(CARD_BG);
                txt.setForeground(HEADER_DARK);
                txt.setCaretColor(Color.BLACK);
                txt.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)
                ));

                row.add(lbl, BorderLayout.NORTH);
                row.add(txt, BorderLayout.CENTER);
                formPanel.add(row);
                formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

                fieldMap.put(col.name, txt);
            }

            JScrollPane scrollPane = new JScrollPane(formPanel);
            scrollPane.getViewport().setBackground(CARD_BG);
            dialog.add(scrollPane, BorderLayout.CENTER);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnPanel.setBackground(ADMINLTE_BG);

            JButton saveBtn = createStyledRoundedButton("💾 SAVE RECORD");
            JButton cancelBtn = createStyledRoundedButton("❌ CANCEL");

            cancelBtn.addActionListener(e -> dialog.dispose());

            saveBtn.addActionListener(e -> {
                StringBuilder colsBuilder = new StringBuilder();
                StringBuilder valsBuilder = new StringBuilder();
                List<Object> values = new ArrayList<>();

                for (ColumnMeta col : currentColumns) {
                    if (col.isAutoIncrement) continue;
                    JTextField txt = fieldMap.get(col.name);
                    if (txt != null) {
                        String val = txt.getText().trim();
                        if (!val.isEmpty()) {
                            if (colsBuilder.length() > 0) {
                                colsBuilder.append(", ");
                                valsBuilder.append(", ");
                            }
                            colsBuilder.append("`").append(col.name).append("`");
                            valsBuilder.append("?");

                            try {
                                String t = col.typeName.toUpperCase();
                                if (t.contains("INT")) {
                                    values.add(Integer.parseInt(val));
                                } else if (t.contains("DECIMAL") || t.contains("DOUBLE") || t.contains("FLOAT")) {
                                    values.add(Double.parseDouble(val));
                                } else {
                                    values.add(val);
                                }
                            } catch (NumberFormatException ex) {
                                values.add(val);
                            }
                        }
                    }
                }

                if (colsBuilder.length() == 0) {
                    JOptionPane.showMessageDialog(dialog, "Please fill in at least one field.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String insertSql = "INSERT INTO `" + currentTable + "` (" + colsBuilder.toString() + ") VALUES (" + valsBuilder.toString() + ")";

                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

                    for (int i = 0; i < values.size(); i++) {
                        pstmt.setObject(i + 1, values.get(i));
                    }

                    pstmt.executeUpdate();
                    dialog.dispose();
                    showToast((JFrame) SwingUtilities.getWindowAncestor(this), "Record successfully added!");
                    refreshTableData();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Database Insert Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnPanel.add(cancelBtn);
            btnPanel.add(saveBtn);
            dialog.add(btnPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);
        }

        private void handleEdit() {
            int selectedRow = dataTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a row in the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (primaryKeys.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cannot update table without a defined primary key.", "Unsupported Operation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Record in " + currentTable, true);
            dialog.setSize(450, 520);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());

            JPanel formPanel = new JPanel();
            formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
            formPanel.setBackground(CARD_BG);
            formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

            Map<String, JTextField> fieldMap = new HashMap<>();

            for (int i = 0; i < currentColumns.size(); i++) {
                ColumnMeta col = currentColumns.get(i);
                JPanel row = new JPanel(new BorderLayout(5, 5));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

                JLabel lbl = new JLabel((col.name + (primaryKeys.contains(col.name) ? " [PK]" : "") + " [" + col.typeName + "]").toUpperCase());
                lbl.setForeground(HEADER_DARK);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 12));

                JTextField txt = new JTextField();
                txt.setBackground(CARD_BG);
                txt.setForeground(HEADER_DARK);
                txt.setCaretColor(Color.BLACK);
                txt.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)
                ));

                Object valObj = dataTable.getValueAt(selectedRow, i);
                if (valObj != null) {
                    txt.setText(valObj.toString());
                }

                row.add(lbl, BorderLayout.NORTH);
                row.add(txt, BorderLayout.CENTER);
                formPanel.add(row);
                formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

                fieldMap.put(col.name, txt);
            }

            JScrollPane scrollPane = new JScrollPane(formPanel);
            scrollPane.getViewport().setBackground(CARD_BG);
            dialog.add(scrollPane, BorderLayout.CENTER);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnPanel.setBackground(ADMINLTE_BG);

            JButton saveBtn = createStyledRoundedButton("💾 UPDATE RECORD");
            JButton cancelBtn = createStyledRoundedButton("❌ CANCEL");

            cancelBtn.addActionListener(e -> dialog.dispose());

            saveBtn.addActionListener(e -> {
                StringBuilder setClause = new StringBuilder();
                List<Object> updateValues = new ArrayList<>();
                List<Object> pkValues = new ArrayList<>();

                for (ColumnMeta col : currentColumns) {
                    JTextField txt = fieldMap.get(col.name);
                    if (txt != null) {
                        String val = txt.getText().trim();
                        Object parsedVal = val.isEmpty() ? null : val;

                        if (!val.isEmpty()) {
                            try {
                                String t = col.typeName.toUpperCase();
                                if (t.contains("INT")) {
                                    parsedVal = Integer.parseInt(val);
                                } else if (t.contains("DECIMAL") || t.contains("DOUBLE") || t.contains("FLOAT")) {
                                    parsedVal = Double.parseDouble(val);
                                }
                            } catch (NumberFormatException ignored) {}
                        }

                        if (primaryKeys.contains(col.name)) {
                            pkValues.add(parsedVal);
                        } else {
                            if (setClause.length() > 0) setClause.append(", ");
                            setClause.append("`").append(col.name).append("` = ?");
                            updateValues.add(parsedVal);
                        }
                    }
                }

                if (setClause.length() == 0) {
                    JOptionPane.showMessageDialog(dialog, "No fields to update.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                StringBuilder updateSql = new StringBuilder("UPDATE `").append(currentTable).append("` SET ").append(setClause).append(" WHERE ");
                for (int i = 0; i < primaryKeys.size(); i++) {
                    updateSql.append("`").append(primaryKeys.get(i)).append("` = ?");
                    if (i < primaryKeys.size() - 1) updateSql.append(" AND ");
                }

                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(updateSql.toString())) {

                    int index = 1;
                    for (Object val : updateValues) pstmt.setObject(index++, val);
                    for (Object pkVal : pkValues) pstmt.setObject(index++, pkVal);

                    pstmt.executeUpdate();
                    dialog.dispose();
                    showToast((JFrame) SwingUtilities.getWindowAncestor(this), "Record successfully updated!");
                    refreshTableData();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Database Update Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnPanel.add(cancelBtn);
            btnPanel.add(saveBtn);
            dialog.add(btnPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);
        }

        private void handleDelete() {
            int row = dataTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a row to delete.");
                return;
            }
            if (primaryKeys.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cannot delete without a primary key.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Delete selected record?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            String pk = primaryKeys.get(0);
            int pkColIdx = -1;
            for (int i = 0; i < dataTable.getColumnCount(); i++) {
                if (dataTable.getColumnName(i).equalsIgnoreCase(pk)) {
                    pkColIdx = i;
                    break;
                }
            }
            if (pkColIdx == -1) return;
            Object pkVal = dataTable.getValueAt(row, pkColIdx);

            String sql = "DELETE FROM `" + currentTable + "` WHERE `" + pk + "` = ?";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setObject(1, pkVal);
                pstmt.executeUpdate();
                showToast((JFrame) SwingUtilities.getWindowAncestor(this), "Record successfully deleted!");
                refreshTableData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Delete failed (Foreign Key Restriction): " + e.getMessage());
            }
        }
    }

    private static JButton createStyledRoundedButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ADMINLTE_BLUE);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);
                g2.dispose();
            }
        };

        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBackground(CARD_BG);
        btn.setForeground(ADMINLTE_BLUE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(new Color(230, 240, 255));
                btn.setForeground(new Color(0, 123, 255));
            }
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(CARD_BG);
                btn.setForeground(ADMINLTE_BLUE);
            }
        });
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new AmbulanceDispatchApp().setVisible(true);
        });
    }
}
