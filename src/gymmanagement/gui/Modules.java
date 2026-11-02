package gymmanagement.gui;

import gymmanagement.db.Db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Factory for every data-entry screen. Each method returns a CrudPanel and
 * contains the SQL for that module's load / insert / update / delete.
 */
public final class Modules {

    private Modules() { }

    // ----------------------------------------------------------- drop-down queries
    private static final String TRAINERS =
            "SELECT TrainerID, TrainerName FROM Trainer ORDER BY TrainerName";
    private static final String STAFF =
            "SELECT StaffID, CONCAT(StaffName, ' (', Position, ')') FROM Staff ORDER BY StaffName";
    private static final String MEMBERS =
            "SELECT m.MemberID, CONCAT(c.LName, ', ', c.FName) FROM Member m "
                    + "JOIN Customer c ON c.CustomerID = m.MemberID ORDER BY c.LName, c.FName";
    private static final String PLANS =
            "SELECT PlanID, CONCAT(PlanName, ' - P', CAST(Price AS VARCHAR(20)), ' / ', DurationMonths, ' mo') "
                    + "FROM Membership_Plan ORDER BY Price";
    private static final String SESSIONS =
            "SELECT s.SessionID, CONCAT('#', s.SessionID, '  ', CONVERT(VARCHAR(10), s.SessionDate, 23), '  ', t.TrainerName) "
                    + "FROM Training_Session s JOIN Trainer t ON t.TrainerID = s.TrainerID ORDER BY s.SessionDate DESC";
    private static final String TRANSACTIONS =
            "SELECT gt.TransactionID, CONCAT('T', gt.TransactionID, ' - ', c.LName, ', ', c.FName, '  (P', "
                    + "CAST(gt.TotalAmount AS VARCHAR(20)), ')') FROM Gym_Transaction gt "
                    + "JOIN Customer c ON c.CustomerID = gt.CustomerID ORDER BY gt.TransactionID DESC";

    // ----------------------------------------------------------- small helpers
    private static java.sql.Date date(String s) {
        return java.sql.Date.valueOf(LocalDate.parse(s));
    }

    private static java.sql.Date today() {
        return java.sql.Date.valueOf(LocalDate.now());
    }

    private static BigDecimal num(String s) {
        return new BigDecimal(s);
    }

    private static int id(String s) {
        return Integer.parseInt(s);
    }

    private static Integer idOrNull(String s) {
        return (s == null || s.isEmpty()) ? null : Integer.valueOf(s);
    }

    private static void requireRef(String type, String ref) {
        if ("Digital".equals(type) && (ref == null || ref.isEmpty())) {
            throw new IllegalArgumentException("Reference number is required for Digital payments.");
        }
    }

    /** Inserts a Payment row + its Cash / Digital_Payment subtype row. */
    private static void addPayment(Connection c, int txId, java.sql.Date when, BigDecimal amount,
                                   String type, String ref) throws SQLException {
        int pid = Db.insertId(c,
                "INSERT INTO Payment (TransactionID, PaymentDate, Amount, PaymentType) VALUES (?,?,?,?)",
                txId, when, amount, type);
        if ("Cash".equals(type)) {
            Db.run(c, "INSERT INTO Cash (CPaymentID) VALUES (?)", pid);
        } else {
            Db.run(c, "INSERT INTO Digital_Payment (DPaymentID, ReferenceNumber) VALUES (?,?)", pid, ref);
        }
    }

