/* =====================================================================
   GYM MANAGEMENT SYSTEM  -  Information Management Final Project
   GROUP 2 | BSIT 2HG1
   Platform : Microsoft SQL Server (run in SSMS)

   HOW TO RUN: open this file in SSMS and press F5 (Execute) ONCE.
   It is safe to run again - it rebuilds the tables with fresh sample data.

   NOTE: This script does NOT create any login or password.
         The login used by the Java GUI is created separately
         (see the step-by-step guide).

   CONTENTS
     PART 1  Create database
     PART 2  DDL  - CREATE TABLE (keys + constraints)
     PART 3  DDL  - ALTER TABLE
     PART 4  DML  - INSERT sample data (5+ rows per table)
     PART 5  CRUD demonstration (Create / Read / Update / Delete)
     PART 6  SELECT with conditions, JOINs and functions
     PART 7  SQL reports (views with computed columns)
   ===================================================================== */


/* =====================================================================
   PART 1 - CREATE DATABASE
   ===================================================================== */
CREATE DATABASE GymManagementDB;
USE GymManagementDB;


/* =====================================================================
   PART 2 - DDL: CREATE TABLE
   Old objects are dropped first (children before parents) so the
   script can be re-run without errors.
   ===================================================================== */
DROP VIEW  IF EXISTS vw_MembershipRevenueByPlan;
DROP VIEW  IF EXISTS vw_ExpiringMemberships;
DROP VIEW  IF EXISTS vw_TrainerWorkload;
DROP VIEW  IF EXISTS vw_StaffSales;
DROP VIEW  IF EXISTS vw_PaymentMethodSummary;
DROP VIEW  IF EXISTS vw_MonthlyRevenue;
DROP VIEW  IF EXISTS vw_TransactionBalance;
GO

DROP TABLE IF EXISTS Cash;
DROP TABLE IF EXISTS Digital_Payment;
DROP TABLE IF EXISTS Payment;
DROP TABLE IF EXISTS Session_Attendance;
DROP TABLE IF EXISTS Training_Session;
DROP TABLE IF EXISTS Day_Pass;
DROP TABLE IF EXISTS Membership;
DROP TABLE IF EXISTS Gym_Transaction;
DROP TABLE IF EXISTS Medical_History;
DROP TABLE IF EXISTS Member;
DROP TABLE IF EXISTS Non_Member;
DROP TABLE IF EXISTS Customer;
DROP TABLE IF EXISTS Membership_Plan;
DROP TABLE IF EXISTS Trainer;
DROP TABLE IF EXISTS Staff;
GO

/* ---------- Staff ---------- */
CREATE TABLE Staff (
    StaffID    INT IDENTITY(1,1) CONSTRAINT PK_Staff PRIMARY KEY,
    StaffName  VARCHAR(60)  NOT NULL,
    Position   VARCHAR(40)  NOT NULL,
    Email      VARCHAR(80)  NOT NULL CONSTRAINT UQ_Staff_Email UNIQUE,
    CONSTRAINT CK_Staff_Email CHECK (Email LIKE '%_@_%._%')
);

/* ---------- Trainer ---------- */
CREATE TABLE Trainer (
    TrainerID     INT IDENTITY(1,1) CONSTRAINT PK_Trainer PRIMARY KEY,
    TrainerName   VARCHAR(60) NOT NULL,
    ContactNumber VARCHAR(11) NOT NULL CONSTRAINT UQ_Trainer_Contact UNIQUE,
    FBAccount     VARCHAR(60) NOT NULL,
    CONSTRAINT CK_Trainer_Contact CHECK (ContactNumber NOT LIKE '%[^0-9]%' AND LEN(ContactNumber) = 11)
);

/* ---------- Membership_Plan ---------- */
CREATE TABLE Membership_Plan (
    PlanID         INT IDENTITY(1,1) CONSTRAINT PK_Membership_Plan PRIMARY KEY,
    PlanName       VARCHAR(40)   NOT NULL CONSTRAINT UQ_Plan_Name UNIQUE,
    PlanType       VARCHAR(10)   NOT NULL,
    Price          DECIMAL(10,2) NOT NULL,
    DurationMonths INT           NOT NULL,
    CONSTRAINT CK_Plan_Type     CHECK (PlanType IN ('Monthly','Bi-Annual','Annual')),
    CONSTRAINT CK_Plan_Price    CHECK (Price > 0),
    CONSTRAINT CK_Plan_Duration CHECK (DurationMonths > 0)
);

/* ---------- Customer (supertype of Member / Non_Member) ---------- */
CREATE TABLE Customer (
    CustomerID       INT IDENTITY(1,1) CONSTRAINT PK_Customer PRIMARY KEY,
    LName            VARCHAR(40) NOT NULL,
    FName            VARCHAR(40) NOT NULL,
    RegistrationDate DATE        NOT NULL,
    CustomerType     VARCHAR(10) NOT NULL,
    CONSTRAINT CK_Customer_Type CHECK (CustomerType IN ('Member','Non-Member'))
);

