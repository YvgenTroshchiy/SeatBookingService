# Seat Booking Service

A backend REST API for booking numbered seats at events — concerts, cinema screenings, flights.
Built to demonstrate production-grade backend thinking: clean domain model, correct concurrency
under contention, versioned migrations, security, tests, and a containerized deliverable.

## Stack

Java 21 · Spring Boot 4 · PostgreSQL · Flyway · Spring Security (JWT) · Testcontainers · Docker · GitHub Actions

## Domain model

One generalized model covers all three use cases:

- `Venue` — physical place with a seating layout
- `Seat` — a seat in a venue (section, row, number)
- `Event` — a scheduled happening at a venue (the concert / screening / flight)
- `EventSeat` — state of a seat *for a specific event*; availability lives here, not on `Seat`
- `Booking` — a user's reservation of one or more `EventSeat`s
- `User` — account + roles

## Booking flow

Hold → Confirm, Ticketmaster-style:

1. Client holds seats → status `HELD` with a 5-minute TTL
2. Client confirms within the window → `BOOKED`; hold expires → seats free up automatically

Concurrency is handled with optimistic locking (`@Version` on `EventSeat`) and a DB-level
`UNIQUE(event_id, seat_id)` constraint as a backstop.

## Running locally

```bash
docker compose up        # starts app + Postgres
```

API docs available at `http://localhost:8080/swagger-ui.html` once the service is up.

## Running tests

```bash
./mvnw verify            # unit + integration tests (Testcontainers spins up Postgres)
```

## Project structure

```
src/main/java/com/troshchii/booking
├── venue       — Venue, seating layout
├── event       — Event, EventSeat, availability
├── booking     — hold/confirm flow, locking
├── user        — User, registration, roles
├── security    — JWT filter, token service
└── common      — global exception handler, config
```

## Design notes

See [`docs/design.md`](docs/design.md) for full rationale behind every decision.
