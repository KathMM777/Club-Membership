USE ClubMembership;

-- Query 1: Clubs a student belongs to (sample student: Emma Johnson, ID 1001)
SELECT Club.ClubName, Club.MeetingDay, Membership.Position, Membership.JoinDate
FROM Student
JOIN Membership ON Student.StudentID = Membership.StudentID
JOIN Club ON Membership.ClubID = Club.ClubID
WHERE Student.StudentID = 1001;



-- Query 4: Students in every club that meets on a given day (sample day: Wednesday)
SELECT Student.FirstName, Student.LastName FROM Student
WHERE NOT EXISTS (
    SELECT * FROM Club
    WHERE Club.MeetingDay = 'Wednesday'
    AND NOT EXISTS (
        SELECT * FROM Membership
        WHERE Membership.StudentID = Student.StudentID
        AND Membership.ClubID = Club.ClubID
    )
);