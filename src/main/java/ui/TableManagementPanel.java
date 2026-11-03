import dao.DatabaseMetadataDAO;
import dao.GenericTableDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableManagementPanel extends JPanel {
    private final DatabaseMetadataDAO metadataDAO = new DatabaseMetadataDAO();
    private final GenericTableDAO tableDAO = new GenericTableDAO();
    
    private JComboBox<String> tableComboBox;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> searchColumnBox;
    
    private String currentTable;
    private List<DatabaseMetadataDAO.ColumnInfo> currentColumns = new ArrayList<>();
    private List<String> currentPrimaryKeys = new ArrayList<>();

    public TableManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Control Toolbar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        topBar.add(new JLabel("Select Table:"));
        tableComboBox = new JComboBox<>();
        loadTableList();
        topBar.add(tableComboBox);

        topBar.add(Box.createHorizontalStrut(15));
        topBar.add(new JLabel("Search Column:"));
        searchColumnBox = new JComboBox<>();
        topBar.add(searchColumnBox);

        topBar.add(new JLabel("Keyword:"));
        searchField = new JTextField(12);
        topBar.add(searchField);

        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(new Color(37, 99, 235));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.addActionListener(e -> refreshTableData());
        topBar.add(searchBtn);

        add(topBar, BorderLayout.NORTH);

        // Center Table View
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        dataTable = new JTable(tableModel);
        dataTable.setRowHeight(25);
        dataTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        add(new JScrollPane(dataTable), BorderLayout.CENTER);

        // Bottom Action Toolbar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        bottomBar.setOpaque(false);

        JButton addBtn = new JButton("Add New Record");
        JButton editBtn = new JButton("Edit Selected");
        JButton deleteBtn = new JButton("Delete Record");
        JButton refreshBtn = new JButton("Refresh");

        addBtn.setBackground(new Color(13, 148, 136));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);

        editBtn.setBackground(new Color(217, 119, 6));
        editBtn.setForeground(Color.WHITE);
        editBtn.setFocusPainted(false);

        deleteBtn.setBackground(new Color(220, 38, 38));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);

        addBtn.addActionListener(e -> handleAddRecord());
        editBtn.addActionListener(e -> handleEditRecord());
        deleteBtn.addActionListener(e -> handleDeleteRecord());
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

        if (tableComboBox.getItemCount() > 0) {
            tableComboBox.setSelectedIndex(0);
        }
    }

    private void loadTableList() {
        try {
            List<String> tables = metadataDAO.getTableNames();
            tableComboBox.removeAllItems();
            for (String t : tables) {
                tableComboBox.addItem(t);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading tables: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onTableChanged() {
        try {
            currentColumns = metadataDAO.getTableColumns(currentTable);
            currentPrimaryKeys = metadataDAO.getPrimaryKeys(currentTable);
            
            searchColumnBox.removeAllItems();
            for (DatabaseMetadataDAO.ColumnInfo col : currentColumns) {
                searchColumnBox.addItem(col.name);
            }
            refreshTableData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error inspecting table schema: " + e.getMessage(), "Schema Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshTableData() {
        if (currentTable == null) return;
        try {
            String searchCol = (String) searchColumnBox.getSelectedItem();
            String keyword = searchField.getText();
            
            GenericTableDAO.TableData data = tableDAO.fetchTableData(currentTable, searchCol, keyword, 100, 0);
            
            Vector<String> colNames = new Vector<>(data.columns);
            Vector<Vector<Object>> rowData = new Vector<>();
            for (List<Object> row : data.rows) {
                rowData.add(new Vector<>(row));
            }
            tableModel.setDataVector(rowData, colNames);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching data: " + e.getMessage(), "Query Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleAddRecord() {
        if (currentTable == null) return;
        RecordFormDialog dialog = new RecordFormDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Record to " + currentTable, currentColumns, null);
        dialog.setVisible(true);

        if (dialog.isSubmitted()) {
            Map<String, String> formValues = dialog.getFieldValues();
            List<String> insertCols = new ArrayList<>();
            List<Object> insertVals = new ArrayList<>();

            for (DatabaseMetadataDAO.ColumnInfo col : currentColumns) {
                if (col.isAutoIncrement) continue;
                String val = formValues.get(col.name);
                if (val != null && !val.isEmpty()) {
                    insertCols.add(col.name);
                    insertVals.add(parseValue(col.typeName, val));
                } else if (!col.isNullable && col.defaultValue == null) {
                    JOptionPane.showMessageDialog(this, "Required field missing: " + col.name, "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            try {
                tableDAO.insertRecord(currentTable, insertCols, insertVals);
                JOptionPane.showMessageDialog(this, "Record successfully inserted!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTableData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Insert failed: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleEditRecord() {
        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a record to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentPrimaryKeys.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot update table without a defined primary key.", "Unsupported Operation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Object> existingValues = new ArrayList<>();
        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            existingValues.add(dataTable.getValueAt(selectedRow, i));
        }

        RecordFormDialog dialog = new RecordFormDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Record in " + currentTable, currentColumns, existingValues);
        dialog.setVisible(true);

        if (dialog.isSubmitted()) {
            Map<String, String> formValues = dialog.getFieldValues();
            List<String> updateCols = new ArrayList<>();
            List<Object> updateVals = new ArrayList<>();
            List<Object> pkVals = new ArrayList<>();

            for (int i = 0; i < currentColumns.size(); i++) {
                DatabaseMetadataDAO.ColumnInfo col = currentColumns.get(i);
                if (currentPrimaryKeys.contains(col.name)) {
                    pkVals.add(existingValues.get(i));
                    continue;
                }
                String val = formValues.get(col.name);
                if (val != null) {
                    updateCols.add(col.name);
                    updateVals.add(val.isEmpty() ? null : parseValue(col.typeName, val));
                }
            }

            try {
                tableDAO.updateRecord(currentTable, updateCols, updateVals, currentPrimaryKeys, pkVals);
                JOptionPane.showMessageDialog(this, "Record successfully updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTableData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Update failed: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteRecord() {
        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a record to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentPrimaryKeys.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot delete record without a defined primary key.", "Unsupported Operation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        List<Object> pkVals = new ArrayList<>();
        for (DatabaseMetadataDAO.ColumnInfo col : currentColumns) {
            if (currentPrimaryKeys.contains(col.name)) {
                int colIdx = -1;
                for (int i = 0; i < dataTable.getColumnCount(); i++) {
                    if (dataTable.getColumnName(i).equalsIgnoreCase(col.name)) {
                        colIdx = i;
                        break;
                    }
                }
                if (colIdx != -1) {
                    pkVals.add(dataTable.getValueAt(selectedRow, colIdx));
                }
            }
        }

        try {
            tableDAO.deleteRecord(currentTable, currentPrimaryKeys, pkVals);
            JOptionPane.showMessageDialog(this, "Record successfully deleted!", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshTableData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Delete failed (Foreign Key Restriction or constraint violation): " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Object parseValue(String typeName, String val) {
        try {
            typeName = typeName.toUpperCase();
            if (typeName.contains("INT")) {
                return Integer.parseInt(val);
            } else if (typeName.contains("DECIMAL") || typeName.contains("DOUBLE") || typeName.contains("FLOAT")) {
                return Double.parseDouble(val);
            }
        } catch (NumberFormatException ignored) {}
        return val;
    }
}