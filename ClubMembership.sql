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

CREATE TABLE Club (
    ClubID INT PRIMARY KEY, 
    ClubName VARCHAR(50),
    Description VARCHAR(255),
    MeetingDay VARCHAR(15),
    MeetingLocation VARCHAR(100)
    );



