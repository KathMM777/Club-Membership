import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Club Membership app (Swing + JDBC), one file.
 *
 *  - Main menu: pick "Add a Membership" or "Run Queries".
 *  - Insert screen: adds a row to the Membership table (user picks values).
 *  - Query screen: Query 1 (members of a club) and Query 4 (students in every
 *    club that meets on a day), both driven by user input, shown in a table.
 *
 * The MySQL password is typed in a pop-up, so it is never stored in the code.
 */
public class ClubApp extends JFrame {

    // =====================================================================
    // Main menu
    // =====================================================================
    public ClubApp() {
        super("Club Membership - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel title = new JLabel("Club Membership Database", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JButton insertButton = new JButton("Add a Membership");
        insertButton.addActionListener(e -> openInsertScreen());
        JButton queryButton = new JButton("Run Queries");
        queryButton.addActionListener(e -> new QueryScreen().setVisible(true));
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 10, 10));
        buttons.setBorder(BorderFactory.createEmptyBorder(16, 40, 24, 40));
        buttons.add(insertButton);
        buttons.add(queryButton);
        buttons.add(exitButton);

        add(title, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        setSize(380, 280);
        setLocationRelativeTo(null);
    }

    private void openInsertScreen() {
        try {
            new MembershipInsertForm().setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not open the form: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClubApp().setVisible(true));
    }

    // =====================================================================
    // Database connection (asks for the password once, then reuses it)
    // =====================================================================
    static class Db {
        private static final String URL = "jdbc:mysql://localhost:3306/ClubMembership";
        private static final String USER = "root";
        private static Connection connection;

        static Connection get() throws SQLException {
            if (connection == null || connection.isClosed()) {
                JPasswordField field = new JPasswordField();
                int choice = JOptionPane.showConfirmDialog(null, field,
                        "MySQL password for root", JOptionPane.OK_CANCEL_OPTION);
                if (choice != JOptionPane.OK_OPTION) {
                    throw new SQLException("Login cancelled");
                }
                connection = DriverManager.getConnection(URL, USER, new String(field.getPassword()));
            }
            return connection;
        }
    }

    // =====================================================================
    // Insert screen: add a row to Membership
    // =====================================================================
    static class MembershipInsertForm extends JFrame {
        // One dropdown entry: the id we save, and the text the user sees.
        private record Item(int id, String label) {
            @Override
            public String toString() {
                return label;
            }
        }

        private final JComboBox<Item> studentBox = new JComboBox<>();
        private final JComboBox<Item> clubBox = new JComboBox<>();
        private final JTextField joinDateField = new JTextField(LocalDate.now().toString(), 15);
        private final JComboBox<String> positionBox = new JComboBox<>(
                new String[]{"Member", "President", "Vice President", "Treasurer", "Secretary"});
        private final JLabel statusLabel = new JLabel(" ");

        MembershipInsertForm() throws SQLException {
            super("Add a Membership");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            loadStudents();
            loadClubs();
            buildLayout();
            setSize(500, 290);
            setLocationRelativeTo(null);
        }

        private void loadStudents() throws SQLException {
            String sql = "SELECT StudentID, FirstName, LastName FROM Student ORDER BY LastName, FirstName";
            try (Statement s = Db.get().createStatement(); ResultSet r = s.executeQuery(sql)) {
                while (r.next()) {
                    String label = r.getString("FirstName") + " " + r.getString("LastName")
                            + " (ID " + r.getInt("StudentID") + ")";
                    studentBox.addItem(new Item(r.getInt("StudentID"), label));
                }
            }
        }

        private void loadClubs() throws SQLException {
            String sql = "SELECT ClubID, ClubName FROM Club ORDER BY ClubName";
            try (Statement s = Db.get().createStatement(); ResultSet r = s.executeQuery(sql)) {
                while (r.next()) {
                    clubBox.addItem(new Item(r.getInt("ClubID"), r.getString("ClubName")));
                }
            }
        }

