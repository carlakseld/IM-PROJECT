package gymmanagement.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A full "table + form" screen (same idea as the barangay template:
 * table on top, form with Add / Update / Delete / Clear at the bottom).
 *
 * Columns are written as  "key:Header"  or  "#key"  (a leading # = hidden column
 * that only stores a raw value such as an ID).  A form field is linked to the
 * column with the same key, so selecting a row fills the form automatically.
 */
public abstract class CrudPanel extends JPanel {

    private final List<String> keys = new ArrayList<String>();
    private final List<Field> fields;
    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField searchField = new JTextField(18);
    private final JButton btnAdd = Ui.button("Add", Ui.SUCCESS);
    private final JButton btnUpdate = Ui.button("Update", Ui.ACCENT);
    private final JButton btnDelete = Ui.button("Delete", Ui.DANGER);
    private final JButton btnClear = Ui.button("Clear Form", Ui.GRAY);

    protected CrudPanel(String title, String subtitle, String[] columns, Field... fieldArray) {
        this.fields = Arrays.asList(fieldArray);

        // ---- parse the column definitions
        List<String> headers = new ArrayList<String>();
        final Set<Integer> hidden = new HashSet<Integer>();
        for (int i = 0; i < columns.length; i++) {
            String col = columns[i];
            boolean hid = col.startsWith("#");
            if (hid) col = col.substring(1);
            String key = col;
            String header = col;
            int colon = col.indexOf(':');
            if (colon >= 0) {
                key = col.substring(0, colon);
                header = col.substring(colon + 1);
            }
            keys.add(key);
            headers.add(header);
            if (hid) hidden.add(i);
        }

        model = new DefaultTableModel(headers.toArray(new String[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = Ui.table(model);
        sorter = new TableRowSorter<DefaultTableModel>(model);
        table.setRowSorter(sorter);

        TableColumnModel cm = table.getColumnModel();
        for (int i = cm.getColumnCount() - 1; i >= 0; i--) {
            TableColumn tc = cm.getColumn(i);
            if (hidden.contains(tc.getModelIndex())) cm.removeColumn(tc);
        }
        if (cm.getColumnCount() > 0) {           // keep the ID column narrow
            cm.getColumn(0).setPreferredWidth(70);
            cm.getColumn(0).setMaxWidth(110);
        }

        // ---- layout
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setBackground(Ui.BG);

        add(buildHeader(title, subtitle), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        add(buildForm(), BorderLayout.SOUTH);

        btnUpdate.setVisible(canUpdate());

        // ---- behavior
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int viewRow = table.getSelectedRow();
            if (viewRow == -1) return;
            populate(table.convertRowIndexToModel(viewRow));
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        btnAdd.addActionListener(e -> onAdd());
        btnUpdate.addActionListener(e -> onUpdate());
        btnDelete.addActionListener(e -> onDelete());
        btnClear.addActionListener(e -> clearForm());
    }

    // ================================================================ to be written by each module

    /** Return one Object[] per row, in the same order as the columns (including hidden ones). */
    protected abstract List<Object[]> load();

    /** Save a new record. {@code v} maps field key -> text typed by the user. */
    protected abstract void insert(Map<String, String> v);

    /** {@code old} = values of the selected row, {@code v} = values now in the form. */
    protected abstract void update(Map<String, String> old, Map<String, String> v);

    /** {@code old} = values of the selected row. */
    protected abstract void delete(Map<String, String> old);

    /** Modules without an Update button (like Attendance) return false. */
    protected boolean canUpdate() {
        return true;
    }

    // ================================================================ public

    /** Reloads the drop-downs and the table from the database. */
    public void refresh() {
        try {
            for (Field f : fields) f.reloadLookup();
            List<Object[]> rows = load();
            model.setRowCount(0);
            for (Object[] r : rows) model.addRow(r);
            table.clearSelection();
        } catch (IllegalStateException ex) {
            dbError(ex.getMessage());
        }
    }

    // ================================================================ UI building

    private JPanel buildHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Ui.font(Font.BOLD, 24));
        t.setForeground(Ui.TEXT);
        JLabel s = new JLabel(subtitle);
        s.setFont(Ui.font(Font.PLAIN, 13));
        s.setForeground(Ui.MUTED);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        right.setOpaque(false);
        JLabel lbl = new JLabel("Search:");
        lbl.setFont(Ui.font(Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(220, 30));
        right.add(lbl);
        right.add(searchField);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.BORDER),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int perRow = 3;
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            int row = i / perRow;
            int col = (i % perRow) * 2;

            JLabel lbl = new JLabel(f.label + ":");
            lbl.setFont(Ui.font(Font.PLAIN, 13));
            lbl.setForeground(Ui.TEXT);

            gbc.gridy = row;
            gbc.gridx = col;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            form.add(lbl, gbc);

            gbc.gridx = col + 1;
            gbc.weightx = 1;
            form.add(f.getComponent(), gbc);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        gbc.gridy = (fields.size() + perRow - 1) / perRow;
        gbc.gridx = 0;
        gbc.gridwidth = perRow * 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(12, 6, 2, 6);
        form.add(buttons, gbc);
        return form;
    }

    // ================================================================ actions

    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
    }

    private Map<String, String> rowMap(int modelRow) {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < keys.size(); i++) {
            Object o = model.getValueAt(modelRow, i);
            m.put(keys.get(i), o == null ? "" : o.toString());
        }
        return m;
    }

    private void populate(int modelRow) {
        Map<String, String> r = rowMap(modelRow);
        for (Field f : fields) {
            if (r.containsKey(f.key)) f.setValue(r.get(f.key));
            f.lock(f.isCreateOnly());
        }
    }

    private Map<String, String> collect() {
        Map<String, String> v = new HashMap<String, String>();
        for (Field f : fields) {
            String val = f.getValue();
            f.validate(val);
            v.put(f.key, val);
        }
        return v;
    }

    private void clearForm() {
        table.clearSelection();
        for (Field f : fields) {
            f.reset();
            f.lock(false);
        }
    }

    private void onAdd() {
        try {
            insert(collect());
            refresh();
            clearForm();
            JOptionPane.showMessageDialog(this, "Record added successfully!");
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        } catch (IllegalStateException ex) {
            dbError(ex.getMessage());
        }
    }

    private void onUpdate() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a record to update.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Map<String, String> old = rowMap(table.convertRowIndexToModel(viewRow));
            update(old, collect());
            refresh();
            clearForm();
            JOptionPane.showMessageDialog(this, "Record updated successfully!");
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        } catch (IllegalStateException ex) {
            dbError(ex.getMessage());
        }
    }

    private void onDelete() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a record to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            delete(rowMap(table.convertRowIndexToModel(viewRow)));
            refresh();
            clearForm();
            JOptionPane.showMessageDialog(this, "Record deleted.");
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        } catch (IllegalStateException ex) {
            dbError(ex.getMessage());
        }
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void dbError(String message) {
        JOptionPane.showMessageDialog(this, message, "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}