/* ---------- Member (subtype) ---------- */
CREATE TABLE Member (
    MemberID               INT         NOT NULL CONSTRAINT PK_Member PRIMARY KEY,
    TrainerID              INT         NULL,
    ContactNumber          VARCHAR(11) NOT NULL CONSTRAINT UQ_Member_Contact UNIQUE,
    Email                  VARCHAR(80) NOT NULL CONSTRAINT UQ_Member_Email UNIQUE,
    Birthdate              DATE        NOT NULL,
    EmergencyContactNumber VARCHAR(11) NOT NULL,
    CONSTRAINT FK_Member_Customer FOREIGN KEY (MemberID)  REFERENCES Customer(CustomerID),
    CONSTRAINT FK_Member_Trainer  FOREIGN KEY (TrainerID) REFERENCES Trainer(TrainerID),
    CONSTRAINT CK_Member_Contact   CHECK (ContactNumber NOT LIKE '%[^0-9]%' AND LEN(ContactNumber) = 11),
    CONSTRAINT CK_Member_Emergency CHECK (EmergencyContactNumber NOT LIKE '%[^0-9]%' AND LEN(EmergencyContactNumber) = 11),
    CONSTRAINT CK_Member_Email     CHECK (Email LIKE '%_@_%._%')
);

/* ---------- Non_Member (subtype) ---------- */
CREATE TABLE Non_Member (
    NonMemberID INT NOT NULL CONSTRAINT PK_Non_Member PRIMARY KEY,
    CONSTRAINT FK_NonMember_Customer FOREIGN KEY (NonMemberID) REFERENCES Customer(CustomerID)
);

/* ---------- Medical_History (weak entity, identified by MemberID) ---------- */
CREATE TABLE Medical_History (
    MemberID         INT          NOT NULL CONSTRAINT PK_Medical_History PRIMARY KEY,
    MedicalCondition VARCHAR(150) NOT NULL CONSTRAINT DF_Medical_Condition DEFAULT 'None',
    CONSTRAINT FK_Medical_Member FOREIGN KEY (MemberID) REFERENCES Member(MemberID) ON DELETE CASCADE
);

/* ---------- Gym_Transaction ---------- */
CREATE TABLE Gym_Transaction (
    TransactionID   INT IDENTITY(1,1) CONSTRAINT PK_Gym_Transaction PRIMARY KEY,
    StaffID         INT           NOT NULL,
    CustomerID      INT           NOT NULL,
    TransactionDate DATE          NOT NULL,
    TotalAmount     DECIMAL(10,2) NOT NULL,
    CONSTRAINT FK_Transaction_Staff    FOREIGN KEY (StaffID)    REFERENCES Staff(StaffID),
    CONSTRAINT FK_Transaction_Customer FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID),
    CONSTRAINT CK_Transaction_Total    CHECK (TotalAmount > 0)
);

/* ---------- Membership ---------- */
CREATE TABLE Membership (
    MembershipID   INT IDENTITY(1,1) CONSTRAINT PK_Membership PRIMARY KEY,
    MemberID       INT         NOT NULL,
    PlanID         INT         NOT NULL,
    TransactionID  INT         NOT NULL,
    StartDate      DATE        NOT NULL,
    ExpirationDate DATE        NOT NULL,
    Status         VARCHAR(10) NOT NULL CONSTRAINT DF_Membership_Status DEFAULT 'Active',
    CONSTRAINT FK_Membership_Member      FOREIGN KEY (MemberID)      REFERENCES Member(MemberID),
    CONSTRAINT FK_Membership_Plan        FOREIGN KEY (PlanID)        REFERENCES Membership_Plan(PlanID),
    CONSTRAINT FK_Membership_Transaction FOREIGN KEY (TransactionID) REFERENCES Gym_Transaction(TransactionID),
    CONSTRAINT CK_Membership_Status CHECK (Status IN ('Active','Expired','Cancelled')),
    CONSTRAINT CK_Membership_Dates  CHECK (ExpirationDate > StartDate)
);

/* ---------- Day_Pass ---------- */
CREATE TABLE Day_Pass (
    DayPassID     INT IDENTITY(1,1) CONSTRAINT PK_Day_Pass PRIMARY KEY,
    NonMemberID   INT           NOT NULL,
    TransactionID INT           NOT NULL,
    PurchaseDate  DATE          NOT NULL,
    Price         DECIMAL(10,2) NOT NULL,
    CONSTRAINT FK_DayPass_NonMember   FOREIGN KEY (NonMemberID)   REFERENCES Non_Member(NonMemberID),
    CONSTRAINT FK_DayPass_Transaction FOREIGN KEY (TransactionID) REFERENCES Gym_Transaction(TransactionID),
    CONSTRAINT CK_DayPass_Price CHECK (Price > 0)
);

/* ---------- Training_Session ---------- */
CREATE TABLE Training_Session (
    SessionID   INT IDENTITY(1,1) CONSTRAINT PK_Training_Session PRIMARY KEY,
    TrainerID   INT           NOT NULL,
    SessionDate DATE          NOT NULL,
    Fee         DECIMAL(10,2) NOT NULL,
    CONSTRAINT FK_Session_Trainer FOREIGN KEY (TrainerID) REFERENCES Trainer(TrainerID),
    CONSTRAINT CK_Session_Fee CHECK (Fee >= 0)
);

