import dao.DatabaseMetadataDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class DashboardPanel extends JPanel {
    private final DatabaseMetadataDAO metadataDAO = new DatabaseMetadataDAO();
    private final JPanel cardsPanel;

    public DashboardPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("Operations Command Center");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(new Color(30, 41, 59));
        
        JButton refreshBtn = new JButton("Refresh Statistics");
        refreshBtn.setBackground(new Color(13, 148, 136));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadDashboardData());
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        cardsPanel = new JPanel(new GridLayout(1, 5, 15, 0));
        cardsPanel.setOpaque(false);
        add(cardsPanel, BorderLayout.CENTER);

        loadDashboardData();
    }

    public void loadDashboardData() {
        cardsPanel.removeAll();
        Map<String, Long> counts = metadataDAO.getDashboardCounts();
        
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            JPanel card = createStatCard(entry.getKey(), String.valueOf(entry.getValue()));
            cardsPanel.add(card);
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel createStatCard(String title, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLbl.setForeground(new Color(100, 116, 139));
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("SansSerif", Font.BOLD, 32));
        valLbl.setForeground(new Color(15, 23, 42));
        valLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLbl);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(valLbl);
        return card;
    }
}