    // ================================================================= MEMBERS
    public static CrudPanel members() {
        return new CrudPanel("Members",
                "Register gym members with their trainer, contact details and medical history",
                new String[]{"MemberID:ID", "LName:Last Name", "FName:First Name", "RegistrationDate:Registered",
                        "TrainerName:Trainer", "ContactNumber:Contact", "Email:Email", "Birthdate:Birthdate",
                        "EmergencyContactNumber:Emergency No.", "MedicalCondition:Medical Condition", "#TrainerID"},
                Field.text("LName", "Last Name"),
                Field.text("FName", "First Name"),
                Field.date("RegistrationDate", "Registered"),
                Field.lookup("TrainerID", "Trainer", TRAINERS).optional(),
                Field.text("ContactNumber", "Contact No.").regex("\\d{11}", "must be an 11-digit number (e.g. 09171234567)."),
                Field.text("Email", "Email").regex(".+@.+\\..+", "must be a valid email address."),
                Field.date("Birthdate", "Birthdate"),
                Field.text("EmergencyContactNumber", "Emergency No.").regex("\\d{11}", "must be an 11-digit number."),
                Field.text("MedicalCondition", "Medical Condition").optional()) {

            @Override
            protected List<Object[]> load() {
                return Db.query("SELECT m.MemberID, c.LName, c.FName, c.RegistrationDate, ISNULL(t.TrainerName, '-'), "
                        + "m.ContactNumber, m.Email, m.Birthdate, m.EmergencyContactNumber, "
                        + "ISNULL(mh.MedicalCondition, 'None'), m.TrainerID "
                        + "FROM Member m JOIN Customer c ON c.CustomerID = m.MemberID "
                        + "LEFT JOIN Trainer t ON t.TrainerID = m.TrainerID "
                        + "LEFT JOIN Medical_History mh ON mh.MemberID = m.MemberID ORDER BY m.MemberID");
            }

            @Override
            protected void insert(Map<String, String> v) {
                final String med = v.get("MedicalCondition").isEmpty() ? "None" : v.get("MedicalCondition");
                Db.transaction(c -> {
                    int newId = Db.insertId(c,
                            "INSERT INTO Customer (LName, FName, RegistrationDate, CustomerType) VALUES (?,?,?, 'Member')",
                            v.get("LName"), v.get("FName"), date(v.get("RegistrationDate")));
                    Db.run(c, "INSERT INTO Member (MemberID, TrainerID, ContactNumber, Email, Birthdate, EmergencyContactNumber) "
                                    + "VALUES (?,?,?,?,?,?)",
                            newId, idOrNull(v.get("TrainerID")), v.get("ContactNumber"), v.get("Email"),
                            date(v.get("Birthdate")), v.get("EmergencyContactNumber"));
                    Db.run(c, "INSERT INTO Medical_History (MemberID, MedicalCondition) VALUES (?,?)", newId, med);
                });
            }

            @Override
            protected void update(Map<String, String> old, Map<String, String> v) {
                final int mid = id(old.get("MemberID"));
                final String med = v.get("MedicalCondition").isEmpty() ? "None" : v.get("MedicalCondition");
                Db.transaction(c -> {
                    Db.run(c, "UPDATE Customer SET LName=?, FName=?, RegistrationDate=? WHERE CustomerID=?",
                            v.get("LName"), v.get("FName"), date(v.get("RegistrationDate")), mid);
                    Db.run(c, "UPDATE Member SET TrainerID=?, ContactNumber=?, Email=?, Birthdate=?, "
                                    + "EmergencyContactNumber=? WHERE MemberID=?",
                            idOrNull(v.get("TrainerID")), v.get("ContactNumber"), v.get("Email"),
                            date(v.get("Birthdate")), v.get("EmergencyContactNumber"), mid);
                    int n = Db.run(c, "UPDATE Medical_History SET MedicalCondition=? WHERE MemberID=?", med, mid);
                    if (n == 0) {
                        Db.run(c, "INSERT INTO Medical_History (MemberID, MedicalCondition) VALUES (?,?)", mid, med);
                    }
                });
            }

            @Override
            protected void delete(Map<String, String> old) {
                final int mid = id(old.get("MemberID"));
                Db.transaction(c -> {
                    Db.run(c, "DELETE FROM Member WHERE MemberID=?", mid);      // Medical_History is removed by ON DELETE CASCADE
                    Db.run(c, "DELETE FROM Customer WHERE CustomerID=?", mid);
                });
            }
        };
    }

