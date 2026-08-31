# Use Case: Plan Events

## Overview

**Use Case ID:** UC-005
**Use Case Name:** Plan Events
**Primary Actor:** User
**Goal:** Keep the catalog of events up to date — title, location, dates, description, and whether attendance is mandatory — so invitations can offer them.
**Status:** Implemented

## Preconditions

- The user is signed in.

## Main Success Scenario

1. The user opens the event list; the system shows the current and future events sorted by title.
2. The user creates a new event or selects an existing one.
3. The user enters title, location, start date, and optionally an end date, a description, and whether the event is mandatory.
4. The user saves the event.
5. The system stores the event and confirms the save.

## Alternative Flows

### A1: Required information missing

**Trigger:** Title, location, or start date is not filled in when saving (step 4)
**Flow:**

1. The system marks the missing fields and does not save.
2. The user completes the missing information.
3. Use case continues at step 4.

### A2: User deletes an event

**Trigger:** The user chooses to delete an event from the list (step 2)
**Flow:**

1. The system asks for confirmation and the user confirms.
2. The system deletes the event and confirms the deletion.
3. Use case ends.

### A3: Event is still referenced

**Trigger:** The event to delete is used by an invitation or has registrations (step 2)
**Flow:**

1. The system rejects the deletion and shows an error notice.
2. Use case ends.

### A4: User shows past events

**Trigger:** The user wants to see events that already took place (step 1)
**Flow:**

1. The user switches the list to include past events.
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- The event is stored with its data, or removed if it was deleted.

### Failure Postconditions

- The event catalog remains unchanged.

## Business Rules

### BR-001: Mandatory event data

Title, location, and start date are required; end date and description are optional.

### BR-002: Single-day events

An event without an end date is a single-day event on its start date.

### BR-003: Past events are hidden by default

An event is past when its end date — or its start date, if it has no end date — lies before today; past events are hidden unless the user shows them.

### BR-004: Mandatory events

An event can be marked as mandatory; invited persons are then automatically registered for it and cannot decline (see UC-003 BR-003).

### BR-005: Referenced events cannot be deleted

An event that is part of an invitation or has registrations cannot be deleted.
