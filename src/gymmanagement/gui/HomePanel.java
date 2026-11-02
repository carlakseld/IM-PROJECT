package gymmanagement.gui;

import gymmanagement.db.Db;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** The Dashboard home page: stat cards, monthly revenue chart and recent activity. */
public class HomePanel extends JPanel {

    // ------------------------------------------------------------------ stat card
    private static final class StatCard extends JPanel {
        final JLabel value = new JLabel("-");
        private final Color color;

        StatCard(String title, String sub, Color color) {
            this.color = color;
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

            JLabel t = new JLabel(title.toUpperCase());
            t.setFont(Ui.font(Font.BOLD, 11));
            t.setForeground(new Color(255, 255, 255, 215));

            value.setFont(Ui.font(Font.BOLD, 28));
            value.setForeground(Color.WHITE);

            JLabel s = new JLabel(sub);
            s.setFont(Ui.font(Font.PLAIN, 12));
            s.setForeground(new Color(255, 255, 255, 200));

            add(t, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
            add(s, BorderLayout.SOUTH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, color, getWidth(), getHeight(), color.darker()));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------------ bar chart
    private static final class BarChart extends JPanel {
        private String[] labels = new String[0];
        private double[] values = new double[0];

        BarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(300, 200));
        }

        void setData(String[] labels, double[] values) {
            this.labels = labels;
            this.values = values;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int left = 8;
            int bottom = 22;
            int top = 20;
            int chartH = h - bottom - top;
            if (values.length == 0 || chartH <= 0) {
                g2.dispose();
                return;
            }

            double max = 0;
            for (double v : values) max = Math.max(max, v);
            if (max <= 0) max = 1;

            // grid lines
            g2.setColor(new Color(235, 238, 243));
            for (int i = 0; i <= 3; i++) {
                int y = top + chartH - (int) (chartH * i / 3.0);
                g2.drawLine(left, y, w - left, y);
            }

            int n = values.length;
            int slot = (w - left * 2) / n;
            int barW = Math.min(36, slot - 12);
            g2.setFont(Ui.font(Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < n; i++) {
                int bh = (int) (chartH * (values[i] / max));
                int x = left + i * slot + (slot - barW) / 2;
                int y = top + chartH - bh;
                g2.setPaint(new GradientPaint(0, y, new Color(96, 165, 250), 0, y + Math.max(bh, 1), new Color(37, 99, 235)));
                if (bh > 0) g2.fillRoundRect(x, y, barW, bh, 8, 8);

                g2.setColor(Ui.MUTED);
                String lab = labels[i];
                g2.drawString(lab, x + (barW - fm.stringWidth(lab)) / 2, h - 6);

                if (values[i] > 0) {
                    String val = values[i] >= 1000 ? String.format("%.1fk", values[i] / 1000.0) : String.format("%.0f", values[i]);
                    g2.setColor(Ui.TEXT);
                    g2.drawString(val, x + (barW - fm.stringWidth(val)) / 2, y - 4);
                }
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ the page
    private final StatCard cMembers = new StatCard("Total Members", "registered members", new Color(37, 99, 235));
    private final StatCard cActive = new StatCard("Active Memberships", "currently active", new Color(22, 163, 74));
    private final StatCard cExpiring = new StatCard("Expiring Soon", "within the next 30 days", new Color(234, 88, 12));
    private final StatCard cDayPass = new StatCard("Day Passes", "sold this month", new Color(124, 58, 237));
    private final StatCard cRevenue = new StatCard("Total Revenue", "all transactions", new Color(13, 148, 136));
    private final StatCard cMonth = new StatCard("Revenue This Month", "memberships + day passes", new Color(79, 70, 229));
    private final StatCard cTrainers = new StatCard("Trainers", "on the team", new Color(219, 39, 119));
    private final StatCard cSessions = new StatCard("Training Sessions", "held this month", new Color(220, 38, 38));

    private final BarChart chart = new BarChart();
    private final DefaultTableModel recentModel = new DefaultTableModel(new String[]{"Date", "Customer", "Amount"}, 0);
    private final DefaultTableModel expiringModel = new DefaultTableModel(new String[]{"Member", "Plan", "Days Left"}, 0);

    public HomePanel(String userName) {
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setBackground(Ui.BG);

        // ---- welcome header
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel welcome = new JLabel("Welcome back, " + userName + "!");
        welcome.setFont(Ui.font(Font.BOLD, 24));
        welcome.setForeground(Ui.TEXT);
        JLabel date = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH))
                + "  |  Here is what is happening in the gym today");
        date.setFont(Ui.font(Font.PLAIN, 13));
        date.setForeground(Ui.MUTED);
        header.add(welcome);
        header.add(Box.createVerticalStrut(2));
        header.add(date);
        add(header, BorderLayout.NORTH);

        // ---- body: cards on top, widgets below
        JPanel cards = new JPanel(new GridLayout(2, 4, 14, 14));
        cards.setOpaque(false);
        cards.add(cMembers);
        cards.add(cActive);
        cards.add(cExpiring);
        cards.add(cDayPass);
        cards.add(cRevenue);
        cards.add(cMonth);
        cards.add(cTrainers);
        cards.add(cSessions);
        cards.setPreferredSize(new Dimension(100, 230));

        JPanel widgets = new JPanel(new GridLayout(1, 3, 14, 0));
        widgets.setOpaque(false);
        widgets.add(Ui.card("Revenue - Last 6 Months", chart));
        widgets.add(Ui.card("Recent Transactions", tableScroll(recentModel)));
        widgets.add(Ui.card("Memberships Expiring Soon", tableScroll(expiringModel)));

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(cards, BorderLayout.NORTH);
        body.add(widgets, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private JScrollPane tableScroll(DefaultTableModel m) {
        JTable t = Ui.table(m) ;
        t.setRowHeight(26);
        t.setEnabled(false);
        JScrollPane sp = new JScrollPane(t);
        sp.setColumnHeaderView(t.getTableHeader());
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Reloads all numbers from the database. */
    public void refresh() {
        Db.autoExpireMemberships();

        cMembers.value.setText(String.valueOf(count("SELECT COUNT(*) FROM Member")));
        cActive.value.setText(String.valueOf(count("SELECT COUNT(*) FROM Membership WHERE Status = 'Active'")));
        cExpiring.value.setText(String.valueOf(count("SELECT COUNT(*) FROM Membership WHERE Status = 'Active' "
                + "AND ExpirationDate BETWEEN CAST(GETDATE() AS DATE) AND DATEADD(DAY, 30, CAST(GETDATE() AS DATE))")));
        cDayPass.value.setText(String.valueOf(count("SELECT COUNT(*) FROM Day_Pass "
                + "WHERE YEAR(PurchaseDate) = YEAR(GETDATE()) AND MONTH(PurchaseDate) = MONTH(GETDATE())")));
        cRevenue.value.setText(Ui.money(Db.scalar("SELECT ISNULL(SUM(TotalAmount), 0) FROM Gym_Transaction")));
        cMonth.value.setText(Ui.money(Db.scalar("SELECT ISNULL(SUM(TotalAmount), 0) FROM Gym_Transaction "
                + "WHERE YEAR(TransactionDate) = YEAR(GETDATE()) AND MONTH(TransactionDate) = MONTH(GETDATE())")));
        cTrainers.value.setText(String.valueOf(count("SELECT COUNT(*) FROM Trainer")));
        cSessions.value.setText(String.valueOf(count("SELECT COUNT(*) FROM Training_Session "
                + "WHERE YEAR(SessionDate) = YEAR(GETDATE()) AND MONTH(SessionDate) = MONTH(GETDATE())")));

        loadChart();

        recentModel.setRowCount(0);
        for (Object[] r : Db.query("SELECT TOP 8 gt.TransactionDate, CONCAT(c.FName, ' ', c.LName), gt.TotalAmount "
                + "FROM Gym_Transaction gt JOIN Customer c ON c.CustomerID = gt.CustomerID "
                + "ORDER BY gt.TransactionDate DESC, gt.TransactionID DESC")) {
            recentModel.addRow(new Object[]{r[0], r[1], Ui.money(r[2])});
        }

        expiringModel.setRowCount(0);
        for (Object[] r : Db.query("SELECT TOP 8 CONCAT(c.FName, ' ', c.LName), mp.PlanName, "
                + "DATEDIFF(DAY, CAST(GETDATE() AS DATE), ms.ExpirationDate) "
                + "FROM Membership ms JOIN Customer c ON c.CustomerID = ms.MemberID "
                + "JOIN Membership_Plan mp ON mp.PlanID = ms.PlanID "
                + "WHERE ms.Status = 'Active' AND ms.ExpirationDate BETWEEN CAST(GETDATE() AS DATE) "
                + "AND DATEADD(DAY, 30, CAST(GETDATE() AS DATE)) ORDER BY ms.ExpirationDate")) {
            expiringModel.addRow(new Object[]{r[0], r[1], r[2] + " day(s)"});
        }
    }

    private int count(String sql) {
        Object o = Db.scalar(sql);
        return o == null ? 0 : ((Number) o).intValue();
    }

    private void loadChart() {
        String[] labels = new String[6];
        double[] values = new double[6];
        YearMonth[] months = new YearMonth[6];
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 6; i++) {
            months[i] = now.minusMonths(5 - i);
            labels[i] = months[i].getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        }
        List<Object[]> rows = Db.query("SELECT YEAR(TransactionDate), MONTH(TransactionDate), SUM(TotalAmount) "
                + "FROM Gym_Transaction WHERE TransactionDate >= "
                + "DATEADD(MONTH, -5, DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1)) "
                + "GROUP BY YEAR(TransactionDate), MONTH(TransactionDate)");
        for (Object[] r : rows) {
            YearMonth ym = YearMonth.of(((Number) r[0]).intValue(), ((Number) r[1]).intValue());
            for (int i = 0; i < 6; i++) {
                if (months[i].equals(ym)) values[i] = ((Number) r[2]).doubleValue();
            }
        }
        chart.setData(labels, values);
    }
}