    // ================================================================= MEMBERSHIPS
    public static CrudPanel memberships() {
        return new CrudPanel("Memberships",
                "Sell a plan to a member. A transaction and the first payment are created automatically",
                new String[]{"MembershipID:ID", "MemberName:Member", "PlanName:Plan", "StartDate:Start",
                        "ExpirationDate:Expires", "Status:Status", "AmountPaid:Amount", "StaffName:Recorded By",
                        "#MemberID", "#PlanID", "#TransactionID", "#StaffID"},
                Field.lookup("MemberID", "Member", MEMBERS).createOnly(),
                Field.lookup("PlanID", "Plan", PLANS).createOnly(),
                Field.date("StartDate", "Start Date"),
                Field.combo("Status", "Status", "Active", "Expired", "Cancelled"),
                Field.lookup("StaffID", "Recorded By", STAFF).createOnly(),
                Field.combo("PaymentType", "Payment", "Cash", "Digital").createOnly(),
                Field.text("Reference", "Digital Ref. No.").optional().createOnly()) {

            @Override
            protected List<Object[]> load() {
                return Db.query("SELECT ms.MembershipID, CONCAT(c.LName, ', ', c.FName), mp.PlanName, ms.StartDate, "
                        + "ms.ExpirationDate, ms.Status, gt.TotalAmount, s.StaffName, "
                        + "ms.MemberID, ms.PlanID, ms.TransactionID, gt.StaffID "
                        + "FROM Membership ms JOIN Customer c ON c.CustomerID = ms.MemberID "
                        + "JOIN Membership_Plan mp ON mp.PlanID = ms.PlanID "
                        + "JOIN Gym_Transaction gt ON gt.TransactionID = ms.TransactionID "
                        + "JOIN Staff s ON s.StaffID = gt.StaffID ORDER BY ms.MembershipID DESC");
            }

            @Override
            protected void insert(Map<String, String> v) {
                final String type = v.get("PaymentType");
                final String ref = v.get("Reference");
                requireRef(type, ref);
                final int memberId = id(v.get("MemberID"));
                final int planId = id(v.get("PlanID"));
                final int staffId = id(v.get("StaffID"));
                List<Object[]> plan = Db.query("SELECT Price, DurationMonths FROM Membership_Plan WHERE PlanID=?", planId);
                if (plan.isEmpty()) throw new IllegalArgumentException("Selected plan was not found.");
                final BigDecimal price = new BigDecimal(plan.get(0)[0].toString());
                final int months = ((Number) plan.get(0)[1]).intValue();
                final java.sql.Date start = date(v.get("StartDate"));

                Db.transaction(c -> {
                    int tx = Db.insertId(c,
                            "INSERT INTO Gym_Transaction (StaffID, CustomerID, TransactionDate, TotalAmount) VALUES (?,?,?,?)",
                            staffId, memberId, today(), price);
                    Db.run(c, "INSERT INTO Membership (MemberID, PlanID, TransactionID, StartDate, ExpirationDate, Status) "
                                    + "VALUES (?,?,?,?, DATEADD(MONTH, ?, ?), ?)",
                            memberId, planId, tx, start, months, start, v.get("Status"));
                    addPayment(c, tx, today(), price, type, ref);
                });
            }

            @Override
            protected void update(Map<String, String> old, Map<String, String> v) {
                java.sql.Date start = date(v.get("StartDate"));
                Db.execute("UPDATE Membership SET StartDate=?, Status=?, "
                                + "ExpirationDate = DATEADD(MONTH, (SELECT DurationMonths FROM Membership_Plan "
                                + "WHERE PlanID = Membership.PlanID), ?) WHERE MembershipID=?",
                        start, v.get("Status"), start, id(old.get("MembershipID")));
            }

            @Override
            protected void delete(Map<String, String> old) {
                final int msId = id(old.get("MembershipID"));
                final int tx = id(old.get("TransactionID"));
                Db.transaction(c -> {
                    Db.run(c, "DELETE FROM Payment WHERE TransactionID=?", tx);  // Cash / Digital rows cascade
                    Db.run(c, "DELETE FROM Membership WHERE MembershipID=?", msId);
                    Db.run(c, "DELETE FROM Gym_Transaction WHERE TransactionID=?", tx);
                });
            }
        };
    }

