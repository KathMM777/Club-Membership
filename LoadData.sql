-- Loads the CSV files in this repo into ClubMembership.
-- 1) Run ClubMembership.sql first (it creates the tables).
-- 2) Change /Users/chriskiooo/Documents/Club-Membership to the repo folder on YOUR computer.
-- 3) Local loading must be on: SET GLOBAL local_infile = 1; and start MySQL with --local-infile=1
-- Order matters: Students and Clubs first, Memberships last (it has the foreign keys).
USE ClubMembership;

LOAD DATA LOCAL INFILE '/Users/chriskiooo/Documents/Club-Membership/Students.csv'
INTO TABLE Student
FIELDS TERMINATED BY ','
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

LOAD DATA LOCAL INFILE '/Users/chriskiooo/Documents/Club-Membership/Clubs.csv'
INTO TABLE Club
FIELDS TERMINATED BY ','
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

LOAD DATA LOCAL INFILE '/Users/chriskiooo/Documents/Club-Membership/Memberships.csv'
INTO TABLE Membership
FIELDS TERMINATED BY ','
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

SELECT 'Student' AS TableName, COUNT(*) AS Rows_Loaded FROM Student
UNION ALL SELECT 'Club', COUNT(*) FROM Club
UNION ALL SELECT 'Membership', COUNT(*) FROM Membership;
