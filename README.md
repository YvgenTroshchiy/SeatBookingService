# Seat Booking Service

A backend REST API for booking numbered seats at events — concerts, cinema screenings, flights.
Built to demonstrate production-grade backend thinking: clean domain model, correct concurrency
under contention, versioned migrations, security, tests, and a containerized deliverable.

## Stack

Java 21 · Spring Boot 4 · PostgreSQL · Redis · Flyway · Spring Security (JWT) · Testcontainers · Docker · GitHub Actions

## Domain model

One generalized model covers all three use cases — concert, cinema, flight are just an `Event` type.

```mermaid
erDiagram
    VENUE {
        uuid id
        string name
        string address
    }
    SEAT {
        uuid id
        uuid venue_id
        string section
        string row
        string number
    }
    EVENT {
        uuid id
        uuid venue_id
        string title
        string type
        timestamp starts_at
    }
    EVENT_SEAT {
        uuid id
        uuid event_id
        uuid seat_id
        uuid booking_id
        enum status
        timestamp held_until
    }
    BOOKING {
        uuid id
        uuid user_id
        string idempotency_key
        enum status
    }
    USER {
        uuid id
        string email
        string password_hash
        enum role
    }

    VENUE ||--o{ SEAT : "has"
    VENUE ||--o{ EVENT : "hosts"
    EVENT ||--o{ EVENT_SEAT : "generates"
    SEAT ||--o{ EVENT_SEAT : "assigned to"
    BOOKING ||--o{ EVENT_SEAT : "reserves"
    USER ||--o{ BOOKING : "makes"
```

`EventSeat.status`: `AVAILABLE → HELD → BOOKED` (and back to `AVAILABLE` on expiry or cancel).

## Booking flow

Hold → Confirm, Ticketmaster-style. Two users racing for the same seat — exactly one wins.

```mermaid
sequenceDiagram
    actor User
    participant API
    participant Redis
    participant DB

    User->>API: GET /events/{id}/seats
    API->>DB: query available EventSeats
    DB-->>API: seat list
    API-->>User: available seats

    User->>API: POST /events/{id}/holds
    API->>Redis: SET seat:{eventId}:{seatId} {userId} NX EX 300
    alt seat is free
        Redis-->>API: OK — hold acquired · TTL 5 min
        API->>DB: UPDATE EventSeat SET status=HELD, held_until=now+5min
        API-->>User: 200 holdId + expiresAt
    else seat already held
        Redis-->>API: nil
        API-->>User: 409 seat no longer available
    end

    User->>API: POST /bookings (Idempotency-Key header)
    API->>DB: verify hold still valid (status=HELD AND held_until > now)
    alt hold valid
        DB->>DB: INSERT booking · UPDATE status=BOOKED · COMMIT
        API->>Redis: DEL seat:{eventId}:{seatId}
        API-->>User: 201 Booking created
    else hold expired
        API-->>User: 409 hold expired
    end
```

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
