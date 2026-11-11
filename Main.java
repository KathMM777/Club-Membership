import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}

class DBConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ClubMembership";
    private static final String DB_USER = "root";
    private static String password;    // typed once in a pop-up, so it is never stored in the code

    static Connection get() throws SQLException {
        if (password == null) {
            JPasswordField field = new JPasswordField();
            int choice = JOptionPane.showConfirmDialog(null, field,
                    "MySQL password for " + DB_USER, JOptionPane.OK_CANCEL_OPTION);
            if (choice != JOptionPane.OK_OPTION) {
                throw new SQLException("Login cancelled");
            }
            password = new String(field.getPassword());
        }
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, password);
        } catch (SQLException ex) {
            password = null;           // wrong password: ask again next time
            throw ex;
        }
    }
}

/** Lets a menu item run a query that returns a table model (and may throw SQLException). */
interface QueryAction {
    DefaultTableModel run() throws SQLException;
}

class MainWindow extends JFrame {

    MainWindow() {
        setTitle("Club Membership");
        setSize(500, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();

        // ----- Insert menu  -----
        JMenu insertMenu = new JMenu("Insert");
        insertMenu.add(menuItem("Add Student", () -> new StudentInsertDialog(this).setVisible(true)));
        insertMenu.add(menuItem("Add Club", () -> new ClubInsertDialog(this).setVisible(true)));
        insertMenu.add(menuItem("Add Membership", () -> new MembershipInsertDialog(this).setVisible(true)));
        menuBar.add(insertMenu);

        // ----- Queries menu  -----
        JMenu queryMenu = new JMenu("Queries");
        // Karen's Queries
        queryMenu.add(menuItem("Members and Officers",
                () -> showQuery("Members and Officers", Queries::roleCategory)));
        queryMenu.add(menuItem("Students and their Clubs by Major", this::askMajorAndShow));
        // Katherine's Queries
        queryMenu.add(menuItem("Students in a Club", this::askClubAndShow));
        queryMenu.add(menuItem("Count Students by Club",
                () -> showQuery("Students per Club", Queries::countStudentsByClub)));
        // Chris's Queries
        queryMenu.add(menuItem("Clubs a Student Belongs To", this::askStudentAndShow));
        queryMenu.add(menuItem("Students in Every Club on a Day", this::askDayAndShow));

        menuBar.add(queryMenu);
        setJMenuBar(menuBar);          // attaches the menu bar to the window
        add(new JLabel("Choose an action from the menu above.", SwingConstants.CENTER));
    }

    /** Runs a query and shows the result in a table window, or shows an error. */
    private void showQuery(String title, QueryAction query) {
        try {
            new ResultDialog(this, title, query.run()).setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Karen: Asks which major to search for, then runs the query.
    private void askMajorAndShow() {
        String major = (String) JOptionPane.showInputDialog(this, "Major:", "Students by Major",
                JOptionPane.QUESTION_MESSAGE, null, null, "");
        if (major == null || major.trim().isEmpty()) {
            return;                                  // user cancelled or left it blank
        }
        showQuery("Students in " + major.trim(), () -> Queries.studentsByMajor(major.trim()));
    }

    //Katherine: Asks the user for a Club ID, then shows a table of all students who belong to that club.
    private void askClubAndShow() {
        String input = JOptionPane.showInputDialog(this, "Enter Club ID:");
        if (input == null) {
            return;
        }
        try {
            int clubID = Integer.parseInt(input.trim());
            showQuery("Students in Club " + clubID, () -> Queries.studentsByClub(clubID));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Club ID.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Chris: Asks for a Student ID, then shows all clubs that student belongs to.
    private void askStudentAndShow() {
        String input = JOptionPane.showInputDialog(this, "Enter Student ID:");
        if (input == null) {
            return;                                       // user clicked Cancel
        }
        try {
            int studentID = Integer.parseInt(input.trim());   // text -> number
            showQuery("Clubs for Student " + studentID, () -> Queries.clubsByStudent(studentID));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Student ID.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Chris: Lets the user pick a meeting day from a dropdown, then runs the query.
    private void askDayAndShow() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String day = (String) JOptionPane.showInputDialog(this, "Meeting day:", "Students in Every Club on a Day",
                JOptionPane.QUESTION_MESSAGE, null, days, days[0]);   // days = the dropdown choices
        if (day == null) {
            return;                                       // user clicked Cancel
        }
        showQuery("Students in every " + day + " club", () -> Queries.studentsInAllClubsOnDay(day));
    }

    private JMenuItem menuItem(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }
}

abstract class FormDialog extends JDialog {

    FormDialog(JFrame parent, String title) {
        super(parent, title, true);
        setLayout(new GridLayout(0, 2, 8, 8));
    }

    /** Adds one "label : field" row to the form. */
    protected void addRow(String label, JComponent field) {
        add(new JLabel(label));
        add(field);
    }

    /** Adds the submit button as the last row, then sizes and positions the dialog. */
    protected void addSubmitButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        add(new JLabel());
        add(button);
        pack();
        setLocationRelativeTo(getParent());
    }

    /** Returns trimmed text from a field, or throws if it is empty. */
    protected String required(JTextField field, String fieldName) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    /** Parses a whole number from a field, with a friendly error message. */
    protected int requiredInt(JTextField field, String fieldName) {
        try {
            return Integer.parseInt(required(field, fieldName));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}

// Karen: INSERT one row into Student.
class StudentInsertDialog extends FormDialog {
    private final JTextField idField = new JTextField(15);
    private final JTextField firstNameField = new JTextField(15);
    private final JTextField lastNameField = new JTextField(15);
    private final JTextField emailField = new JTextField(15);
    private final JTextField majorField = new JTextField(15);
    private final JComboBox<String> classYearBox =
            new JComboBox<>(new String[]{"Freshman", "Sophomore", "Junior", "Senior"});

    StudentInsertDialog(JFrame parent) {
        super(parent, "Add Student");
        addRow("Student ID:", idField);
        addRow("First Name:", firstNameField);
        addRow("Last Name:", lastNameField);
        addRow("Email:", emailField);
        addRow("Major:", majorField);
        addRow("Class Year:", classYearBox);
        addSubmitButton("Submit", this::insertStudent);
    }

    private void insertStudent() {
        String sql = "INSERT INTO Student (StudentID, FirstName, LastName, Email, Major, ClassYear) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, requiredInt(idField, "Student ID"));
            ps.setString(2, required(firstNameField, "First name"));
            ps.setString(3, required(lastNameField, "Last name"));
            ps.setString(4, required(emailField, "Email"));
            ps.setString(5, majorField.getText().trim());
            ps.setString(6, (String) classYearBox.getSelectedItem());
            ps.executeUpdate();      // runs the INSERT

            showInfo("Student added.");
            dispose();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }
}

// Katherine
class ClubInsertDialog extends FormDialog {
    private final JTextField idField = new JTextField(15);
    private final JTextField nameField = new JTextField(15);
    private final JTextField descriptionField = new JTextField(15);
    // Added drop down to reduce user errors
    private final JComboBox<String> dayBox = new JComboBox<>(new String[]{
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"});
    private final JTextField locationField = new JTextField(15);

    ClubInsertDialog(JFrame parent) {
        super(parent, "Add Club");
        addRow("Club ID:", idField);
        addRow("Club Name:", nameField);
        addRow("Description:", descriptionField);
        addRow("Meeting Day:", dayBox);
        addRow("Meeting Location:", locationField);
        addSubmitButton("Submit", this::insertClub);
    }

    private void insertClub() {
        String sql = "INSERT INTO Club (ClubID, ClubName, Description, MeetingDay, MeetingLocation) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, requiredInt(idField, "Club ID"));
            ps.setString(2, required(nameField, "Club name"));
            ps.setString(3, descriptionField.getText().trim());
            ps.setString(4, (String) dayBox.getSelectedItem());
            ps.setString(5, locationField.getText().trim());
            ps.executeUpdate();

            showInfo("Club added.");
            dispose();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }
}

/** Chris: INSERT one row into Membership. The student and club are picked from lists. */
class MembershipInsertDialog extends FormDialog {
    // One dropdown entry: the id we save, and the text the user sees.
    private record Item(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final JComboBox<Item> studentBox = new JComboBox<>();
    private final JComboBox<Item> clubBox = new JComboBox<>();
    private final JTextField joinDateField = new JTextField(java.time.LocalDate.now().toString(), 15);
    private final JComboBox<String> positionBox = new JComboBox<>(
            new String[]{"Member", "President", "Vice President", "Treasurer", "Secretary"});

    MembershipInsertDialog(JFrame parent) {
        super(parent, "Add Membership");
        try {
            loadList(studentBox, "SELECT StudentID, CONCAT(FirstName, ' ', LastName, ' (ID ', StudentID, ')') "
                    + "FROM Student ORDER BY LastName, FirstName");
            loadList(clubBox, "SELECT ClubID, ClubName FROM Club ORDER BY ClubName");
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
        addRow("Student:", studentBox);
        addRow("Club:", clubBox);
        addRow("Join Date (YYYY-MM-DD):", joinDateField);
        addRow("Position:", positionBox);
        addSubmitButton("Submit", this::insertMembership);
    }

    /** Fills a dropdown from a query that returns (id, label). */
    private void loadList(JComboBox<Item> box, String sql) throws SQLException {
        try (Connection conn = DBConnection.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                box.addItem(new Item(rs.getInt(1), rs.getString(2)));
            }
        }
    }

    private void insertMembership() {
        Item student = (Item) studentBox.getSelectedItem();
        Item club = (Item) clubBox.getSelectedItem();
        if (student == null || club == null) {
            showError("Please pick a student and a club.");
            return;
        }
        java.time.LocalDate joinDate;
        try {
            joinDate = java.time.LocalDate.parse(required(joinDateField, "Join date"));
        } catch (java.time.format.DateTimeParseException ex) {
            showError("Join date must look like 2026-11-04 (year-month-day).");
            return;
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        String sql = "INSERT INTO Membership (StudentID, ClubID, JoinDate, Position) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, student.id());
            ps.setInt(2, club.id());
            ps.setDate(3, java.sql.Date.valueOf(joinDate));
            ps.setString(4, (String) positionBox.getSelectedItem());
            ps.executeUpdate();

            showInfo("Membership added.");
            dispose();
        } catch (SQLIntegrityConstraintViolationException ex) {
            showError("That student is already a member of that club.");
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }
}

// Holds the SQL queries. Each method runs one query and returns the result as a table model.
class Queries {

    // ----- Karen's queries -----
    private static final String ROLE_CATEGORY_SQL =
            "SELECT Student.FirstName, Student.LastName, Club.ClubName, Membership.Position, "
                    + "CASE "
                    + "  WHEN Membership.Position IN ('President', 'Vice President', 'Treasurer', 'Secretary') "
                    + "       THEN 'Officer' "
                    + "  WHEN Membership.Position = 'Member' THEN 'General Member' "
                    + "  ELSE 'Other' "
                    + "END AS RoleCategory "
                    + "FROM Student "
                    + "JOIN Membership ON Student.StudentID = Membership.StudentID "
                    + "JOIN Club ON Membership.ClubID = Club.ClubID";

    private static final String BY_MAJOR_SQL =
            "SELECT Student.FirstName, Student.LastName, Student.Major, "
                    + "Club.ClubName, Membership.Position "
                    + "FROM Student "
                    + "JOIN Membership ON Student.StudentID = Membership.StudentID "
                    + "JOIN Club ON Membership.ClubID = Club.ClubID "
                    + "WHERE Student.Major = @major";

  // ----- Katherine's Queries -----
// Changed Name to FirstName and LastName
private static final String STUDENTS_BY_CLUB_SQL =
        "SELECT s.StudentID, CONCAT(s.FirstName, ' ', s.LastName) AS StudentName, c.ClubName "
                + "FROM Student s "
                + "JOIN Membership m ON s.StudentID = m.StudentID "
                + "JOIN Club c ON m.ClubID = c.ClubID "
                + "WHERE c.ClubID = ?";

// Katherine: Count students by club using a subquery and CASE
private static final String COUNT_BY_CLUB_SQL =
        "SELECT c.ClubID, c.ClubName, "
                + "COUNT(m.StudentID) AS TotalStudents, "
                + "CASE "
                + "WHEN COUNT(m.StudentID) > ( "
                + "SELECT AVG(ClubCount.TotalStudents) "
                + "FROM ( "
                + "SELECT COUNT(m2.StudentID) AS TotalStudents "
                + "FROM Club c2 "
                + "LEFT JOIN Membership m2 ON c2.ClubID = m2.ClubID "
                + "GROUP BY c2.ClubID "
                + ") AS ClubCount "
                + ") "
                + "THEN 'Above average' "
                + "ELSE 'Average or below' "
                + "END AS MembershipCategory "
                + "FROM Club c "
                + "LEFT JOIN Membership m ON c.ClubID = m.ClubID "
                + "GROUP BY c.ClubID, c.ClubName "
                + "ORDER BY c.ClubID";

    // ----- Chris's Queries -----
    private static final String CLUBS_BY_STUDENT_SQL =
            "SELECT Club.ClubName, Club.MeetingDay, Membership.Position, Membership.JoinDate "
                    + "FROM Student "
                    + "JOIN Membership ON Student.StudentID = Membership.StudentID "
                    + "JOIN Club ON Membership.ClubID = Club.ClubID "
                    + "WHERE Student.StudentID = ?";

    // "Students in every club that meets on a given day": a student is returned only if
// there is NO club on that day that they are NOT a member of.
    private static final String STUDENTS_IN_ALL_CLUBS_ON_DAY_SQL =
            "SELECT Student.FirstName, Student.LastName FROM Student "
                    + "WHERE EXISTS (SELECT * FROM Club WHERE Club.MeetingDay = ?) "   // day must have at least one club
                    + "AND NOT EXISTS ( "
                    + "    SELECT * FROM Club "
                    + "    WHERE Club.MeetingDay = ? "
                    + "    AND NOT EXISTS ( "
                    + "        SELECT * FROM Membership "
                    + "        WHERE Membership.StudentID = Student.StudentID "
                    + "        AND Membership.ClubID = Club.ClubID "
                    + "    ) "
                    + ")";

    static DefaultTableModel roleCategory() throws SQLException {
        try (Connection conn = DBConnection.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(ROLE_CATEGORY_SQL)) {
            return toTableModel(rs);
        }
    }

    static DefaultTableModel studentsByMajor(String major) throws SQLException {
        try (Connection conn = DBConnection.get()) {
            try (PreparedStatement set = conn.prepareStatement("SET @major = ?")) {
                set.setString(1, major);
                set.execute();
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(BY_MAJOR_SQL)) {
                return toTableModel(rs);
            }
        }
    }

    /** Katherine's Query 1: students registered in one club (the club ID fills the ?). */
    static DefaultTableModel studentsByClub(int clubID) throws SQLException {
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(STUDENTS_BY_CLUB_SQL)) {
            ps.setInt(1, clubID);
            try (ResultSet rs = ps.executeQuery()) {
                return toTableModel(rs);
            }
        }
    }

    /** Katherine's Query 2: number of students in each club. */
    static DefaultTableModel countStudentsByClub() throws SQLException {
        try (Connection conn = DBConnection.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(COUNT_BY_CLUB_SQL)) {
            return toTableModel(rs);
        }
    }

    /** Chris's Query 1: all clubs one student belongs to (the student ID fills the ?). */
    static DefaultTableModel clubsByStudent(int studentID) throws SQLException {
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(CLUBS_BY_STUDENT_SQL)) {
            ps.setInt(1, studentID);              // number -> setInt
            try (ResultSet rs = ps.executeQuery()) {
                return toTableModel(rs);
            }
        }
    }

    /** Chris's Query 2: students who are in every club that meets on the given day. */
    static DefaultTableModel studentsInAllClubsOnDay(String day) throws SQLException {
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(STUDENTS_IN_ALL_CLUBS_ON_DAY_SQL)) {
            ps.setString(1, day);                 // text -> setString
            ps.setString(2, day);                 // the same day fills the second ?
            try (ResultSet rs = ps.executeQuery()) {
                return toTableModel(rs);
            }
        }
    }

    private static DefaultTableModel toTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        String[] columns = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columns[i] = meta.getColumnLabel(i + 1);
        }

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getObject(i + 1);
            }
            model.addRow(row);
        }
        return model;
    }
}

/** A window that shows a query result in a scrollable table. */
class ResultDialog extends JDialog {
    ResultDialog(JFrame parent, String title, DefaultTableModel model) {
        super(parent, title, false);                 // false = doesn't block the main window
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);          // click a column header to sort
        add(new JScrollPane(table), BorderLayout.CENTER);   // scroll bars if it's big
        setSize(700, 350);
        setLocationRelativeTo(parent);
    }
}
