package gymmanagement.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/** Main screen after login: dark sidebar on the left, the selected page on the right. */
public class DashboardPanel extends JPanel {

    public interface LogoutListener {
        void onLogout();
    }

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel sideBar = new JPanel();
    private final Map<String, JButton> navButtons = new HashMap<String, JButton>();
    private final Map<String, Runnable> refreshers = new HashMap<String, Runnable>();
    private String current;

    public DashboardPanel(String userName, LogoutListener logoutListener) {
        setLayout(new BorderLayout());

        // ---------- pages
        HomePanel home = new HomePanel(userName);
        gymmanagement.gui.CrudPanel members = Modules.members();
        CrudPanel dayPass = Modules.dayPasses();
        CrudPanel memberships = Modules.memberships();
        CrudPanel payments = Modules.payments();
        CrudPanel trainers = Modules.trainers();
        CrudPanel sessions = Modules.sessions();
        CrudPanel attendance = Modules.attendance();
        CrudPanel staff = Modules.staff();
        ReportsPanel reports = new ReportsPanel();

        addPage("Dashboard", home, home::refresh);
        addPage("Members", members, members::refresh);
        addPage("Day Pass", dayPass, dayPass::refresh);
        addPage("Memberships", memberships, memberships::refresh);
        addPage("Payments", payments, payments::refresh);
        addPage("Trainers", trainers, trainers::refresh);
        addPage("Sessions", sessions, sessions::refresh);
        addPage("Attendance", attendance, attendance::refresh);
        addPage("Staff", staff, staff::refresh);
        addPage("Reports", reports, reports::refresh);

        // ---------- sidebar
        sideBar.setLayout(new BoxLayout(sideBar, BoxLayout.Y_AXIS));
        sideBar.setBackground(Ui.SIDEBAR);
        sideBar.setPreferredSize(new Dimension(235, 0));
        sideBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        sideBar.add(brand());
        addNav("Dashboard", "Dashboard");

        addSection("CUSTOMERS");
        addNav("Members", "Members");
        addNav("Day Pass", "Day Pass");

        addSection("SALES");
        addNav("Memberships", "Memberships");
        addNav("Transactions & Payments", "Payments");

        addSection("TRAINING");
        addNav("Trainers", "Trainers");
        addNav("Training Sessions", "Sessions");
        addNav("Session Attendance", "Attendance");

        addSection("ADMIN");
        addNav("Staff", "Staff");
        addNav("Reports", "Reports");

        sideBar.add(Box.createVerticalGlue());

        JLabel user = new JLabel("Signed in as " + userName);
        user.setFont(Ui.font(Font.PLAIN, 12));
        user.setForeground(new Color(148, 163, 184));
        user.setAlignmentX(Component.LEFT_ALIGNMENT);
        user.setBorder(BorderFactory.createEmptyBorder(0, 22, 8, 0));
        sideBar.add(user);

        JButton logout = Ui.button("Logout", Ui.DANGER);
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(195, 38));
        logout.addActionListener(e -> {
            if (logoutListener != null) logoutListener.onLogout();
        });
        JPanel logoutWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        logoutWrap.setOpaque(false);
        logoutWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        logoutWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutWrap.add(logout);
        sideBar.add(logoutWrap);

        add(sideBar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        navigate("Dashboard");
    }

    // ------------------------------------------------------------------ building blocks
    private JPanel brand() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        p.setBorder(BorderFactory.createEmptyBorder(22, 22, 18, 10));
        JLabel a = new JLabel("GYM");
        a.setFont(Ui.font(Font.BOLD, 28));
        a.setForeground(Color.WHITE);
        JLabel b = new JLabel("Management System");
        b.setFont(Ui.font(Font.PLAIN, 12));
        b.setForeground(new Color(148, 163, 184));
        p.add(a);
        p.add(b);
        return p;
    }

    private void addSection(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Ui.font(Font.BOLD, 11));
        l.setForeground(new Color(100, 116, 139));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(16, 22, 6, 0));
        sideBar.add(l);
    }

    private void addNav(String text, final String page) {
        final JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(Ui.font(Font.PLAIN, 14));
        b.setForeground(new Color(226, 232, 240));
        b.setBackground(Ui.SIDEBAR);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(navBorder(false));
        b.addActionListener(e -> navigate(page));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!page.equals(current)) b.setBackground(Ui.SIDEBAR_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!page.equals(current)) b.setBackground(Ui.SIDEBAR);
            }
        });
        navButtons.put(page, b);
        sideBar.add(b);
    }

    private javax.swing.border.Border navBorder(boolean active) {
        Color stripe = active ? Ui.ACCENT : Ui.SIDEBAR;
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, stripe),
                BorderFactory.createEmptyBorder(0, 18, 0, 10));
    }

    private void addPage(String name, JComponent page, Runnable refresh) {
        content.add(page, name);
        refreshers.put(name, refresh);
    }

    // ------------------------------------------------------------------ navigation
    private void navigate(String page) {
        current = page;
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean active = e.getKey().equals(page);
            e.getValue().setBackground(active ? Ui.SIDEBAR_ACTIVE : Ui.SIDEBAR);
            e.getValue().setBorder(navBorder(active));
            e.getValue().setForeground(active ? Color.WHITE : new Color(226, 232, 240));
        }
        cards.show(content, page);
        try {
            refreshers.get(page).run();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
