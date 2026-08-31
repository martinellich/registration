# Use Case: Maintain Person Register

## Overview

**Use Case ID:** UC-006
**Use Case Name:** Maintain Person Register
**Primary Actor:** User
**Goal:** Keep the register of persons who can be invited to events up to date, including whether they are active.
**Status:** Implemented

## Preconditions

- The user is signed in.

## Main Success Scenario

1. The user opens the person list; the system shows the active persons sorted by last and first name.
2. The user creates a new person or selects an existing one.
3. The user enters last name and first name, and optionally email address, date of birth, and whether the person is active.
4. The user saves the person.
5. The system stores the person and confirms the save.

## Alternative Flows

### A1: Required information missing

**Trigger:** Last name or first name is not filled in when saving (step 4)
**Flow:**

1. The system marks the missing fields and does not save.
2. The user completes the missing information.
3. Use case continues at step 4.

### A2: User deletes a person

**Trigger:** The user chooses to delete a person from the list (step 2)
**Flow:**

1. The system asks for confirmation and the user confirms.
2. The system deletes the person and confirms the deletion.
3. Use case ends.

### A3: Person is still referenced

**Trigger:** The person to delete is referenced by invitations or registrations (step 2)
**Flow:**

1. The system deactivates the person instead of deleting them and confirms the deactivation.
2. Use case ends.

### A4: User shows inactive persons

**Trigger:** The user wants to see inactive persons (step 1)
**Flow:**

1. The user switches the list to include inactive persons.
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- The person is stored with the entered data, removed, or deactivated.

### Failure Postconditions

- The person register remains unchanged.

## Business Rules

### BR-001: Mandatory person data

Last name and first name are required; email address and date of birth are optional.

### BR-002: New persons are active

A newly created person is active by default.

### BR-003: Referenced persons are deactivated, not deleted

A person referenced by invitations or registrations cannot be removed; the system deactivates them instead so history is preserved.

### BR-004: Inactive persons stay out of the way

Inactive persons are hidden from the list by default and cannot be selected for invitations (see UC-001 BR-003).