        private void buildLayout() {
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
            form.add(new JLabel("Student:"));
            form.add(studentBox);
            form.add(new JLabel("Club:"));
            form.add(clubBox);
            form.add(new JLabel("Join date (YYYY-MM-DD):"));
            form.add(joinDateField);
            form.add(new JLabel("Position:"));
            form.add(positionBox);

            JButton addButton = new JButton("Add Membership");
            addButton.addActionListener(e -> addMembership());

            JPanel bottom = new JPanel(new BorderLayout(0, 6));
            bottom.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
            bottom.add(addButton, BorderLayout.NORTH);
            bottom.add(statusLabel, BorderLayout.SOUTH);

            add(form, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        private void addMembership() {
            Item student = (Item) studentBox.getSelectedItem();
            Item club = (Item) clubBox.getSelectedItem();
            String position = (String) positionBox.getSelectedItem();

            if (student == null || club == null) {
                showStatus("Please pick a student and a club.", true);
                return;
            }
            LocalDate joinDate;
            try {
                joinDate = LocalDate.parse(joinDateField.getText().trim());
            } catch (DateTimeParseException ex) {
                showStatus("Join date must look like 2026-11-04 (year-month-day).", true);
                return;
            }

            String sql = "INSERT INTO Membership (StudentID, ClubID, JoinDate, Position) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
                ps.setInt(1, student.id());
                ps.setInt(2, club.id());
                ps.setDate(3, Date.valueOf(joinDate));
                ps.setString(4, position);
                ps.executeUpdate();
                showStatus("Added: " + student.label() + " joined " + club.label()
                        + " as " + position + ".", false);
            } catch (SQLIntegrityConstraintViolationException ex) {
                showStatus("That student is already a member of that club.", true);
            } catch (SQLException ex) {
                showStatus("Database error: " + ex.getMessage(), true);
            }
        }

        private void showStatus(String message, boolean isError) {
            statusLabel.setForeground(isError ? Color.RED : new Color(0, 120, 0));
            statusLabel.setText(message);
        }
    }

    // =====================================================================
    // Query screen: Query 1 and Query 4, results shown in a table
    // =====================================================================
    static class QueryScreen extends JFrame {
        // Query 1: members of a club. The club name is typed or picked by the user.
        private static final String QUERY_1 =
                "SELECT Student.FirstName AS `First Name`, Student.LastName AS `Last Name`, "
              + "Membership.Position AS `Position`, Membership.JoinDate AS `Join Date` "
              + "FROM Student "
              + "JOIN Membership ON Student.StudentID = Membership.StudentID "
              + "JOIN Club ON Membership.ClubID = Club.ClubID "
              + "WHERE Club.ClubName = ?";

        // Query 4 (division): students who belong to EVERY club that meets on the given day.
        // The first EXISTS line stops a day with no clubs from returning every student.
        private static final String QUERY_4 =
                "SELECT Student.FirstName AS `First Name`, Student.LastName AS `Last Name` "
              + "FROM Student "
              + "WHERE EXISTS (SELECT * FROM Club WHERE Club.MeetingDay = ?) "
              + "AND NOT EXISTS ( "
              + "    SELECT * FROM Club "
              + "    WHERE Club.MeetingDay = ? "
              + "    AND NOT EXISTS ( "
              + "        SELECT * FROM Membership "
              + "        WHERE Membership.StudentID = Student.StudentID "
              + "        AND Membership.ClubID = Club.ClubID))";

        private int currentQuery = 1;
        private final JLabel titleLabel = new JLabel();
        private final JLabel inputLabel = new JLabel();
        private final JComboBox<String> inputBox = new JComboBox<>();
        private final JTable table = new JTable();
        private final JLabel statusLabel = new JLabel(" ");

        QueryScreen() {
            super("Club Membership Queries");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            buildMenu();
            buildLayout();
            selectQuery(1);
            setSize(720, 460);
            setLocationRelativeTo(null);
        }

