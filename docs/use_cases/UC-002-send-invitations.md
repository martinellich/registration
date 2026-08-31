# Use Case: Send Invitations

## Overview

**Use Case ID:** UC-002
**Use Case Name:** Send Invitations
**Primary Actor:** User
**Goal:** Create the mailing for an invitation and send each invited household an email with its personal registration link, so invitees can register for the events.
**Status:** Implemented

## Preconditions

- The user is signed in.
- The invitation is saved with at least one event and one person selected (UC-001) and has no unsaved changes.

## Main Success Scenario

1. The user opens the invitation and requests the mailing to be created.
2. The system asks for confirmation and the user confirms.
3. The system creates one mailing entry per distinct email address of the invited persons, assigns each entry its own personal registration link, and attaches the persons sharing that address.
4. The system confirms that the mailing was created.
5. The user requests the invitation emails to be sent and confirms the prompt.
6. The system sends an invitation email containing the personal link to every mailing entry that has not been sent yet, marks each entry with the sending time, and completes the sending in the background.
7. The user reviews the mailing list, which shows for each address when the email was sent and when the recipient last registered.

## Alternative Flows

### A1: Mailing already exists

**Trigger:** A mailing was already created for the invitation when the user creates it again (step 3)
**Flow:**

1. The system keeps the existing entries and their personal links and adds entries only for newly invited persons or addresses.
2. Use case continues at step 4.

### A2: An invitation email cannot be delivered

**Trigger:** Sending fails for a mailing entry (step 6)
**Flow:**

1. The system records the problem, leaves the entry marked as not sent, and continues with the remaining entries.
2. The user can trigger sending again later; only unsent entries are sent.
3. Use case continues at step 7.

### A3: User removes a mailing entry

**Trigger:** The user deletes an entry from the mailing list (step 7)
**Flow:**

1. The system asks for confirmation and the user confirms.
2. The system removes the mailing entry and confirms the deletion.
3. Use case ends.

## Postconditions

### Success Postconditions

- Every invited email address has a mailing entry with a personal registration link.
- The invitation emails are sent and each entry carries its sending time.

### Failure Postconditions

- Entries whose email could not be sent remain marked as unsent and can be sent again.

## Business Rules

### BR-001: Mailing requires a complete, saved invitation

The mailing can only be created when the invitation is saved with at least one event and one person selected and there are no pending edits.

### BR-002: One mailing entry per email address

Persons sharing an email address are grouped into a single mailing entry; an invitation has at most one entry per address.

### BR-003: Every entry gets a unique personal link

Each mailing entry receives its own personal link that grants access to the registration form (UC-003).

### BR-004: Emails are sent only once per entry

Sending covers only entries not sent before; repeating the send never produces duplicate emails.

### BR-005: Invitation email content

The invitation email text of the invitation must be present; the subject combines the invitation title and year, and replies go to the user who triggered the sending.

### BR-006: Mail sending can be disabled per environment

On non-production environments mail sending can be switched off; messages are then only recorded instead of delivered.