/* ---------- Session_Attendance (associative: Member <-> Training_Session) ---------- */
CREATE TABLE Session_Attendance (
    MemberID  INT NOT NULL,
    SessionID INT NOT NULL,
    CONSTRAINT PK_Session_Attendance PRIMARY KEY (MemberID, SessionID),
    CONSTRAINT FK_Attendance_Member  FOREIGN KEY (MemberID)  REFERENCES Member(MemberID),
    CONSTRAINT FK_Attendance_Session FOREIGN KEY (SessionID) REFERENCES Training_Session(SessionID)
);

/* ---------- Payment (supertype of Cash / Digital_Payment) ---------- */
CREATE TABLE Payment (
    PaymentID     INT IDENTITY(1,1) CONSTRAINT PK_Payment PRIMARY KEY,
    TransactionID INT           NOT NULL,
    PaymentDate   DATE          NOT NULL,
    Amount        DECIMAL(10,2) NOT NULL,
    PaymentType   VARCHAR(10)   NOT NULL,
    CONSTRAINT FK_Payment_Transaction FOREIGN KEY (TransactionID) REFERENCES Gym_Transaction(TransactionID),
    CONSTRAINT CK_Payment_Amount CHECK (Amount > 0),
    CONSTRAINT CK_Payment_Type   CHECK (PaymentType IN ('Cash','Digital'))
);

/* ---------- Cash (subtype) ---------- */
CREATE TABLE Cash (
    CPaymentID INT NOT NULL CONSTRAINT PK_Cash PRIMARY KEY,
    CONSTRAINT FK_Cash_Payment FOREIGN KEY (CPaymentID) REFERENCES Payment(PaymentID) ON DELETE CASCADE
);

/* ---------- Digital_Payment (subtype) ---------- */
CREATE TABLE Digital_Payment (
    DPaymentID      INT         NOT NULL CONSTRAINT PK_Digital_Payment PRIMARY KEY,
    ReferenceNumber VARCHAR(30) NOT NULL CONSTRAINT UQ_Digital_Reference UNIQUE,
    CONSTRAINT FK_Digital_Payment FOREIGN KEY (DPaymentID) REFERENCES Payment(PaymentID) ON DELETE CASCADE
);
go


