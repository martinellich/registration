# Use Case: Prepare Invitation

## Overview

**Use Case ID:** UC-001
**Use Case Name:** Prepare Invitation
**Primary Actor:** User
**Goal:** Define an invitation for a year — which persons are invited to which events, when registration is open, and how the invitation and confirmation emails read — so that a mailing can later be sent out.
**Status:** Implemented

## Preconditions

- The user is signed in.
- The events of the year exist (UC-005) and the persons to invite are recorded and active (UC-006).

## Main Success Scenario

1. The user opens the invitation list; the system shows the current invitations with title, year, open period, and whether a mailing was created and sent.
2. The user creates a new invitation or selects an existing one.
3. The user enters the title, the year, and the period during which registration is open, and optionally remarks that will be shown to invitees.
4. The user writes the invitation email text and, optionally, the subjects and texts of the confirmation emails for new and for updated registrations.
5. The system offers the events taking place in the chosen year and all active persons.
6. The user selects the events and the persons the invitation covers.
7. The user saves the invitation.
8. The system stores the invitation with its selections and confirms the save.

## Alternative Flows

### A1: Required information missing

**Trigger:** Title, year, or the open period is not filled in when saving (step 7)
**Flow:**

1. The system marks the missing fields and does not save.
2. The user completes the missing information.
3. Use case continues at step 7.

### A2: User changes the year

**Trigger:** The user changes the year of the invitation (step 3)
**Flow:**

1. The system replaces the offered events with those of the new year and clears the current event selection.
2. Use case continues at step 6.

### A3: User deletes an invitation

**Trigger:** The user chooses to delete an invitation from the list (step 2)
**Flow:**

1. The system asks for confirmation.
2. The user confirms the deletion.
3. The system deletes the invitation together with its mailings and event registrations and confirms the deletion.
4. Use case ends.

## Postconditions

### Success Postconditions

- The invitation is stored with title, year, open period, remarks, email texts, and its selected events and persons.

### Failure Postconditions

- The invitation and its selections remain unchanged.

## Business Rules

### BR-001: Mandatory invitation data

Title, year, and both dates of the open period are required to save an invitation.

### BR-002: Events are limited to the invitation year

Only events taking place within the invitation's year can be selected.

### BR-003: Only active persons can be invited

The person selection offers only persons marked as active.

### BR-004: Past invitations are hidden by default

The invitation list hides invitations of past years and invitations whose closing date has passed; the user can show them on demand.

### BR-005: Deleting an invitation removes its dependent data

Deleting an invitation also removes its mailings and the event registrations collected for it.

### BR-006: Confirmation email templates support placeholders

The confirmation email texts may contain the placeholders %PERSON_NAMES%, %EVENTS%, %LINK%, %OPEN_FROM%, %OPEN_UNTIL%, and %REMARKS%, which are replaced when the email is sent (see UC-003).
