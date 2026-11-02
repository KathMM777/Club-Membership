DROP DATABASE IF EXISTS ClubMembership;
CREATE DATABASE ClubMembership;
USE ClubMembership;

CREATE TABLE Student (
    StudentID INT PRIMARY KEY,
    Name VARCHAR(50) NOT NULL,
    Email VARCHAR(50) NOT NULL,
    Major VARCHAR(50) NOT NULL,
    ClassYear VARCHAR(50) NOT NULL
);



CREATE TABLE Membership (
    StudentID INT NOT NULL,
    ClubID INT NOT NULL,
    JoinDate DATE NOT NULL,
    Position VARCHAR(30) NOT NULL DEFAULT 'Member',
    PRIMARY KEY (StudentID, ClubID),
    FOREIGN KEY (StudentID) REFERENCES Student(StudentID),
    FOREIGN KEY (ClubID) REFERENCES Club(ClubID),
    INDEX idx_membership_club (ClubID),
    INDEX idx_membership_joindate (JoinDate)
);