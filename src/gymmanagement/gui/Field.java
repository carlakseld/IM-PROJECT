package gymmanagement.gui;

import gymmanagement.db.Db;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * One input in a form: text box, date box, number box, fixed drop-down, or a
 * drop-down filled from the database (LOOKUP, shows a name but keeps the ID).
 */
public class Field {

    public enum Type { TEXT, DATE, NUMBER, COMBO, LOOKUP }

    /** Drop-down item that shows a label but remembers an ID. */
    public static final class Item {
        public final String id;
        public final String label;

        public Item(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public final String key;
    public final String label;
    public final Type type;
    private final JComponent comp;
    private String lookupSql;
    private boolean optional;
    private boolean createOnly;
    private String regex;
    private String regexMsg;

    private Field(String key, String label, Type type, JComponent comp) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.comp = comp;
        comp.setPreferredSize(new Dimension(190, 30));
        comp.setFont(Ui.font(Font.PLAIN, 13));
    }

    // ------------------------------------------------------------ factories

    public static Field text(String key, String label) {
        return new Field(key, label, Type.TEXT, new JTextField());
    }

    public static Field date(String key, String label) {
        return new Field(key, label, Type.DATE, new JTextField(LocalDate.now().toString()));
    }

    public static Field number(String key, String label) {
        return new Field(key, label, Type.NUMBER, new JTextField());
    }

    public static Field combo(String key, String label, String... options) {
        return new Field(key, label, Type.COMBO, new JComboBox<Object>(options));
    }

    /** SQL must return 2 columns: ID, then the text to show. */
    public static Field lookup(String key, String label, String sql) {
        Field f = new Field(key, label, Type.LOOKUP, new JComboBox<Object>());
        f.lookupSql = sql;
        return f;
    }

    // ------------------------------------------------------------ options

    /** This field may be left empty. */
    public Field optional() {
        this.optional = true;
        return this;
    }

    /** This field can only be chosen when adding; it is locked when editing an existing record. */
    public Field createOnly() {
        this.createOnly = true;
        return this;
    }

    public Field regex(String pattern, String message) {
        this.regex = pattern;
        this.regexMsg = message;
        return this;
    }

    public boolean isCreateOnly() {
        return createOnly;
    }

    public JComponent getComponent() {
        return comp;
    }

    // ------------------------------------------------------------ values

    @SuppressWarnings("unchecked")
    private JComboBox<Object> box() {
        return (JComboBox<Object>) comp;
    }

    public String getValue() {
        if (comp instanceof JTextField) {
            return ((JTextField) comp).getText().trim();
        }
        Object s = box().getSelectedItem();
        if (s == null) return "";
        if (s instanceof Item) return ((Item) s).id;
        return s.toString();
    }

    public void setValue(String v) {
        if (v == null) v = "";
        if (comp instanceof JTextField) {
            ((JTextField) comp).setText(v);
            return;
        }
        JComboBox<Object> b = box();
        for (int i = 0; i < b.getItemCount(); i++) {
            Object o = b.getItemAt(i);
            String id = (o instanceof Item) ? ((Item) o).id : o.toString();
            if (id.equalsIgnoreCase(v)) {
                b.setSelectedIndex(i);
                return;
            }
        }
    }

    public void reset() {
        if (type == Type.DATE) {
            ((JTextField) comp).setText(LocalDate.now().toString());
        } else if (comp instanceof JTextField) {
            ((JTextField) comp).setText("");
        } else if (box().getItemCount() > 0) {
            box().setSelectedIndex(0);
        }
    }

    public void lock(boolean locked) {
        comp.setEnabled(!locked);
    }

    /** Fills a LOOKUP drop-down from the database (keeps the current selection if possible). */
    public void reloadLookup() {
        if (type != Type.LOOKUP) return;
        String keep = getValue();
        DefaultComboBoxModel<Object> m = new DefaultComboBoxModel<Object>();
        if (optional) m.addElement(new Item("", "(none)"));
        for (Object[] r : Db.query(lookupSql)) {
            m.addElement(new Item(String.valueOf(r[0]), String.valueOf(r[1])));
        }
        box().setModel(m);
        if (m.getSize() > 0) box().setSelectedIndex(0);
        setValue(keep);
    }

    /** Throws IllegalArgumentException with a readable message if the value is not acceptable. */
    public void validate(String val) {
        if (val.isEmpty()) {
            if (!optional) throw new IllegalArgumentException(label + " is required.");
            return;
        }
        if (type == Type.DATE) {
            try {
                LocalDate.parse(val);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(label + " must be in YYYY-MM-DD format.");
            }
        } else if (type == Type.NUMBER) {
            try {
                new BigDecimal(val);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(label + " must be a valid number.");
            }
        }
        if (regex != null && !val.matches(regex)) {
            throw new IllegalArgumentException(label + " " + regexMsg);
        }
    }
}
