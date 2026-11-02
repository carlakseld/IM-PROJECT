package gymmanagement.gui;

import gymmanagement.db.Db;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pick a report from the drop-down and the SQL result is shown in a table. */
public class ReportsPanel extends JPanel {

    private static final class Report {
        final String description;
        final String sql;

        Report(String description, String sql) {
            this.description = description;
            this.sql = sql;
        }
    }

    private final Map<String, Report> reports = new LinkedHashMap<String, Report>();
    private final JComboBox<String> chooser = new JComboBox<String>();
    private final JLabel description = new JLabel(" ");
    private final JLabel rowCount = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel();
    private final JTable table = Ui.table(model);

    public ReportsPanel() {
        defineReports();

        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setBackground(Ui.BG);

        // header
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Reports");
        title.setFont(Ui.font(Font.BOLD, 24));
        title.setForeground(Ui.TEXT);
        JLabel sub = new JLabel("Business reports generated with SQL JOIN, GROUP BY, aggregate and date functions");
        sub.setFont(Ui.font(Font.PLAIN, 13));
        sub.setForeground(Ui.MUTED);
        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(sub);

        // chooser row
        JPanel chooserRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        chooserRow.setOpaque(false);
        for (String name : reports.keySet()) chooser.addItem(name);
        chooser.setPreferredSize(new Dimension(340, 32));
        chooser.setFont(Ui.font(Font.PLAIN, 13));
        JButton run = Ui.button("Run Report", Ui.ACCENT);
        run.addActionListener(e -> runSelected());
        chooser.addActionListener(e -> runSelected());
        chooserRow.add(new JLabel("Report:"));
        chooserRow.add(chooser);
        chooserRow.add(run);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(chooserRow, BorderLayout.CENTER);
        description.setFont(Ui.font(Font.ITALIC, 13));
        description.setForeground(Ui.MUTED);
        description.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        north.add(description, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        rowCount.setFont(Ui.font(Font.PLAIN, 12));
        rowCount.setForeground(Ui.MUTED);
        add(rowCount, BorderLayout.SOUTH);
    }

    public void refresh() {
        runSelected();
    }

    private void runSelected() {
        String name = (String) chooser.getSelectedItem();
        if (name == null) return;
        Report r = reports.get(name);
        description.setText(r.description);
        try {
            Db.Table t = Db.table(r.sql);
            Object[][] data = t.rows.toArray(new Object[0][]);
            model.setDataVector(data, t.columns);
            table.createDefaultColumnsFromModel();
            rowCount.setText(t.rows.size() == 0
                    ? "No rows returned (nothing to report)."
                    : t.rows.size() + " row(s)");
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void defineReports() {
        reports.put("R1. Membership Revenue by Plan", new Report(
                "Which plans earn the most? (LEFT JOIN, GROUP BY, SUM, COUNT)",
                "SELECT mp.PlanName, mp.PlanType, COUNT(ms.MembershipID) AS TotalMemberships, "
                        + "ISNULL(SUM(gt.TotalAmount), 0) AS TotalRevenue "
                        + "FROM Membership_Plan mp LEFT JOIN Membership ms ON ms.PlanID = mp.PlanID "
                        + "LEFT JOIN Gym_Transaction gt ON gt.TransactionID = ms.TransactionID "
                        + "GROUP BY mp.PlanName, mp.PlanType ORDER BY TotalRevenue DESC"));

        reports.put("R2. Memberships Expiring in 30 Days", new Report(
                "Who should the front desk remind to renew? (date functions)",
                "SELECT CONCAT(c.FName, ' ', c.LName) AS MemberName, m.ContactNumber, mp.PlanName, ms.ExpirationDate, "
                        + "DATEDIFF(DAY, GETDATE(), ms.ExpirationDate) AS DaysLeft "
                        + "FROM Membership ms JOIN Member m ON m.MemberID = ms.MemberID "
                        + "JOIN Customer c ON c.CustomerID = m.MemberID JOIN Membership_Plan mp ON mp.PlanID = ms.PlanID "
                        + "WHERE ms.Status = 'Active' AND ms.ExpirationDate BETWEEN CAST(GETDATE() AS DATE) "
                        + "AND DATEADD(DAY, 30, CAST(GETDATE() AS DATE)) ORDER BY ms.ExpirationDate"));

        reports.put("R3. Trainer Workload & Session Fees", new Report(
                "How busy is each trainer? Session fees are paid outside the gym transaction.",
                "SELECT t.TrainerName, COUNT(DISTINCT ts.SessionID) AS SessionsConducted, COUNT(sa.MemberID) AS TotalAttendees, "
                        + "SUM(CASE WHEN sa.MemberID IS NOT NULL THEN ts.Fee ELSE 0 END) AS TotalSessionFees "
                        + "FROM Trainer t LEFT JOIN Training_Session ts ON ts.TrainerID = t.TrainerID "
                        + "LEFT JOIN Session_Attendance sa ON sa.SessionID = ts.SessionID "
                        + "GROUP BY t.TrainerName ORDER BY TotalSessionFees DESC"));

        reports.put("R4. Staff Sales Performance", new Report(
                "Which staff member records the most sales? (COUNT, SUM, AVG, MAX)",
                "SELECT s.StaffName, s.Position, COUNT(gt.TransactionID) AS TransactionsRecorded, "
                        + "ISNULL(SUM(gt.TotalAmount), 0) AS TotalSales, "
                        + "ISNULL(CAST(AVG(gt.TotalAmount) AS DECIMAL(10,2)), 0) AS AverageSale, "
                        + "ISNULL(MAX(gt.TotalAmount), 0) AS LargestSale "
                        + "FROM Staff s LEFT JOIN Gym_Transaction gt ON gt.StaffID = s.StaffID "
                        + "GROUP BY s.StaffName, s.Position ORDER BY TotalSales DESC"));

        reports.put("R5. Collections by Payment Method", new Report(
                "How do customers prefer to pay? (percentage computed column)",
                "SELECT p.PaymentType, COUNT(*) AS NumberOfPayments, SUM(p.Amount) AS TotalCollected, "
                        + "CAST(100.0 * SUM(p.Amount) / (SELECT SUM(Amount) FROM Payment) AS DECIMAL(5,2)) AS PercentShare "
                        + "FROM Payment p GROUP BY p.PaymentType ORDER BY TotalCollected DESC"));

        reports.put("R6. Day Pass Sales per Non-Member", new Report(
                "Which walk-in customers spend the most?",
                "SELECT CONCAT(c.FName, ' ', c.LName) AS NonMemberName, COUNT(dp.DayPassID) AS DayPassesBought, "
                        + "ISNULL(SUM(dp.Price), 0) AS TotalSpent, MAX(dp.PurchaseDate) AS LastPurchase "
                        + "FROM Non_Member nm JOIN Customer c ON c.CustomerID = nm.NonMemberID "
                        + "LEFT JOIN Day_Pass dp ON dp.NonMemberID = nm.NonMemberID "
                        + "GROUP BY c.FName, c.LName ORDER BY TotalSpent DESC, NonMemberName"));

        reports.put("R7. Monthly Revenue", new Report(
                "How does revenue move month to month? (YEAR, MONTH, DATENAME)",
                "SELECT YEAR(TransactionDate) AS SalesYear, MONTH(TransactionDate) AS MonthNo, "
                        + "DATENAME(MONTH, TransactionDate) AS SalesMonth, COUNT(*) AS Transactions, "
                        + "SUM(TotalAmount) AS MonthlyRevenue FROM Gym_Transaction "
                        + "GROUP BY YEAR(TransactionDate), MONTH(TransactionDate), DATENAME(MONTH, TransactionDate) "
                        + "ORDER BY SalesYear, MonthNo"));

        reports.put("R8. Unpaid Balances (Payment Reconciliation)", new Report(
                "Transactions where the payments do not match the total. Empty = everything is fully paid.",
                "SELECT gt.TransactionID, gt.TransactionDate, gt.TotalAmount, ISNULL(SUM(p.Amount), 0) AS TotalPaid, "
                        + "gt.TotalAmount - ISNULL(SUM(p.Amount), 0) AS Balance "
                        + "FROM Gym_Transaction gt LEFT JOIN Payment p ON p.TransactionID = gt.TransactionID "
                        + "GROUP BY gt.TransactionID, gt.TransactionDate, gt.TotalAmount "
                        + "HAVING gt.TotalAmount <> ISNULL(SUM(p.Amount), 0)"));

        reports.put("R9. Split Payments", new Report(
                "Transactions that were paid with more than one payment.",
                "SELECT gt.TransactionID, CONCAT(c.FName, ' ', c.LName) AS CustomerName, gt.TotalAmount, "
                        + "COUNT(p.PaymentID) AS NumberOfPayments "
                        + "FROM Gym_Transaction gt JOIN Customer c ON c.CustomerID = gt.CustomerID "
                        + "JOIN Payment p ON p.TransactionID = gt.TransactionID "
                        + "GROUP BY gt.TransactionID, c.FName, c.LName, gt.TotalAmount HAVING COUNT(p.PaymentID) > 1"));
    }
}
