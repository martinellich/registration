# Use Case: Track Event Registrations

## Overview

**Use Case ID:** UC-004
**Use Case Name:** Track Event Registrations
**Primary Actor:** User
**Goal:** See who has registered for which events of an invitation, including totals per event, and take the overview away as a spreadsheet.
**Status:** Implemented

## Preconditions

- The user is signed in.
- An invitation exists for which invitees have registered (UC-003).

## Main Success Scenario

1. The user opens the event registrations overview, either directly or from an invitation in the invitation list.
2. The user selects an invitation.
3. The system shows a matrix with one row per registered person and one column per event, marking each participation, with a total of participants per event.
4. The user requests the export.
5. The system provides the matrix as a spreadsheet for download.

## Alternative Flows

### A1: No registrations yet

**Trigger:** No participation choices have been submitted for the selected invitation (step 3)
**Flow:**

1. The system shows a notice that there are no registrations.
2. Use case ends.

## Postconditions

### Success Postconditions

- The user has seen the current registration matrix and, if requested, downloaded it as a spreadsheet.
- The stored data is unchanged.

### Failure Postconditions

- The stored data is unchanged.

## Business Rules

### BR-001: Matrix layout

The matrix contains one row per person with registrations for the invitation, sorted by name, and one column per event of the invitation, sorted by title.

### BR-002: Totals per event

Each event column shows the number of registered participants as its total.

### BR-003: Export marks participation

In the exported spreadsheet a participation is marked with "X"; a declined or missing participation is left blank.
