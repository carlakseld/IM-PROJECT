package gymmanagement.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Shared colors, fonts and small component factories so every screen looks the same. */
public final class Ui {

    public static final Color SIDEBAR        = new Color(24, 31, 46);
    public static final Color SIDEBAR_HOVER  = new Color(38, 48, 70);
    public static final Color SIDEBAR_ACTIVE = new Color(47, 60, 90);
    public static final Color ACCENT         = new Color(59, 130, 246);
    public static final Color BG             = new Color(243, 245, 249);
    public static final Color TEXT           = new Color(33, 37, 41);
    public static final Color MUTED          = new Color(108, 117, 125);
    public static final Color BORDER         = new Color(222, 226, 232);
    public static final Color SUCCESS        = new Color(22, 163, 74);
    public static final Color DANGER         = new Color(220, 53, 69);
    public static final Color GRAY           = new Color(100, 116, 139);

    public static final String PESO = "\u20B1";

    private Ui() { }

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static String money(Object o) {
        double v = (o instanceof Number) ? ((Number) o).doubleValue() : 0;
        return String.format("%s%,.2f", PESO, v);
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(font(Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    /** A white rounded-looking card with a title, used on the dashboard. */
    public static JPanel card(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 14));
        t.setForeground(TEXT);
        p.add(t, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    /** A JTable with the system look: dark header, striped rows, no grid. */
    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(28);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFont(font(Font.PLAIN, 13));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setAutoCreateColumnsFromModel(false);

        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(0, 32));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setOpaque(true);
                setBackground(SIDEBAR);
                setForeground(Color.WHITE);
                setFont(font(Font.BOLD, 12));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setHorizontalAlignment(LEFT);
                return this;
            }
        });

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!sel) {
                    setBackground(r % 2 == 0 ? Color.WHITE : new Color(247, 249, 252));
                    setForeground(TEXT);
                }
                return this;
            }
        });
        return t;
    }
}