-- PART 3 - DDL: ALTER TABLE
-- 3a. Add a new column (existing rows get today's date automatically)
ALTER TABLE Staff
    ADD HireDate DATE NOT NULL CONSTRAINT DF_Staff_HireDate DEFAULT (CAST(GETDATE() AS DATE));
GO

-- 3b. Add a new constraint (each trainer must have a different Facebook account)
ALTER TABLE Trainer
    ADD CONSTRAINT UQ_Trainer_FB UNIQUE (FBAccount);
GO

-- 3c. Add a new CHECK constraint (a member must have a realistic birthdate)
ALTER TABLE Member
    ADD CONSTRAINT CK_Member_Birthdate CHECK (Birthdate >= '1900-01-01');
GO

-- 3d. Modify a column (allow longer position titles)
ALTER TABLE Staff
    ALTER COLUMN Position VARCHAR(60) NOT NULL;
GO


/* =====================================================================
   PART 4 - DML: INSERT SAMPLE DATA
   Dates are relative to today (GETDATE) so the GUI dashboard, the
   "expiring in 30 days" list and the monthly chart always have data.
   Tables were just re-created, so identity values start at 1 and the
   IDs used below are predictable.
   ===================================================================== */
DECLARE @today DATE = CAST(GETDATE() AS DATE);

/* ---- Staff (6) ---- */
INSERT INTO Staff (StaffName, Position, Email) VALUES
('Maria Santos',     'Front Desk Receptionist', 'maria.santos@gymflex.ph'),
('Jose Ramirez',     'Gym Manager',             'jose.ramirez@gymflex.ph'),
('Angela Cruz',      'Cashier',                 'angela.cruz@gymflex.ph'),
('Mark Villanueva',  'Front Desk Receptionist', 'mark.villanueva@gymflex.ph'),
('Patricia Lim',     'Membership Officer',      'patricia.lim@gymflex.ph'),
('Daniel Reyes',     'Cashier',                 'daniel.reyes@gymflex.ph');

/* ---- Trainer (5) ---- */
INSERT INTO Trainer (TrainerName, ContactNumber, FBAccount) VALUES
('Marco Dela Cruz',  '09171110001', 'facebook.com/marco.delacruz.fit'),
('Bianca Reyes',     '09171110002', 'facebook.com/bianca.reyes.coach'),
('Rico Mendoza',     '09171110003', 'facebook.com/rico.mendoza.pt'),
('Katrina Flores',   '09171110004', 'facebook.com/katrina.flores.yoga'),
('Paolo Gutierrez',  '09171110005', 'facebook.com/paolo.gutierrez.strength');

/* ---- Membership_Plan (6) ---- */
INSERT INTO Membership_Plan (PlanName, PlanType, Price, DurationMonths) VALUES
('Monthly Basic',      'Monthly',   800.00,  1),
('Monthly Premium',    'Monthly',  1200.00,  1),
('Bi-Annual Standard', 'Bi-Annual', 4200.00, 6),
('Bi-Annual Premium',  'Bi-Annual', 5400.00, 6),
('Annual Standard',    'Annual',    7500.00, 12),
('Annual VIP',         'Annual',    9900.00, 12);

/* ---- Customer (8 members first = IDs 1-8, then 6 non-members = IDs 9-14) ---- */
INSERT INTO Customer (LName, FName, RegistrationDate, CustomerType) VALUES
('Reyes',     'Juan',     DATEADD(DAY, -230, @today), 'Member'),      -- 1
('Garcia',    'Andrea',   DATEADD(DAY, -200, @today), 'Member'),      -- 2
('Bautista',  'Miguel',   DATEADD(DAY, -180, @today), 'Member'),      -- 3
('Castillo',  'Sofia',    DATEADD(DAY, -165, @today), 'Member'),      -- 4
('Navarro',   'Gabriel',  DATEADD(DAY, -205, @today), 'Member'),      -- 5
('Villareal', 'Isabel',   DATEADD(DAY, -125, @today), 'Member'),      -- 6
('Domingo',   'Rafael',   DATEADD(DAY,  -62, @today), 'Member'),      -- 7
('Aquino',    'Camille',  DATEADD(DAY,  -78, @today), 'Member'),      -- 8
('Mercado',   'Paolo',    DATEADD(DAY,  -45, @today), 'Non-Member'),  -- 9
('Tan',       'Kevin',    DATEADD(DAY,  -33, @today), 'Non-Member'),  -- 10
('Lopez',     'Elena',    DATEADD(DAY,  -12, @today), 'Non-Member'),  -- 11
('Rivera',    'Dennis',   DATEADD(DAY,   -7, @today), 'Non-Member'),  -- 12
('Soriano',   'Jasmine',  DATEADD(DAY,   -3, @today), 'Non-Member'),  -- 13
('Pascual',   'Ryan',     DATEADD(DAY,   -1, @today), 'Non-Member');  -- 14

/* ---- Member (8) ---- */
INSERT INTO Member (MemberID, TrainerID, ContactNumber, Email, Birthdate, EmergencyContactNumber) VALUES
(1, 1,    '09181230001', 'juan.reyes@email.com',        '2001-03-14', '09281230001'),
(2, 2,    '09181230002', 'andrea.garcia@email.com',     '1999-07-22', '09281230002'),
(3, 1,    '09181230003', 'miguel.bautista@email.com',   '2002-11-05', '09281230003'),
(4, NULL, '09181230004', 'sofia.castillo@email.com',    '2000-01-30', '09281230004'),
(5, 3,    '09181230005', 'gabriel.navarro@email.com',   '1997-09-18', '09281230005'),
(6, 4,    '09181230006', 'isabel.villareal@email.com',  '2003-05-09', '09281230006'),
(7, 2,    '09181230007', 'rafael.domingo@email.com',    '1995-12-27', '09281230007'),
(8, NULL, '09181230008', 'camille.aquino@email.com',    '1998-04-16', '09281230008');

/* ---- Non_Member (6) ---- */
INSERT INTO Non_Member (NonMemberID) VALUES (9), (10), (11), (12), (13), (14);

/* ---- Medical_History (8) ---- */
INSERT INTO Medical_History (MemberID, MedicalCondition) VALUES
(1, 'None'),
(2, 'Asthma'),
(3, 'Hypertension'),
(4, 'None'),
(5, 'Lower back pain'),
(6, 'None'),
(7, 'Knee injury (recovering)'),
(8, 'Diabetes Type 2');

/* ---- Gym_Transaction (18) : 1-10 = memberships, 11-18 = day passes ---- */
INSERT INTO Gym_Transaction (StaffID, CustomerID, TransactionDate, TotalAmount) VALUES
(1, 1,  DATEADD(DAY,  -80, @today),  800.00),   -- 1
(3, 6,  DATEADD(DAY, -120, @today), 1200.00),   -- 2
(3, 8,  DATEADD(DAY,  -75, @today), 1200.00),   -- 3
(2, 5,  DATEADD(DAY, -200, @today), 9900.00),   -- 4
(5, 4,  DATEADD(DAY, -160, @today), 4200.00),   -- 5
(1, 2,  DATEADD(DAY,  -90, @today), 7500.00),   -- 6
(5, 7,  DATEADD(DAY,  -60, @today), 5400.00),   -- 7
(4, 3,  DATEADD(DAY,  -35, @today), 1200.00),   -- 8
(1, 1,  DATEADD(DAY,  -20, @today),  800.00),   -- 9
(6, 6,  DATEADD(DAY,  -10, @today),  800.00),   -- 10
(4, 9,  DATEADD(DAY,  -45, @today),  150.00),   -- 11
(1, 10, DATEADD(DAY,  -33, @today),  150.00),   -- 12
(4, 9,  DATEADD(DAY,  -18, @today),  150.00),   -- 13
(6, 11, DATEADD(DAY,  -12, @today),  150.00),   -- 14
(1, 12, DATEADD(DAY,   -7, @today),  150.00),   -- 15
(3, 13, DATEADD(DAY,   -3, @today),  150.00),   -- 16
(6, 14, DATEADD(DAY,   -1, @today),  200.00),   -- 17
(4, 10, @today,                      150.00);   -- 18

/* ---- Membership (10) : expiration = start + plan duration ---- */
INSERT INTO Membership (MemberID, PlanID, TransactionID, StartDate, ExpirationDate, Status) VALUES
(1, 1, 1,  DATEADD(DAY,  -80, @today), DATEADD(MONTH,  1, DATEADD(DAY,  -80, @today)), 'Expired'),
(6, 2, 2,  DATEADD(DAY, -120, @today), DATEADD(MONTH,  1, DATEADD(DAY, -120, @today)), 'Expired'),
(8, 2, 3,  DATEADD(DAY,  -75, @today), DATEADD(MONTH,  1, DATEADD(DAY,  -75, @today)), 'Cancelled'),
(5, 6, 4,  DATEADD(DAY, -200, @today), DATEADD(MONTH, 12, DATEADD(DAY, -200, @today)), 'Active'),
(4, 3, 5,  DATEADD(DAY, -160, @today), DATEADD(MONTH,  6, DATEADD(DAY, -160, @today)), 'Active'),
(2, 5, 6,  DATEADD(DAY,  -90, @today), DATEADD(MONTH, 12, DATEADD(DAY,  -90, @today)), 'Active'),
(7, 4, 7,  DATEADD(DAY,  -60, @today), DATEADD(MONTH,  6, DATEADD(DAY,  -60, @today)), 'Active'),
(3, 2, 8,  DATEADD(DAY,  -35, @today), DATEADD(MONTH,  1, DATEADD(DAY,  -35, @today)), 'Expired'),
(1, 1, 9,  DATEADD(DAY,  -20, @today), DATEADD(MONTH,  1, DATEADD(DAY,  -20, @today)), 'Active'),
(6, 1, 10, DATEADD(DAY,  -10, @today), DATEADD(MONTH,  1, DATEADD(DAY,  -10, @today)), 'Active');

/* ---- Day_Pass (8) : transactions 11-18 ---- */
INSERT INTO Day_Pass (NonMemberID, TransactionID, PurchaseDate, Price) VALUES
(9,  11, DATEADD(DAY, -45, @today), 150.00),
(10, 12, DATEADD(DAY, -33, @today), 150.00),
(9,  13, DATEADD(DAY, -18, @today), 150.00),
(11, 14, DATEADD(DAY, -12, @today), 150.00),
(12, 15, DATEADD(DAY,  -7, @today), 150.00),
(13, 16, DATEADD(DAY,  -3, @today), 150.00),
(14, 17, DATEADD(DAY,  -1, @today), 200.00),
(10, 18, @today,                    150.00);

/* ---- Payment (20) : every transaction is paid in full; 4 and 6 are split payments ---- */
INSERT INTO Payment (TransactionID, PaymentDate, Amount, PaymentType) VALUES
(1,  DATEADD(DAY,  -80, @today),  800.00, 'Cash'),      -- 1
(2,  DATEADD(DAY, -120, @today), 1200.00, 'Digital'),   -- 2
(3,  DATEADD(DAY,  -75, @today), 1200.00, 'Cash'),      -- 3
(4,  DATEADD(DAY, -200, @today), 5000.00, 'Cash'),      -- 4  (split)
(4,  DATEADD(DAY, -200, @today), 4900.00, 'Digital'),   -- 5  (split)
(5,  DATEADD(DAY, -160, @today), 4200.00, 'Digital'),   -- 6
(6,  DATEADD(DAY,  -90, @today), 3500.00, 'Cash'),      -- 7  (split)
(6,  DATEADD(DAY,  -90, @today), 4000.00, 'Digital'),   -- 8  (split)
(7,  DATEADD(DAY,  -60, @today), 5400.00, 'Digital'),   -- 9
(8,  DATEADD(DAY,  -35, @today), 1200.00, 'Cash'),      -- 10
(9,  DATEADD(DAY,  -20, @today),  800.00, 'Cash'),      -- 11
(10, DATEADD(DAY,  -10, @today),  800.00, 'Digital'),   -- 12
(11, DATEADD(DAY,  -45, @today),  150.00, 'Cash'),      -- 13
(12, DATEADD(DAY,  -33, @today),  150.00, 'Cash'),      -- 14
(13, DATEADD(DAY,  -18, @today),  150.00, 'Digital'),   -- 15
(14, DATEADD(DAY,  -12, @today),  150.00, 'Cash'),      -- 16
(15, DATEADD(DAY,   -7, @today),  150.00, 'Digital'),   -- 17
(16, DATEADD(DAY,   -3, @today),  150.00, 'Cash'),      -- 18
(17, DATEADD(DAY,   -1, @today),  200.00, 'Cash'),      -- 19
(18, @today,                      150.00, 'Digital');   -- 20

/* ---- Cash (11) : every Payment of type Cash ---- */
INSERT INTO Cash (CPaymentID)
SELECT PaymentID FROM Payment WHERE PaymentType = 'Cash';

/* ---- Digital_Payment (9) : every Payment of type Digital + reference number ---- */
INSERT INTO Digital_Payment (DPaymentID, ReferenceNumber) VALUES
(2,  'GCASH-7781203451'),
(5,  'GCASH-7781209982'),
(6,  'MAYA-5520011876'),
(8,  'GCASH-7781230045'),
(9,  'BANK-2026-00418273'),
(12, 'MAYA-5520044120'),
(15, 'GCASH-7781255610'),
(17, 'MAYA-5520061993'),
(20, 'GCASH-7781270334');

/* ---- Training_Session (9) ---- */
INSERT INTO Training_Session (TrainerID, SessionDate, Fee) VALUES
(1, DATEADD(DAY, -40, @today), 500.00),   -- 1
(2, DATEADD(DAY, -30, @today), 600.00),   -- 2
(1, DATEADD(DAY, -21, @today), 500.00),   -- 3
(3, DATEADD(DAY, -14, @today), 700.00),   -- 4
(4, DATEADD(DAY,  -9, @today), 650.00),   -- 5
(2, DATEADD(DAY,  -5, @today), 600.00),   -- 6
(1, DATEADD(DAY,  -2, @today), 500.00),   -- 7
(5, DATEADD(DAY,  -1, @today), 800.00),   -- 8
(3, @today,                    700.00);   -- 9

/* ---- Session_Attendance (15) ---- */
INSERT INTO Session_Attendance (MemberID, SessionID) VALUES
(1, 1), (3, 1),
(2, 2), (7, 2), (8, 2),
(1, 3),
(5, 4),
(6, 5),
(2, 6), (7, 6),
(1, 7), (3, 7),
(4, 8), (5, 8),
(5, 9);
GO

-- Give the staff realistic hire dates (UPDATE on the column added by ALTER TABLE)
UPDATE Staff SET HireDate = '2022-01-10' WHERE StaffID = 1;
UPDATE Staff SET HireDate = '2020-06-01' WHERE StaffID = 2;
UPDATE Staff SET HireDate = '2023-03-15' WHERE StaffID = 3;
UPDATE Staff SET HireDate = '2023-08-21' WHERE StaffID = 4;
UPDATE Staff SET HireDate = '2021-11-02' WHERE StaffID = 5;
UPDATE Staff SET HireDate = '2024-02-12' WHERE StaffID = 6;
GO


/* =====================================================================
   PART 5 - CRUD DEMONSTRATION
   Each demo creates a record, reads it, updates it and deletes it again,
   so the sample data used by the GUI stays clean afterwards.
   ===================================================================== */

/* ---------- 5A. CRUD on TRAINER ---------- */
-- CREATE
INSERT INTO Trainer (TrainerName, ContactNumber, FBAccount)
VALUES ('Demo Trainer', '09179990000', 'facebook.com/demo.trainer');

-- READ
SELECT * FROM Trainer WHERE TrainerName = 'Demo Trainer';

-- UPDATE
UPDATE Trainer
SET    ContactNumber = '09179991111', FBAccount = 'facebook.com/demo.trainer.updated'
WHERE  TrainerName = 'Demo Trainer';

SELECT * FROM Trainer WHERE TrainerName = 'Demo Trainer';   -- shows the new values

-- DELETE
DELETE FROM Trainer WHERE TrainerName = 'Demo Trainer';

SELECT COUNT(*) AS DemoTrainersLeft FROM Trainer WHERE TrainerName = 'Demo Trainer';  -- 0
DBCC CHECKIDENT ('Trainer', RESEED, 5);   -- put the ID counter back so the next real trainer is ID 6
GO

/* ---------- 5B. CRUD on MEMBER (Customer + Member + Medical_History) ----------
   Runs inside a transaction that is ROLLED BACK at the end, so nothing
   is kept. Remove the ROLLBACK (and DBCC lines) if you want to keep it. */
BEGIN TRANSACTION;

    -- CREATE
    INSERT INTO Customer (LName, FName, RegistrationDate, CustomerType)
    VALUES ('Demo', 'Member', CAST(GETDATE() AS DATE), 'Member');

    DECLARE @newId INT = SCOPE_IDENTITY();

    INSERT INTO Member (MemberID, TrainerID, ContactNumber, Email, Birthdate, EmergencyContactNumber)
    VALUES (@newId, 1, '09175550000', 'demo.member@email.com', '2000-01-01', '09275550000');

    INSERT INTO Medical_History (MemberID, MedicalCondition) VALUES (@newId, 'None');

    -- READ (JOIN across the three tables)
    SELECT c.CustomerID, c.LName, c.FName, m.ContactNumber, m.Email, mh.MedicalCondition
    FROM   Customer c
           INNER JOIN Member m           ON m.MemberID  = c.CustomerID
           INNER JOIN Medical_History mh ON mh.MemberID = m.MemberID
    WHERE  c.CustomerID = @newId;

    -- UPDATE
    UPDATE Member          SET ContactNumber = '09175551111' WHERE MemberID = @newId;
    UPDATE Medical_History SET MedicalCondition = 'Asthma'   WHERE MemberID = @newId;

    SELECT m.MemberID, m.ContactNumber, mh.MedicalCondition
    FROM   Member m JOIN Medical_History mh ON mh.MemberID = m.MemberID
    WHERE  m.MemberID = @newId;

    -- DELETE (Medical_History is removed automatically by ON DELETE CASCADE)
    DELETE FROM Member   WHERE MemberID   = @newId;
    DELETE FROM Customer WHERE CustomerID = @newId;

    SELECT COUNT(*) AS MedicalRowsLeft FROM Medical_History WHERE MemberID = @newId;  -- 0

ROLLBACK TRANSACTION;
DBCC CHECKIDENT ('Customer', RESEED, 14);   -- 14 customers exist, next real one is ID 15
GO

/* ---------- 5C. Useful UPDATE / DELETE on real data (same rule the GUI uses) ---------- */
-- Mark every Active membership whose expiration date already passed as Expired
UPDATE Membership
SET    Status = 'Expired'
WHERE  Status = 'Active' AND ExpirationDate < CAST(GETDATE() AS DATE);

-- Example of a DELETE with a condition (returns 0 rows because no such record exists)
DELETE FROM Session_Attendance
WHERE  SessionID NOT IN (SELECT SessionID FROM Training_Session);
GO


/* =====================================================================
   PART 6 - SELECT: CONDITIONS, JOINS AND FUNCTIONS
   ===================================================================== */

-- 6.1  WHERE + ORDER BY: all active memberships, soonest expiry first
SELECT MembershipID, MemberID, StartDate, ExpirationDate, Status
FROM   Membership
WHERE  Status = 'Active'
ORDER BY ExpirationDate;

-- 6.2  INNER JOIN + String functions (CONCAT, UPPER, LOWER)
SELECT c.CustomerID,
       CONCAT(UPPER(c.LName), ', ', c.FName) AS FullName,
       LOWER(m.Email)                        AS EmailLower,
       t.TrainerName
FROM   Customer c
       INNER JOIN Member m  ON m.MemberID  = c.CustomerID
       INNER JOIN Trainer t ON t.TrainerID = m.TrainerID
ORDER BY c.LName;

-- 6.3  LEFT JOIN: every member, even those with no trainer
SELECT CONCAT(c.FName, ' ', c.LName) AS MemberName,
       ISNULL(t.TrainerName, 'No trainer assigned') AS Trainer
FROM   Member m
       INNER JOIN Customer c ON c.CustomerID = m.MemberID
       LEFT  JOIN Trainer  t ON t.TrainerID  = m.TrainerID;

-- 6.4  RIGHT JOIN: every trainer, even those with no members
SELECT t.TrainerName, COUNT(m.MemberID) AS AssignedMembers
FROM   Member m
       RIGHT JOIN Trainer t ON t.TrainerID = m.TrainerID
GROUP BY t.TrainerName
ORDER BY AssignedMembers DESC;

-- 6.5  Date functions (GETDATE, DATEDIFF, DATEADD): days left on active memberships
SELECT MembershipID,
       ExpirationDate,
       DATEDIFF(DAY, GETDATE(), ExpirationDate)              AS DaysLeft,
       DATEADD(DAY, 7, ExpirationDate)                       AS GraceDeadline
FROM   Membership
WHERE  Status = 'Active';

-- 6.6  Member age (DATEDIFF) with a computed column
SELECT CONCAT(c.FName, ' ', c.LName) AS MemberName,
       m.Birthdate,
       DATEDIFF(YEAR, m.Birthdate, GETDATE()) AS Age
FROM   Member m JOIN Customer c ON c.CustomerID = m.MemberID
ORDER BY Age DESC;

-- 6.7  Aggregate functions on transactions
SELECT COUNT(*)         AS TotalTransactions,
       SUM(TotalAmount) AS TotalRevenue,
       AVG(TotalAmount) AS AverageSale,
       MIN(TotalAmount) AS SmallestSale,
       MAX(TotalAmount) AS LargestSale
FROM   Gym_Transaction;

-- 6.8  GROUP BY + HAVING: plans sold more than once
SELECT mp.PlanName, COUNT(*) AS TimesSold
FROM   Membership ms JOIN Membership_Plan mp ON mp.PlanID = ms.PlanID
GROUP BY mp.PlanName
HAVING COUNT(*) > 1;

-- 6.9  Computed column: total fees collected per session (Fee x Attendees)
SELECT ts.SessionID, ts.SessionDate, ts.Fee,
       COUNT(sa.MemberID)           AS Attendees,
       ts.Fee * COUNT(sa.MemberID)  AS TotalFees
FROM   Training_Session ts
       LEFT JOIN Session_Attendance sa ON sa.SessionID = ts.SessionID
GROUP BY ts.SessionID, ts.SessionDate, ts.Fee
ORDER BY ts.SessionDate;
GO


/* =====================================================================
   PART 7 - SQL REPORTS (VIEWS)
   These return the same results as Reports R1-R9 in the Java GUI.
   ===================================================================== */

-- R1. Membership revenue by plan (LEFT JOIN, GROUP BY, SUM, COUNT)
CREATE VIEW vw_MembershipRevenueByPlan AS
SELECT mp.PlanName, mp.PlanType,
       COUNT(ms.MembershipID)         AS TotalMemberships,
       ISNULL(SUM(gt.TotalAmount), 0) AS TotalRevenue
FROM   Membership_Plan mp
       LEFT JOIN Membership ms       ON ms.PlanID = mp.PlanID
       LEFT JOIN Gym_Transaction gt  ON gt.TransactionID = ms.TransactionID
GROUP BY mp.PlanName, mp.PlanType;
GO

-- R2. Memberships expiring in the next 30 days (date functions, computed DaysLeft)
CREATE VIEW vw_ExpiringMemberships AS
SELECT CONCAT(c.FName, ' ', c.LName) AS MemberName,
       m.ContactNumber, mp.PlanName, ms.ExpirationDate,
       DATEDIFF(DAY, GETDATE(), ms.ExpirationDate) AS DaysLeft
FROM   Membership ms
       JOIN Member m           ON m.MemberID   = ms.MemberID
       JOIN Customer c         ON c.CustomerID = m.MemberID
       JOIN Membership_Plan mp ON mp.PlanID    = ms.PlanID
WHERE  ms.Status = 'Active'
  AND  ms.ExpirationDate BETWEEN CAST(GETDATE() AS DATE) AND DATEADD(DAY, 30, CAST(GETDATE() AS DATE));
GO

-- R3. Trainer workload and session fees (computed column TotalSessionFees)
CREATE VIEW vw_TrainerWorkload AS
SELECT t.TrainerName,
       COUNT(DISTINCT ts.SessionID) AS SessionsConducted,
       COUNT(sa.MemberID)           AS TotalAttendees,
       SUM(CASE WHEN sa.MemberID IS NOT NULL THEN ts.Fee ELSE 0 END) AS TotalSessionFees
FROM   Trainer t
       LEFT JOIN Training_Session ts    ON ts.TrainerID = t.TrainerID
       LEFT JOIN Session_Attendance sa  ON sa.SessionID = ts.SessionID
GROUP BY t.TrainerName;
GO

-- R4. Staff sales performance (COUNT, SUM, AVG, MAX)
CREATE VIEW vw_StaffSales AS
SELECT s.StaffName, s.Position,
       COUNT(gt.TransactionID)                       AS TransactionsRecorded,
       ISNULL(SUM(gt.TotalAmount), 0)                AS TotalSales,
       ISNULL(CAST(AVG(gt.TotalAmount) AS DECIMAL(10,2)), 0) AS AverageSale,
       ISNULL(MAX(gt.TotalAmount), 0)                AS LargestSale
FROM   Staff s LEFT JOIN Gym_Transaction gt ON gt.StaffID = s.StaffID
GROUP BY s.StaffName, s.Position;
GO

-- R5. Collections by payment method (computed percentage)
CREATE VIEW vw_PaymentMethodSummary AS
SELECT p.PaymentType,
       COUNT(*)      AS NumberOfPayments,
       SUM(p.Amount) AS TotalCollected,
       CAST(100.0 * SUM(p.Amount) / (SELECT SUM(Amount) FROM Payment) AS DECIMAL(5,2)) AS PercentShare
FROM   Payment p
GROUP BY p.PaymentType;
GO

-- R7. Monthly revenue (YEAR, MONTH, DATENAME)
CREATE VIEW vw_MonthlyRevenue AS
SELECT YEAR(TransactionDate)  AS SalesYear,
       MONTH(TransactionDate) AS MonthNo,
       DATENAME(MONTH, TransactionDate) AS SalesMonth,
       COUNT(*)               AS Transactions,
       SUM(TotalAmount)       AS MonthlyRevenue
FROM   Gym_Transaction
GROUP BY YEAR(TransactionDate), MONTH(TransactionDate), DATENAME(MONTH, TransactionDate);
GO

-- R8. Transaction balance / payment reconciliation (computed Balance)
CREATE VIEW vw_TransactionBalance AS
SELECT gt.TransactionID, gt.TransactionDate, gt.TotalAmount,
       ISNULL(SUM(p.Amount), 0)                    AS TotalPaid,
       gt.TotalAmount - ISNULL(SUM(p.Amount), 0)   AS Balance
FROM   Gym_Transaction gt
       LEFT JOIN Payment p ON p.TransactionID = gt.TransactionID
GROUP BY gt.TransactionID, gt.TransactionDate, gt.TotalAmount;
GO

/* ---------- Run the reports ---------- */
SELECT * FROM vw_MembershipRevenueByPlan ORDER BY TotalRevenue DESC;
SELECT * FROM vw_ExpiringMemberships     ORDER BY ExpirationDate;
SELECT * FROM vw_TrainerWorkload         ORDER BY TotalSessionFees DESC;
SELECT * FROM vw_StaffSales              ORDER BY TotalSales DESC;
SELECT * FROM vw_PaymentMethodSummary    ORDER BY TotalCollected DESC;
SELECT * FROM vw_MonthlyRevenue          ORDER BY SalesYear, MonthNo;
SELECT * FROM vw_TransactionBalance      WHERE Balance <> 0;   -- empty = all transactions fully paid
GO

PRINT 'GymManagementDB created successfully.';
GO