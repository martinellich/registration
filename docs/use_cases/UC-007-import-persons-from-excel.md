# Use Case: Import Persons from Excel

## Overview

**Use Case ID:** UC-007
**Use Case Name:** Import Persons from Excel
**Primary Actor:** User
**Goal:** Synchronize the person register with the club's membership list by uploading an Excel workbook and selectively applying the detected differences.
**Status:** Implemented

## Preconditions

- The user is signed in.
- A membership list is available as an Excel workbook with the expected columns (member number, first name, last name, email, alternative email).

## Main Success Scenario

1. The user opens the upload dialog from the person list.
2. The user uploads the membership workbook.
3. The system reads member number, first name, last name, and email address from each row, accepting the columns in any order and falling back to the alternative email column when the main one is empty.
4. The system compares the rows with the stored persons, matching by member number first and by first and last name otherwise.
5. The system presents the proposed changes — new persons, updated persons, and persons to deactivate — with old and new values, all accepted by default.
6. The user reviews the proposals and accepts or rejects them individually or all at once.
7. The user applies the changes.
8. The system creates, updates, and deactivates persons according to the accepted proposals and reports how many changes were applied.

## Alternative Flows

### A1: File contains no usable rows

**Trigger:** No person rows can be read from the uploaded file (step 3)
**Flow:**

1. The system shows an error message and imports nothing.
2. Use case ends.

### A2: Required columns missing

**Trigger:** The first-name or last-name column is not present in the file (step 3)
**Flow:**

1. The system rejects the file and imports nothing.
2. Use case ends.

### A3: No differences found

**Trigger:** The file matches the stored persons (step 4)
**Flow:**

1. The system reports that there is nothing to import.
2. Use case ends.

### A4: User cancels the review

**Trigger:** The user closes the review without applying (step 6)
**Flow:**

1. The system changes nothing.
2. Use case ends.

## Postconditions

### Success Postconditions

- The accepted proposals are applied: new persons are created as active, changed persons are updated, and persons missing from the list are deactivated.

### Failure Postconditions

- The person register remains unchanged.

## Business Rules

### BR-001: Upload restrictions

Only a single Excel workbook of at most 10 MB can be uploaded per import.

### BR-002: Required columns

First name and last name are required columns; rows without both names are skipped.

### BR-003: Email fallback

The email address is taken from the main email column; when it is empty, the alternative email column is used.

### BR-004: Matching order

A row is matched to a stored person by member number first; without a match, by first and last name.

### BR-005: Missing persons are deactivated, never deleted

Active persons that do not appear in the uploaded list are proposed for deactivation, never for deletion.

### BR-006: Only accepted proposals are applied

Rejected proposals are ignored; newly created persons are active.