    // ================================================================= DAY PASS
    public static CrudPanel dayPasses() {
        return new CrudPanel("Day Pass",
                "Walk-in (non-member) day passes. The customer, transaction and payment are created together",
                new String[]{"DayPassID:ID", "NonMemberName:Non-Member", "PurchaseDate:Date", "Price:Price",
                        "StaffName:Recorded By", "PayType:Payment",
                        "#NonMemberID", "#TransactionID", "#StaffID", "#LName", "#FName"},
                Field.text("LName", "Last Name"),
                Field.text("FName", "First Name"),
                Field.date("PurchaseDate", "Purchase Date"),
                Field.number("Price", "Price"),
                Field.lookup("StaffID", "Recorded By", STAFF).createOnly(),
                Field.combo("PaymentType", "Payment", "Cash", "Digital").createOnly(),
                Field.text("Reference", "Digital Ref. No.").optional().createOnly()) {

            @Override
            protected List<Object[]> load() {
                return Db.query("SELECT dp.DayPassID, CONCAT(c.LName, ', ', c.FName), dp.PurchaseDate, dp.Price, s.StaffName, "
                        + "(SELECT TOP 1 p.PaymentType FROM Payment p WHERE p.TransactionID = dp.TransactionID), "
                        + "dp.NonMemberID, dp.TransactionID, gt.StaffID, c.LName, c.FName "
                        + "FROM Day_Pass dp JOIN Customer c ON c.CustomerID = dp.NonMemberID "
                        + "JOIN Gym_Transaction gt ON gt.TransactionID = dp.TransactionID "
                        + "JOIN Staff s ON s.StaffID = gt.StaffID ORDER BY dp.DayPassID DESC");
            }

            @Override
            protected void insert(Map<String, String> v) {
                final String type = v.get("PaymentType");
                final String ref = v.get("Reference");
                requireRef(type, ref);
                final int staffId = id(v.get("StaffID"));
                final BigDecimal price = num(v.get("Price"));
                final java.sql.Date when = date(v.get("PurchaseDate"));

                Db.transaction(c -> {
                    int cust = Db.insertId(c,
                            "INSERT INTO Customer (LName, FName, RegistrationDate, CustomerType) VALUES (?,?,?, 'Non-Member')",
                            v.get("LName"), v.get("FName"), when);
                    Db.run(c, "INSERT INTO Non_Member (NonMemberID) VALUES (?)", cust);
                    int tx = Db.insertId(c,
                            "INSERT INTO Gym_Transaction (StaffID, CustomerID, TransactionDate, TotalAmount) VALUES (?,?,?,?)",
                            staffId, cust, when, price);
                    Db.run(c, "INSERT INTO Day_Pass (NonMemberID, TransactionID, PurchaseDate, Price) VALUES (?,?,?,?)",
                            cust, tx, when, price);
                    addPayment(c, tx, when, price, type, ref);
                });
            }

            @Override
            protected void update(Map<String, String> old, Map<String, String> v) {
                final int cust = id(old.get("NonMemberID"));
                final int tx = id(old.get("TransactionID"));
                final int dpId = id(old.get("DayPassID"));
                final BigDecimal price = num(v.get("Price"));
                final java.sql.Date when = date(v.get("PurchaseDate"));
                Db.transaction(c -> {
                    Db.run(c, "UPDATE Customer SET LName=?, FName=? WHERE CustomerID=?", v.get("LName"), v.get("FName"), cust);
                    Db.run(c, "UPDATE Day_Pass SET PurchaseDate=?, Price=? WHERE DayPassID=?", when, price, dpId);
                    Db.run(c, "UPDATE Gym_Transaction SET TransactionDate=?, TotalAmount=? WHERE TransactionID=?", when, price, tx);
                    // keep the single payment equal to the new price
                    Db.run(c, "UPDATE Payment SET Amount=? WHERE TransactionID=? "
                            + "AND (SELECT COUNT(*) FROM Payment WHERE TransactionID=?) = 1", price, tx, tx);
                });
            }

            @Override
            protected void delete(Map<String, String> old) {
                final int cust = id(old.get("NonMemberID"));
                final int tx = id(old.get("TransactionID"));
                final int dpId = id(old.get("DayPassID"));
                Db.transaction(c -> {
                    Db.run(c, "DELETE FROM Payment WHERE TransactionID=?", tx);
                    Db.run(c, "DELETE FROM Day_Pass WHERE DayPassID=?", dpId);
                    Db.run(c, "DELETE FROM Gym_Transaction WHERE TransactionID=?", tx);
                    Db.run(c, "DELETE FROM Non_Member WHERE NonMemberID=? "
                            + "AND NOT EXISTS (SELECT 1 FROM Day_Pass WHERE NonMemberID=?)", cust, cust);
                    Db.run(c, "DELETE FROM Customer WHERE CustomerID=? "
                            + "AND NOT EXISTS (SELECT 1 FROM Non_Member WHERE NonMemberID=?)", cust, cust);
                });
            }
        };
    }

