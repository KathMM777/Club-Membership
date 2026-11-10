
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClubManagment {

    // MySQL connection information
    private static final String URL =
            "jdbc:mysql://localhost:3306/clubmembership";

    private static final String USER = "root";

    // Test the database connection
    private static void testConnection() {

        String password = System.getenv("MYSQL_PASSWORD");

        if (password == null || password.isEmpty()) {
            System.out.println("MYSQL_PASSWORD is not configured.");
            return;
        }

        try (Connection conn =
                     DriverManager.getConnection(URL, USER, password)) {

            System.out.println("Connected to MySQL successfully!");

        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    // Insert a new club into MySQL
    private static void insertClub(
            int clubID,
            String clubName,
            String description,
            String meetingDay,
            String meetingLocation) {

        String sql = """
                INSERT INTO Club
                (ClubID, ClubName, Description, MeetingDay, MeetingLocation)
                VALUES (?, ?, ?, ?, ?)
                """;

        String password = System.getenv("MYSQL_PASSWORD");

        if (password == null || password.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "MYSQL_PASSWORD is not configured.");
            return;
        }

        try (Connection conn =
                     DriverManager.getConnection(URL, USER, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clubID);
            stmt.setString(2, clubName);
            stmt.setString(3, description);
            stmt.setString(4, meetingDay);
            stmt.setString(5, meetingLocation);

            int rows = stmt.executeUpdate();

            System.out.println(
                    "Club added successfully! Rows inserted: " + rows
            );

            JOptionPane.showMessageDialog(null,
                    "Club added successfully!");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error adding club: " + e.getMessage());
        }
    }

    // Query 1: Find students registered in a specific club
    private static void findStudentsByClub(int clubID) {

        String sql = """
                SELECT s.StudentID, s.Name, c.ClubName
                FROM Student s
                JOIN Membership m ON s.StudentID = m.StudentID
                JOIN Club c ON m.ClubID = c.ClubID
                WHERE c.ClubID = ?
                """;

        String password = System.getenv("MYSQL_PASSWORD");

        if (password == null || password.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "MYSQL_PASSWORD is not configured.");
            return;
        }

        try (Connection conn =
                     DriverManager.getConnection(URL, USER, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clubID);

            try (ResultSet rs = stmt.executeQuery()) {

                DefaultTableModel model = new DefaultTableModel(
                        new String[]{
                                "Student ID",
                                "Student Name",
                                "Club Name"
                        },
                        0
                );

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("StudentID"),
                            rs.getString("Name"),
                            rs.getString("ClubName")
                    });
                }

                showResults("Students in Club " + clubID, model);
            }

            System.out.println("JOIN query executed successfully!");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Query error: " + e.getMessage());
        }
    }

    // Query 2: Count students registered in each club
    private static void countStudentsByClub() {

        String sql = """
                SELECT c.ClubID, c.ClubName,
                       COUNT(m.StudentID) AS TotalStudents
                FROM Club c
                LEFT JOIN Membership m
                    ON c.ClubID = m.ClubID
                GROUP BY c.ClubID, c.ClubName
                ORDER BY c.ClubID
                """;

        String password = System.getenv("MYSQL_PASSWORD");

        if (password == null || password.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "MYSQL_PASSWORD is not configured.");
            return;
        }

        try (Connection conn =
                     DriverManager.getConnection(URL, USER, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            DefaultTableModel model = new DefaultTableModel(
                    new String[]{
                            "Club ID",
                            "Club Name",
                            "Total Students"
                    },
                    0
            );

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("ClubID"),
                        rs.getString("ClubName"),
                        rs.getInt("TotalStudents")
                });
            }

            showResults("Students per Club", model);

            System.out.println(
                    "GROUP BY query executed successfully!"
            );

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Query error: " + e.getMessage());
        }
    }

    // Display SQL results in a JTable
    private static void showResults(
            String title,
            DefaultTableModel model) {

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JFrame resultsFrame = new JFrame(title);
        resultsFrame.setSize(650, 350);
        resultsFrame.add(scrollPane);
        resultsFrame.setLocationRelativeTo(null);
        resultsFrame.setVisible(true);
    }

    public static void main(String[] args) {

        // Test MySQL connection
        testConnection();

        // Create the main window
        JFrame frame = new JFrame("Club Management System");
        frame.setSize(550, 450);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(8, 2, 10, 10));
        frame.add(panel, BorderLayout.NORTH);

        // Club ID
        JLabel clubIDLabel = new JLabel("Club ID:");
        panel.add(clubIDLabel);

        JTextField clubIDField = new JTextField(15);
        panel.add(clubIDField);

        // Club Name
        JLabel clubNameLabel = new JLabel("Club Name:");
        panel.add(clubNameLabel);

        JTextField clubNameField = new JTextField(15);
        panel.add(clubNameField);

        // Description
        JLabel descriptionLabel = new JLabel("Description:");
        panel.add(descriptionLabel);

        JTextField descriptionField = new JTextField(15);
        panel.add(descriptionField);

        // Meeting Day
        JLabel meetingDayLabel = new JLabel("Meeting Day:");
        panel.add(meetingDayLabel);

        JTextField meetingDayField = new JTextField(15);
        panel.add(meetingDayField);

        // Meeting Location
        JLabel meetingLocationLabel = new JLabel("Meeting Location:");
        panel.add(meetingLocationLabel);

        JTextField meetingLocationField = new JTextField(15);
        panel.add(meetingLocationField);

        // Add Club Button
        JButton addClubButton = new JButton("Add Club");
        panel.add(addClubButton);

        addClubButton.addActionListener(e -> {

            String clubID = clubIDField.getText();
            String clubName = clubNameField.getText().trim();
            String description = descriptionField.getText().trim();
            String meetingDay = meetingDayField.getText().trim();
            String meetingLocation = meetingLocationField.getText().trim();

            if (clubName.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Club Name cannot be empty.");
                return;
            }

            try {
                int id = Integer.parseInt(clubID.trim());

                insertClub(
                        id,
                        clubName,
                        description,
                        meetingDay,
                        meetingLocation
                );

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Club ID must be a number.");
            }
        });

        // Query 1 Button: Find Students by Club
        JButton searchButton =
                new JButton("Find Students by Club");

        panel.add(searchButton);

        searchButton.addActionListener(e -> {

            String input = JOptionPane.showInputDialog(
                    frame,
                    "Enter Club ID:"
            );

            if (input == null) {
                return;
            }

            try {
                int id = Integer.parseInt(input.trim());
                findStudentsByClub(id);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Please enter a valid Club ID.");
            }
        });

        // Query 2 Button: Count Students by Club
        JButton countButton =
                new JButton("Count Students by Club");

        panel.add(countButton);

        countButton.addActionListener(e -> {
            countStudentsByClub();
        });

        // Window settings
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
