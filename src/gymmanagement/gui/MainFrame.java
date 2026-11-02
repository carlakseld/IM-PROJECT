package gymmanagement.gui;

import gymmanagement.db.Db;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private static final String LOGIN_CARD = "LOGIN";
    private static final String DASHBOARD_CARD = "DASHBOARD";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel container = new JPanel(cardLayout);
    private final LoginPanel loginPanel;
    private DashboardPanel dashboardPanel;

    public MainFrame() {
        setTitle("Gym Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 800);
        setMinimumSize(new Dimension(1150, 700));
        setLocationRelativeTo(null);

        loginPanel = new LoginPanel(user -> showDashboard(user));
        container.add(loginPanel, LOGIN_CARD);
        add(container);
        cardLayout.show(container, LOGIN_CARD);
    }

    private void showDashboard(String user) {
        // The dashboard is built AFTER login so it always loads fresh data
        try {
            Db.testConnection();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database Connection Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (dashboardPanel != null) container.remove(dashboardPanel);
        dashboardPanel = new DashboardPanel(user, () -> confirmLogout());
        container.add(dashboardPanel, DASHBOARD_CARD);
        container.revalidate();
        cardLayout.show(container, DASHBOARD_CARD);
    }

    private void confirmLogout() {
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            loginPanel.reset();
            cardLayout.show(container, LOGIN_CARD);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