        private void buildMenu() {
            JMenu queryMenu = new JMenu("Queries");
            JMenuItem q1 = new JMenuItem("1. Members of a club");
            q1.addActionListener(e -> selectQuery(1));
            JMenuItem q4 = new JMenuItem("4. Students in every club that meets on a day");
            q4.addActionListener(e -> selectQuery(4));
            queryMenu.add(q1);
            queryMenu.add(q4);

            JMenuBar bar = new JMenuBar();
            bar.add(queryMenu);
            setJMenuBar(bar);
        }

        private void buildLayout() {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15f));

            inputBox.setEditable(true);
            inputBox.setPreferredSize(new Dimension(260, 28));
            JButton runButton = new JButton("Run Query");
            runButton.addActionListener(e -> runQuery());

            JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            inputRow.add(inputLabel);
            inputRow.add(inputBox);
            inputRow.add(runButton);

            JPanel top = new JPanel(new BorderLayout(0, 8));
            top.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
            top.add(titleLabel, BorderLayout.NORTH);
            top.add(inputRow, BorderLayout.CENTER);

            table.setFillsViewportHeight(true);
            table.setRowHeight(24);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));
            bottom.add(statusLabel, BorderLayout.CENTER);

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        // Switches the screen to the chosen query and fills the input list from the database.
        private void selectQuery(int number) {
            currentQuery = number;
            table.setModel(new DefaultTableModel());
            showStatus(" ", false);
            try {
                if (number == 1) {
                    titleLabel.setText("Query 1: Members of a club");
                    inputLabel.setText("Club name:");
                    fillInputBox("SELECT ClubName FROM Club ORDER BY ClubName");
                } else {
                    titleLabel.setText("Query 4: Students who belong to every club that meets on a day");
                    inputLabel.setText("Meeting day:");
                    fillInputBox("SELECT DISTINCT MeetingDay FROM Club ORDER BY MeetingDay");
                }
            } catch (SQLException ex) {
                showStatus("Database error: " + ex.getMessage(), true);
            }
        }

        private void fillInputBox(String sql) throws SQLException {
            inputBox.removeAllItems();
            try (Statement s = Db.get().createStatement(); ResultSet r = s.executeQuery(sql)) {
                while (r.next()) {
                    inputBox.addItem(r.getString(1));
                }
            }
        }

        private void runQuery() {
            Object typed = inputBox.getEditor().getItem();
            String value = (typed == null) ? "" : typed.toString().trim();
            if (value.isEmpty()) {
                showStatus("Please type or pick a value first.", true);
                return;
            }

            String sql = (currentQuery == 1) ? QUERY_1 : QUERY_4;
            try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
                ps.setString(1, value);
                if (currentQuery == 4) {
                    ps.setString(2, value);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    DefaultTableModel model = buildModel(rs);
                    table.setModel(model);
                    int rows = model.getRowCount();
                    if (rows == 0) {
                        showStatus("No rows found for \"" + value + "\".", true);
                    } else {
                        showStatus(rows + (rows == 1 ? " row" : " rows") + " returned.", false);
                    }
                }
            } catch (SQLException ex) {
                showStatus("Database error: " + ex.getMessage(), true);
            }
        }

        // Turns any query result into a table model, so every query can reuse it.
        private static DefaultTableModel buildModel(ResultSet rs) throws SQLException {
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= columns; i++) {
                names.add(meta.getColumnLabel(i));
            }
            DefaultTableModel model = new DefaultTableModel(names.toArray(), 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            while (rs.next()) {
                Object[] row = new Object[columns];
                for (int i = 0; i < columns; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                model.addRow(row);
            }
            return model;
        }

        private void showStatus(String message, boolean isError) {
            statusLabel.setForeground(isError ? Color.RED : new Color(0, 120, 0));
            statusLabel.setText(message);
        }
    }
}
