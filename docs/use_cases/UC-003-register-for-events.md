# Use Case: Register for Events

## Overview

**Use Case ID:** UC-003
**Use Case Name:** Register for Events
**Primary Actor:** Invitee
**Goal:** Declare, via the personal link from the invitation email, which of the invited persons participate in which events, and receive a confirmation of the choices.
**Status:** Implemented

## Preconditions

- The invitee has received an invitation email containing their personal registration link (UC-002).

## Main Success Scenario

1. The invitee opens their personal link.
2. The system verifies the link and shows the registration form with the invitation's title, year, and remarks.
3. The system lists the persons covered by this invitation.
4. The system lists the invitation's events with date and description and, for each person, a participation choice pre-filled with the currently stored selections; mandatory events are shown pre-selected and locked.
5. The invitee chooses for each person and event whether the person participates.
6. The invitee submits the registration.
7. The system stores the choices, records the registration time, and sends a confirmation email summarizing the persons, their choices per event, the registration period, and the personal link.
8. The system shows the invitee that the registration was successful.

## Alternative Flows

### A1: Unknown link

**Trigger:** The personal link cannot be matched to any invitation (step 2)
**Flow:**

1. The system shows a "not found" page.
2. Use case ends.

### A2: Registration period closed

**Trigger:** The invitation's closing date is in the past (step 2)
**Flow:**

1. The system shows the form read-only with a notice that registration is closed.
2. Use case ends.

### A3: Nothing changed

**Trigger:** The submitted choices are identical to the stored ones (step 7)
**Flow:**

1. The system stores nothing, sends no confirmation, and tells the invitee that nothing changed.
2. Use case ends.

### A4: Updating an existing registration

**Trigger:** Choices were already submitted earlier via this link (step 6)
**Flow:**

1. The system updates the stored choices and sends the confirmation for an updated registration instead of the first-time confirmation.
2. Use case continues at step 8.

## Postconditions

### Success Postconditions

- The participation of every listed person in every event of the invitation is stored.
- The time of the (latest) registration is recorded on the mailing entry.
- A confirmation email was sent, if a confirmation template is configured.

### Failure Postconditions

- The stored registrations remain unchanged and no confirmation is sent.

## Business Rules

### BR-001: The personal link alone grants access

Opening the registration form requires only the personal link; no sign-in is needed.

### BR-002: Registration only while the period is open

Choices can be submitted or changed only until the invitation's closing date; afterwards the form is read-only.

### BR-003: Mandatory events cannot be declined

Events marked as mandatory (UC-005 BR-004) are pre-selected for every invited person and cannot be deselected.

### BR-004: Confirmation only on actual changes

A confirmation email is sent only when the submission changes the stored choices.

### BR-005: Different templates for first and updated registrations

The first submission uses the "new registration" template, later submissions the "updated registration" template; if no template is configured, no confirmation is sent.

### BR-006: Placeholder substitution

Placeholders in the confirmation template are replaced with the person names, the participation per event, the registration period, the remarks, and the personal link (see UC-001 BR-006).

### BR-007: Registration survives confirmation problems

If the confirmation email cannot be delivered, the stored registration remains valid.
