import dao.DatabaseMetadataDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordFormDialog extends JDialog {
    private final Map<String, JTextField> fieldMap = new HashMap<>();
    private boolean submitted = false;

    public RecordFormDialog(Frame owner, String title, List<DatabaseMetadataDAO.ColumnInfo> columns, List<Object> existingValues) {
        super(owner, title, true);
        setSize(450, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        formPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        for (int i = 0; i < columns.size(); i++) {
            DatabaseMetadataDAO.ColumnInfo col = columns.get(i);
            if (col.isAutoIncrement) continue; // Skip auto-increment primary keys

            JPanel row = new JPanel(new BorderLayout(5, 5));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            JLabel lbl = new JLabel(col.name + (col.isNullable ? "" : " *"));
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lbl.setForeground(new Color(51, 65, 85));

            JTextField txt = new JTextField();
            txt.setPreferredSize(new Dimension(200, 30));
            if (existingValues != null && i < existingValues.size() && existingValues.get(i) != null) {
                txt.setText(existingValues.get(i).toString());
            }

            row.add(lbl, BorderLayout.NORTH);
            row.add(txt, BorderLayout.CENTER);
            formPanel.add(row);
            formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            fieldMap.put(col.name, txt);
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(new Color(241, 245, 249));
        JButton cancelBtn = new JButton("Cancel");
        JButton saveBtn = new JButton("Save Record");

        saveBtn.setBackground(new Color(13, 148, 136));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);

        cancelBtn.addActionListener(e -> dispose());
        saveBtn.addActionListener(e -> {
            submitted = true;
            dispose();
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public Map<String, String> getFieldValues() {
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<String, JTextField> entry : fieldMap.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getText().trim());
        }
        return values;
    }
}