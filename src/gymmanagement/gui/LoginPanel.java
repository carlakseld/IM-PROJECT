package gymmanagement.gui;

import javax.swing.*;
import java.awt.*;

/** Login screen: dark branding on the left, sign-in form on the right. */
public class LoginPanel extends JPanel {

    // change these to whatever username / password your group wants
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin123";

    public interface LoginListener {
        void onLoginSuccess(String userName);
    }

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel errorLabel = new JLabel(" ");
    private final LoginListener loginListener;

    public LoginPanel(LoginListener listener) {
        this.loginListener = listener;
        setLayout(new GridLayout(1, 2));

        add(buildBrandPanel());
        add(buildFormPanel());
    }

    private JPanel buildBrandPanel() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Ui.SIDEBAR, getWidth(), getHeight(), new Color(30, 64, 175)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        p.setLayout(new GridBagLayout());

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("GYM");
        logo.setFont(Ui.font(Font.BOLD, 64));
        logo.setForeground(Color.WHITE);

        JLabel name = new JLabel("Management System");
        name.setFont(Ui.font(Font.PLAIN, 24));
        name.setForeground(new Color(191, 219, 254));

        JLabel tag = new JLabel("Members  |  Memberships  |  Training  |  Payments");
        tag.setFont(Ui.font(Font.PLAIN, 13));
        tag.setForeground(new Color(148, 163, 184));

        box.add(logo);
        box.add(name);
        box.add(Box.createVerticalStrut(18));
        box.add(tag);
        p.add(box);
        return p;
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);

        JLabel title = new JLabel("Welcome back");
        title.setFont(Ui.font(Font.BOLD, 28));
        title.setForeground(Ui.TEXT);
        gbc.gridy = 0;
        p.add(title, gbc);

        JLabel sub = new JLabel("Sign in to manage the gym");
        sub.setFont(Ui.font(Font.PLAIN, 14));
        sub.setForeground(Ui.MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 22, 0);
        p.add(sub, gbc);

        gbc.insets = new Insets(8, 0, 2, 0);
        gbc.gridy = 2;
        p.add(label("Username"), gbc);
        usernameField.setPreferredSize(new Dimension(280, 36));
        usernameField.setFont(Ui.font(Font.PLAIN, 14));
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.gridy = 3;
        p.add(usernameField, gbc);

        gbc.insets = new Insets(8, 0, 2, 0);
        gbc.gridy = 4;
        p.add(label("Password"), gbc);
        passwordField.setPreferredSize(new Dimension(280, 36));
        passwordField.setFont(Ui.font(Font.PLAIN, 14));
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.gridy = 5;
        p.add(passwordField, gbc);

        errorLabel.setForeground(Ui.DANGER);
        errorLabel.setFont(Ui.font(Font.PLAIN, 12));
        gbc.insets = new Insets(2, 0, 8, 0);
        gbc.gridy = 6;
        p.add(errorLabel, gbc);

        JButton login = Ui.button("Sign In", Ui.ACCENT);
        login.setFont(Ui.font(Font.BOLD, 15));
        login.setPreferredSize(new Dimension(280, 40));
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy = 7;
        p.add(login, gbc);

        login.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Ui.font(Font.BOLD, 12));
        l.setForeground(Ui.TEXT);
        return l;
    }

    private void attemptLogin() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();

        if (user.equals(USERNAME) && pass.equals(PASSWORD)) {
            errorLabel.setText(" ");
            if (loginListener != null) loginListener.onLoginSuccess(user);
        } else {
            errorLabel.setText("Invalid username or password");
        }
    }

    /** Clears the form (called after logout). */
    public void reset() {
        usernameField.setText("");
        passwordField.setText("");
        errorLabel.setText(" ");
    }
}
