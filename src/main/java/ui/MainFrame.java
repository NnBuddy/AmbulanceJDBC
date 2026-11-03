import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private final DashboardPanel dashboardPanel;
    private final TableManagementPanel tableManagementPanel;

    public MainFrame() {
        setTitle("Ambulance Dispatch Management System");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // Sidebar Navigation
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(15, 23, 42)); // Dark navy
        sidebar.setPreferredSize(new Dimension(220, getHeight()));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel logoLabel = new JLabel("🚑 AMBULANCE SYS");
        logoLabel.setForeground(new Color(248, 113, 113)); // Emergency Red accent
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logoLabel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        JButton dashboardBtn = createNavButton("Dashboard");
        JButton tablesBtn = createNavButton("Table Management");

        sidebar.add(dashboardBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(tablesBtn);

        add(sidebar, BorderLayout.WEST);

        // Content Area
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);

        dashboardPanel = new DashboardPanel();
        tableManagementPanel = new TableManagementPanel();

        mainContentPanel.add(dashboardPanel, "DASHBOARD");
        mainContentPanel.add(tableManagementPanel, "TABLES");

        add(mainContentPanel, BorderLayout.CENTER);

        dashboardBtn.addActionListener(e -> {
            cardLayout.show(mainContentPanel, "DASHBOARD");
            dashboardPanel.loadDashboardData();
        });
        tablesBtn.addActionListener(e -> {
            cardLayout.show(mainContentPanel, "TABLES");
            tableManagementPanel.refreshTableData();
        });
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(200, 40));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBackground(new Color(30, 41, 59));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT; // corrected formatting syntax
        return btn;
    }
}