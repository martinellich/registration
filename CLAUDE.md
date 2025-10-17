# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5 + Vaadin 24 event registration application called "Anmeldetool" (Registration Tool). It manages persons, events, and event registrations with email notifications. The application uses jOOQ for database access, PostgreSQL for persistence, and Azure Active Directory for authentication.

## Technology Stack

- **Backend**: Spring Boot 3.5.5, Java 21
- **Frontend**: Vaadin 24.8 (Java-based UI framework)
- **Database**: PostgreSQL 16.1 with jOOQ 3.20 for type-safe SQL
- **Security**: Spring Security with Azure AD OAuth2
- **Database Migration**: Flyway
- **Testing**: JUnit 5, Testcontainers, Karibu Testing (Vaadin UI tests)
- **Build**: Maven

## Build and Run Commands

### Development
```bash
# Run application in development mode (default goal)
./mvnw

# Or explicitly:
./mvnw spring-boot:run

# Run with PostgreSQL via Testcontainers (recommended for local dev)
./mvnw spring-boot:test-run
```

### Testing
```bash
# Run all unit tests
./mvnw test

# Run integration tests
./mvnw verify -Pit

# Run tests with coverage report
./mvnw clean verify -Pcoverage
# Coverage report: target/site/jacoco/index.html

# Run single test class
./mvnw test -Dtest=RegistrationViewTest

# Run single test method
./mvnw test -Dtest=RegistrationViewTest#testMethodName
```

### Code Quality
```bash
# Check code formatting (Spring Java Format)
./mvnw spring-javaformat:validate

# Auto-format code
./mvnw spring-javaformat:apply
```

### Production Build
```bash
# Create production JAR with optimized frontend bundle
./mvnw clean package -Pproduction

# Run production JAR
java -jar target/registration-1.10.15-SNAPSHOT.jar
```

### jOOQ Code Generation
```bash
# Regenerate jOOQ classes from database schema
# This happens automatically during build, but can be run manually:
./mvnw generate-sources

# Note: Uses testcontainers-jooq-codegen-maven-plugin which:
# 1. Starts PostgreSQL container
# 2. Runs Flyway migrations
# 3. Generates jOOQ code from actual schema
# Generated code location: target/generated-sources/jooq/
```

## Architecture

### Package Structure

```
ch.martinelli.oss.registration/
├── RegistrationApplication.java      # Main Spring Boot entry point
├── domain/                            # Business logic layer
│   ├── *Repository.java              # jOOQ repositories (data access)
│   ├── *Service.java                 # Business services
│   └── EmailSender.java              # Email notification service
├── security/                          # Security configuration
│   ├── SecurityConfiguration.java    # Spring Security + Azure AD config
│   ├── SecurityContext.java          # Security helper utilities
│   └── Roles.java                    # Role constants
└── ui/                                # Vaadin UI layer
    ├── views/                         # View components
    │   ├── MainLayout.java           # App shell with navigation
    │   ├── EditView.java             # Base class for CRUD views
    │   ├── events/                    # Event management views
    │   ├── persons/                   # Person management views
    │   └── registration/              # Registration views (admin + public)
    ├── components/                    # Reusable UI components
    └── translation/                   # I18n support
```

### Data Layer (jOOQ)

The application uses jOOQ for type-safe database access instead of JPA. Key patterns:

- **Repository classes** extend `SimpleFilterDslRepository<Record, POJO>` from `vaadin-jooq` library
- **Generated code** in `target/generated-sources/jooq/ch/martinelli/oss/registration/db/`
- **Database schema** defined in `src/main/resources/db/migration/V*.sql` (Flyway)
- **Custom generator** `EqualsAndHashCodeJavaGenerator` adds equals/hashCode to POJOs

Core entities:
- `person` - Individuals who can register for events
- `event` - Events that can be registered for
- `registration` - Registration periods (year, open dates)
- `registration_email` - Email invitations with magic links
- `registration_email_person` - Many-to-many: emails ↔ persons
- `event_registration` - Many-to-many: persons ↔ events

### UI Layer (Vaadin)

- **Server-side rendering**: All UI is Java code, no JavaScript required
- **Routing**: `@Route` annotations define URL paths
- **Security**: `@PermitAll` / `@RolesAllowed` on views
- **Layout**: `MainLayout` provides navigation sidebar
- **Testing**: Karibu Testing library for server-side UI tests

View types:
- Admin views: `EventView`, `PersonView`, `RegistrationView`, etc.
- Public view: `PublicEventRegistrationView` (accessible via magic link)

### Security

- **Authentication**: Azure Active Directory OAuth2
- **Authorization**: Role-based (`Roles.ADMIN`, `Roles.USER`)
- **Public access**: Health endpoint and public registration view
- **Config**: `SecurityConfiguration` extends `VaadinWebSecurity`

### Email System

Registration workflow:
1. Admin creates registration period and selects persons
2. System generates unique links for each person
3. `EmailSender` sends emails asynchronously (`@Async`)
4. Recipients access public registration form via magic link
5. They select events to register for
6. Confirmations sent via email

## Testing

### Test Infrastructure

- **Testcontainers**: PostgreSQL container auto-started for tests
- **Configuration**: `TestcontainersConfiguration` provides `@ServiceConnection`
- **UI Tests**: Karibu Testing simulates Vaadin UI without browser
- **Mail Tests**: Mailcatcher container for testing email sending

### Test Conventions

- Unit tests: `*Test.java`
- Integration tests: `*IT.java` (run with `-Pit` profile)
- UI tests use `MockVaadin.setup()` / `MockVaadin.tearDown()`
- Spring Boot test slices: `@SpringBootTest` with Testcontainers

## Configuration

### Application Properties

Key configuration in `src/main/resources/application.properties`:

- `server.port` - Default 8080, overridable via `PORT` env var
- `vaadin.launch-browser` - Auto-open browser in dev mode
- `spring.mail.*` - SMTP configuration for Gmail
- `spring.cloud.azure.active-directory.*` - Azure AD OAuth2
- `registration.public.address` - Base URL for magic links
- `registration.title` - Application title

### Environment-Specific Config

- Development: Uses Testcontainers PostgreSQL
- Production: Configure via environment variables or external `application.properties`

## Database Migrations

Flyway migrations in `src/main/resources/db/migration/`:
- Versioned format: `V001__description.sql`, `V002__description.sql`, etc.
- Applied automatically on application startup
- After adding migration, regenerate jOOQ code: `./mvnw generate-sources`

## Deployment

### Docker
```bash
docker build -t registration .
docker run -p 8080:8080 registration
```

### Jelastic (Production)
```bash
./mvnw jelastic:deploy -Djelastic.username=... -Djelastic.password=...
```

## Development Workflow

1. **Add new feature**: Modify domain logic → update migrations if needed → regenerate jOOQ
2. **UI changes**: Modify Vaadin views → hot-reload via Spring DevTools
3. **Code formatting**: Run `./mvnw spring-javaformat:apply` before commit
4. **Testing**: Write Karibu tests for UI, standard JUnit for services
5. **Git workflow**: Using gitflow (develop → main branches)

## Important Notes

- **No manual SQL strings**: Use jOOQ's type-safe DSL
- **No JPA annotations**: This is a jOOQ project, not JPA
- **Frontend bundling**: `src/main/bundles/dev.bundle` is version-controlled (pre-compiled frontend)
- **Code style**: Uses Spring Java Format (runs on validate phase)
- **Azure AD required**: App won't start without valid Azure AD config (or disable in properties)