    // ================================================================= PAYMENTS
    private static void checkBalance(int txId, BigDecimal amount, BigDecimal ignore) {
        List<Object[]> r = Db.query("SELECT gt.TotalAmount, "
                + "ISNULL((SELECT SUM(Amount) FROM Payment WHERE TransactionID = gt.TransactionID), 0) "
                + "FROM Gym_Transaction gt WHERE gt.TransactionID=?", txId);
        if (r.isEmpty()) throw new IllegalArgumentException("Transaction not found.");
        BigDecimal total = new BigDecimal(r.get(0)[0].toString());
        BigDecimal paid = new BigDecimal(r.get(0)[1].toString());
        BigDecimal remaining = total.subtract(paid).add(ignore);
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Payment is more than the remaining balance (" + remaining.toPlainString() + ").");
        }
    }

    public static CrudPanel payments() {
        return new CrudPanel("Transactions & Payments",
                "Every transaction can be paid in one or more payments (cash and/or digital)",
                new String[]{"PaymentID:Payment ID", "TransactionLabel:Transaction", "TotalAmount:Txn Total",
                        "PaymentDate:Paid On", "Amount:Amount", "PaymentType:Method", "ReferenceNumber:Reference",
                        "Balance:Txn Balance", "#TransactionID"},
                Field.lookup("TransactionID", "Transaction", TRANSACTIONS).createOnly(),
                Field.date("PaymentDate", "Payment Date"),
                Field.number("Amount", "Amount"),
                Field.combo("PaymentType", "Method", "Cash", "Digital"),
                Field.text("ReferenceNumber", "Digital Ref. No.").optional()) {

            @Override
            protected List<Object[]> load() {
                return Db.query("SELECT p.PaymentID, CONCAT('T', gt.TransactionID, ' - ', c.LName, ', ', c.FName), "
                        + "gt.TotalAmount, p.PaymentDate, p.Amount, p.PaymentType, ISNULL(dp.ReferenceNumber, ''), "
                        + "gt.TotalAmount - (SELECT SUM(x.Amount) FROM Payment x WHERE x.TransactionID = gt.TransactionID), "
                        + "p.TransactionID "
                        + "FROM Payment p JOIN Gym_Transaction gt ON gt.TransactionID = p.TransactionID "
                        + "JOIN Customer c ON c.CustomerID = gt.CustomerID "
                        + "LEFT JOIN Digital_Payment dp ON dp.DPaymentID = p.PaymentID ORDER BY p.PaymentID DESC");
            }

            @Override
            protected void insert(Map<String, String> v) {
                final String type = v.get("PaymentType");
                final String ref = v.get("ReferenceNumber");
                requireRef(type, ref);
                final int tx = id(v.get("TransactionID"));
                final BigDecimal amt = num(v.get("Amount"));
                checkBalance(tx, amt, BigDecimal.ZERO);
                Db.transaction(c -> addPayment(c, tx, date(v.get("PaymentDate")), amt, type, ref));
            }

            @Override
            protected void update(Map<String, String> old, Map<String, String> v) {
                final String type = v.get("PaymentType");
                final String ref = v.get("ReferenceNumber");
                requireRef(type, ref);
                final int pid = id(old.get("PaymentID"));
                final BigDecimal amt = num(v.get("Amount"));
                checkBalance(id(old.get("TransactionID")), amt, num(old.get("Amount")));
                Db.transaction(c -> {
                    Db.run(c, "UPDATE Payment SET PaymentDate=?, Amount=?, PaymentType=? WHERE PaymentID=?",
                            date(v.get("PaymentDate")), amt, type, pid);
                    Db.run(c, "DELETE FROM Cash WHERE CPaymentID=?", pid);
                    Db.run(c, "DELETE FROM Digital_Payment WHERE DPaymentID=?", pid);
                    if ("Cash".equals(type)) {
                        Db.run(c, "INSERT INTO Cash (CPaymentID) VALUES (?)", pid);
                    } else {
                        Db.run(c, "INSERT INTO Digital_Payment (DPaymentID, ReferenceNumber) VALUES (?,?)", pid, ref);
                    }
                });
            }

            @Override
            protected void delete(Map<String, String> old) {
                final int tx = id(old.get("TransactionID"));
                Object count = Db.scalar("SELECT COUNT(*) FROM Payment WHERE TransactionID=?", tx);
                if (count != null && ((Number) count).intValue() <= 1) {
                    throw new IllegalArgumentException("A transaction must keep at least one payment.\n"
                            + "Add another payment first, or delete the whole membership / day pass instead.");
                }
                Db.execute("DELETE FROM Payment WHERE PaymentID=?", id(old.get("PaymentID")));
            }
        };
    }

    // ================================================================= TRAINERS
    public static CrudPanel trainers() {
        return new CrudPanel("Trainers", "Gym trainers and their contact details",
                new String[]{"TrainerID:ID", "TrainerName:Name", "ContactNumber:Contact", "FBAccount:Facebook"},
                Field.text("TrainerName", "Name"),
                Field.text("ContactNumber", "Contact No.").regex("\\d{11}", "must be an 11-digit number."),
                Field.text("FBAccount", "Facebook")) {

            @Override
            protected List<Object[]> load() {
                return Db.query("SELECT TrainerID, TrainerName, ContactNumber, FBAccount FROM Trainer ORDER BY TrainerID");
            }

            @Override
            protected void insert(Map<String, String> v) {
                Db.execute("INSERT INTO Trainer (TrainerName, ContactNumber, FBAccount) VALUES (?,?,?)",
                        v.get("TrainerName"), v.get("ContactNumber"), v.get("FBAccount"));
            }

            @Override
            protected void update(Map<String, String> old, Map<String, String> v) {
                Db.execute("UPDATE Trainer SET TrainerName=?, ContactNumber=?, FBAccount=? WHERE TrainerID=?",
                        v.get("TrainerName"), v.get("ContactNumber"), v.get("FBAccount"), id(old.get("TrainerID")));
            }

            @Override
            protected void delete(Map<String, String> old) {
                Db.execute("DELETE FROM Trainer WHERE TrainerID=?", id(old.get("TrainerID")));
            }
        };
    }

    // ================================================================= SESSIONS
    public static CrudPanel sessions() {
        return new CrudPanel("Training Sessions", "Sessions conducted by trainers (fees are paid outside the gym transaction)",
                new String[]{"SessionID:ID", "TrainerName:Trainer", "SessionDate:Date", "Fee:Fee",
                        "Attendees:Attendees", "#TrainerID"},
                Field.lookup("TrainerID", "Trainer", TRAINERS),
                Field.date("SessionDate", "Session Date"),
                Field.number("Fee", "Fee")) {

            @Override
            protected List<Object[]> load() {
                return Db.query("SELECT s.SessionID, t.TrainerName, s.SessionDate, s.Fee, "
                        + "(SELECT COUNT(*) FROM Session_Attendance a WHERE a.SessionID = s.SessionID), s.TrainerID "
                        + "FROM Training_Session s JOIN Trainer t ON t.TrainerID = s.TrainerID ORDER BY s.SessionDate DESC, s.SessionID DESC");
            }

            @Override
            protected void insert(Map<String, String> v) {
                Db.execute("INSERT INTO Training_Session (TrainerID, SessionDate, Fee) VALUES (?,?,?)",
                        id(v.get("TrainerID")), date(v.get("SessionDate")), num(v.get("Fee")));
            }

            @Override
            protected void update(Map<String, String> old, Map<String, String> v) {
                Db.execute("UPDATE Training_Session SET TrainerID=?, SessionDate=?, Fee=? WHERE SessionID=?",
                        id(v.get("TrainerID")), date(v.get("SessionDate")), num(v.get("Fee")), id(old.get("SessionID")));
            }

            @Override
            protected void delete(Map<String, String> old) {
                final int sid = id(old.get("SessionID"));
                Db.transaction(c -> {
                    Db.run(c, "DELETE FROM Session_Attendance WHERE SessionID=?", sid);
                    Db.run(c, "DELETE FROM Training_Session WHERE SessionID=?", sid);
                });
            }
        };
    }

    // ================================================================= ATTENDANCE
    public static CrudPanel attendance() {
        return new CrudPanel("Session Attendance", "Which members attended which training session",
                new String[]{"SessionID:Session", "SessionDate:Date", "TrainerName:Trainer", "MemberName:Member", "#MemberID"},
                Field.lookup("SessionID", "Session", SESSIONS),
                Field.lookup("MemberID", "Member", MEMBERS)) {

            @Override
            protected boolean canUpdate() {
                return false;
            }

            @Override
            protected List<Object[]> load() {
                return Db.query("SELECT a.SessionID, s.SessionDate, t.TrainerName, CONCAT(c.LName, ', ', c.FName), a.MemberID "
                        + "FROM Session_Attendance a JOIN Training_Session s ON s.SessionID = a.SessionID "
                        + "JOIN Trainer t ON t.TrainerID = s.TrainerID JOIN Customer c ON c.CustomerID = a.MemberID "
                        + "ORDER BY s.SessionDate DESC, a.SessionID DESC");
            }

            @Override
            protected void insert(Map<String, String> v) {
                Db.execute("INSERT INTO Session_Attendance (MemberID, SessionID) VALUES (?,?)",
                        id(v.get("MemberID")), id(v.get("SessionID")));
            }

            @Override
            protected void update(Map<String, String> old, Map<String, String> v) {
                // not used (Update button is hidden)
            }

            @Override
            protected void delete(Map<String, String> old) {
                Db.execute("DELETE FROM Session_Attendance WHERE MemberID=? AND SessionID=?",
                        id(old.get("MemberID")), id(old.get("SessionID")));
            }
        };
    }

    // ================================================================= STAFF
    public static CrudPanel staff() {
        return new CrudPanel("Staff", "Employees who record transactions",
                new String[]{"StaffID:ID", "StaffName:Name", "Position:Position", "Email:Email"},
                Field.text("StaffName", "Name"),
                Field.text("Position", "Position"),
                Field.text("Email", "Email").regex(".+@.+\\..+", "must be a valid email address.")) {

            @Override
            protected List<Object[]> load() {
                return Db.query("SELECT StaffID, StaffName, Position, Email FROM Staff ORDER BY StaffID");
            }

            @Override
            protected void insert(Map<String, String> v) {
                Db.execute("INSERT INTO Staff (StaffName, Position, Email) VALUES (?,?,?)",
                        v.get("StaffName"), v.get("Position"), v.get("Email"));
            }

            @Override
            protected void update(Map<String, String> old, Map<String, String> v) {
                Db.execute("UPDATE Staff SET StaffName=?, Position=?, Email=? WHERE StaffID=?",
                        v.get("StaffName"), v.get("Position"), v.get("Email"), id(old.get("StaffID")));
            }

            @Override
            protected void delete(Map<String, String> old) {
                Db.execute("DELETE FROM Staff WHERE StaffID=?", id(old.get("StaffID")));
            }
        };
    }
}
