# Anmeldetool - Event Registration System

A web-based event registration system designed for organizations that need to manage recurring event registrations with email-based access control.

## What This Application Does

This application solves the problem of collecting event registrations from a group of people in a structured, organized way. It's particularly suited for sports clubs, youth organizations, or community groups that run multiple events throughout the year.

### Business Scenario

**Example:** A youth sports club (TV Erlach - Jugi) runs various training sessions and events throughout the year. Every season, they need to know which members want to participate in which events.

Instead of managing spreadsheets, phone calls, or paper forms, administrators use this system to:
1. Send personalized registration invitations via email
2. Collect registrations through a simple web form
3. Track who has registered for which events
4. Export registration data for planning purposes

## Core Features

### 1. Person Management
- Maintain a database of people (members, participants, families)
- Store contact information (name, email, date of birth)
- Mark people as active/inactive
- Persons are the recipients of registration invitations

### 2. Event Management
- Create and manage events with details:
  - Title and description
  - Location
  - Date range (from/to dates)
- Events are what people register for (e.g., "Summer Camp", "Saturday Training", "Tournament")

### 3. Registration Periods
- Define registration periods by year with open/close dates
- Select which events are available in this registration period
- Select which persons should receive registration invitations
- Add custom email text with registration instructions
- Track email creation and sending status

### 4. Email Invitation System (Magic Links)
- Generate unique, secure links for each person/email address
- No passwords required - recipients access via their personal link
- Email template includes custom text and the unique registration link
- Track which emails have been sent and when
- Resend capability for failed deliveries

### 5. Public Registration Interface
- Recipients click their unique link and see their personal registration form
- View all available events with descriptions, dates, and locations
- Check/uncheck events they want to register for
- Submit registrations (can be modified by clicking the link again)
- Track registration timestamp

### 6. Registration Tracking & Reporting
- View all registrations in a matrix format (persons × events)
- See who has registered for which events
- Track email delivery status
- Export registration data to Excel spreadsheet
- View registration statistics (emails created/sent)

### 7. Security & Access Control
- Admin access via Azure Active Directory (OAuth2)
- Role-based access (USER and ADMIN roles)
- Public registration views accessible only via valid magic links
- No authentication required for registration submission

## User Workflows

### Administrator Workflow

1. **Setup Phase**
   - Maintain list of persons (add/edit/deactivate members)
   - Create events for the period (e.g., all training sessions for spring 2025)

2. **Registration Period Creation**
   - Create new registration (e.g., "Spring 2025")
   - Set open/close dates (e.g., open from Jan 1 to Feb 28)
   - Select which events are available for registration
   - Select which persons should receive invitations
   - Write custom email text with instructions

3. **Email Campaign**
   - Click "Create Mailing" - generates unique links for each person
   - Review email list
   - Click "Send Emails" - sends all pending invitation emails
   - Monitor delivery status

4. **Registration Tracking**
   - View registration matrix (who registered for what)
   - Export to Excel for planning and logistics
   - Check registration status (who hasn't responded yet)

### Recipient Workflow

1. **Receive Email**
   - Get personalized email with registration instructions
   - Email contains unique link valid only for this person

2. **Register for Events**
   - Click the link in email
   - See list of available events with all details
   - Check boxes for events they want to attend
   - Submit registration

3. **Modify Registration**
   - Click the same link again to modify selections
   - Change event selections up until registration closes
   - Submit updated registration

## Technical Details

- **Framework**: Spring Boot + Vaadin (Java full-stack)
- **Database**: PostgreSQL with jOOQ for type-safe SQL
- **Authentication**: Azure Active Directory OAuth2
- **Email**: SMTP (Gmail) with async sending
- **Export**: Apache POI for Excel generation

## Use Cases

This application is ideal for:

- **Sports Clubs**: Collect registrations for training sessions, camps, tournaments
- **Youth Organizations**: Manage event attendance for recurring programs
- **Community Groups**: Coordinate participation in group activities
- **Educational Programs**: Track enrollment in workshops or classes
- **Volunteer Organizations**: Manage volunteer sign-ups for different shifts/events

## Key Benefits

1. **Email-Based Access**: No user accounts or passwords to manage - recipients just click their link
2. **Reusable Links**: Recipients can modify their registration anytime by clicking the same link
3. **Centralized Management**: All persons, events, and registrations in one place
4. **Audit Trail**: Track when emails were sent and when registrations were submitted
5. **Excel Export**: Easy data export for offline planning and analysis
6. **Secure**: Magic links are unique UUIDs - difficult to guess
7. **Responsive**: Works on desktop, tablet, and mobile devices

## Configuration

The application is configured for a specific organization via `application.properties`:

```properties
registration.public.address=https://anmeldungen.tverlach.ch
registration.title=Jugi TV Erlach - Anmeldungen
```

This makes the system customizable for different organizations while using the same codebase.

## Development & Deployment

See [CLAUDE.md](CLAUDE.md) for detailed technical documentation including build commands, architecture, and development guidelines.

## License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.
