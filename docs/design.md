# Seat Booking Service — Design & Build Plan

A backend service for booking numbered seats for **events** — where an event is a concert,
a cinema screening, or a flight. The whole point of the project is to show production-grade
backend thinking: a clean domain model, correct concurrency under contention, migrations,
security, tests, and a containerized, CI-built deliverable.

> This is a portfolio project. Every decision below is made to **demonstrate competence and
> de-risk a hire**, not to be the most feature-rich app possible. Scope is deliberately tight.

---

## 1. Locked decisions

| Thing            | Choice                          | Why |
|------------------|---------------------------------|-----|
| Repo name        | `seat-booking-service`          | Reads as a backend service, not an "app" |
| Base package     | `com.troshchii.booking`         | Reverse-domain, surname over first name |
| Language         | Java 21 (Amazon Corretto)       | LTS, virtual threads (Loom) already final |
| Build tool       | Maven                           | Closest to enterprise Java reality |
| Framework        | Spring Boot 4 (Spring Framework 7) | Current baseline, first-class Java 21/25 |
| Package layout   | package-by-feature              | Groups by business domain, scales better |

---

## 2. Core insight: one abstraction for all three

Concert, cinema, and flight are **the same thing**: a scheduled event at a place that has a
fixed grid of seats, where each seat can be booked by exactly one person for that event.

Do **not** build three separate modules. Build one generalized model and make
"concert / cinema / flight" just a *type* of event. That single design decision is worth more
in an interview than the booking flow itself.

- `Venue` — the physical place with a seating layout (hall, screening room, aircraft).
- `Seat` — a physical seat in a venue (section, row, number, seat class).
- `Event` — a scheduled happening at a venue at a time (the concert/screening/flight).
- `EventSeat` — **the key table**: the state of a given seat *for a given event*.
  A seat is free for one show and taken for another, so availability lives here, not on `Seat`.
- `Booking` — a user's reservation of one or more `EventSeat`s, with a lifecycle status.
- `User` — account + roles.

```
Venue 1───* Seat
Venue 1───* Event
Event 1───* EventSeat *───1 Seat
Booking 1───* EventSeat
User 1───* Booking
```

`EventSeat.status`: `AVAILABLE → HELD → BOOKED` (and back to `AVAILABLE` on expiry/cancel).

---

## 3. The hard part: no double-booking under contention

This is the section that makes or breaks the project. Two users selecting the same seat for the
same event at the same instant must result in **exactly one winner**.

### Concurrency strategy

**Redis as the gatekeeper (primary mechanism)**

```
SET seat:{eventId}:{seatId} {userId} NX EX 300
```

- `NX` — set only if key does not exist: atomic, no race window.
- `EX 300` — TTL of 5 minutes; hold expires automatically, no sweep job needed.
- Only one thread wins; everyone else gets 409 immediately, before touching Postgres.

**DB unique constraint as the backstop**

- `UNIQUE(event_id, seat_id)` on the bookings table.
- Even if Redis state is somehow inconsistent, the database refuses a duplicate insert.
- Belt-and-suspenders: two independent layers, neither trusts the other.

**Idempotency key on confirm**

- Client sends an `Idempotency-Key` header on `POST /bookings`.
- Double-click or network retry returns the same booking instead of creating a duplicate charge.

### The interesting edge case

What if the Redis TTL expires at the exact moment the user sends confirm?

The confirm handler must re-verify the hold inside the DB transaction:
1. Check `EventSeat.status = HELD` and `held_until > now()`.
2. If expired → 409, do not insert booking.
3. If valid → insert booking, flip `EventSeat.status = BOOKED`, commit.

Redis says "who got the hold"; Postgres is the source of truth for durable state.
If they disagree, Postgres wins.

---

## 4. Hold expiry

Redis TTL handles expiry automatically — when the key disappears, the seat is free again for
new hold attempts. No sweep job needed for the Redis side.

Postgres still needs to know a hold expired (for the confirm-time check). Two options:
- **Lazy expiry** — on confirm, check `held_until > now()`. If not, reject. Simple and correct.
- **Scheduled sweep** — a `@Scheduled` job that flips stale `HELD` rows back to `AVAILABLE`.

Lazy is enough for correctness; the sweep keeps the table tidy and is a good talking point
about eventual vs read-time consistency.

---

## 5. Tech stack

| Concern        | Choice |
|----------------|--------|
| Web / REST     | Spring Web (MVC) |
| Persistence    | Spring Data JPA + Hibernate |
| Database       | PostgreSQL |
| Cache / holds  | Redis (`SET NX EX` for atomic hold, Spring Data Redis) |
| Migrations     | Flyway (SQL-first, readable, versioned) |
| Auth           | Spring Security + JWT (stateless) |
| Validation     | Jakarta Bean Validation |
| API docs       | springdoc-openapi (Swagger UI) |
| DTO mapping    | MapStruct (optional, keeps entities out of the API) |
| Tests          | JUnit 5 + AssertJ + Testcontainers |
| Health/metrics | Spring Boot Actuator |
| Container      | Docker + docker-compose (app + Postgres + Redis) |
| CI             | GitHub Actions |

---

## 6. Project structure (package-by-feature)

```
com.troshchii.booking
├── venue       (Venue, SeatLayout, controller/service/repository)
├── event       (Event, EventSeat, availability)
├── booking     (Booking, hold/confirm flow, locking logic)
├── user        (User, registration, roles)
├── security    (JWT filter, config, token service)
└── common      (config, global exception handler, error DTOs, base classes)
```

Each feature package holds its own controller / service / repository / entity / dto. No giant
shared `service` or `controller` package.

---

## 7. API surface (sketch)

```
# Auth
POST   /api/auth/register
POST   /api/auth/login                  -> JWT

# Catalog (public read)
GET    /api/events
GET    /api/events/{id}
GET    /api/events/{id}/seats           -> per-seat availability for this event

# Booking (authenticated)
POST   /api/events/{id}/holds           -> hold seats (returns hold + expiry)
POST   /api/bookings                    -> confirm a hold into a booking (idempotency key)
GET    /api/bookings/{id}
DELETE /api/bookings/{id}               -> cancel (frees seats)

# Admin (ROLE_ADMIN)
POST   /api/venues
POST   /api/events                      -> creates Event + generates EventSeats from layout
```

---

## 8. Database

- Schema lives in **Flyway migrations** (`src/main/resources/db/migration`), never auto-DDL.
- Set `spring.jpa.hibernate.ddl-auto=validate` so Hibernate checks the schema but never changes it.
- Key constraints: FK integrity, `UNIQUE(event_id, seat_id)` on active holds/bookings.
- Indexes on `(event_id, status)` for fast availability queries.

---

## 9. Security

- Stateless JWT: login returns a signed token; a filter validates it per request.
- Roles: `ROLE_USER` (book seats), `ROLE_ADMIN` (manage venues/events).
- Passwords hashed with BCrypt. No secrets in the repo — config via env vars.

---

## 10. Testing strategy

- **Unit** — booking rules, hold expiry, state transitions (fast, no Spring context).
- **Slice** — `@WebMvcTest` for controllers, `@DataJpaTest` for repositories.
- **Integration** — `@SpringBootTest` + **Testcontainers** against a real Postgres.
- **The concurrency test (do not skip this):** fire N parallel booking attempts at one seat and
  assert **exactly one** succeeds and the rest fail cleanly. This single test is the strongest
  proof in the whole project that you understand backend correctness.

---

## 11. Infrastructure

- **Dockerfile** — multi-stage build (Maven build stage → slim JRE runtime).
- **docker-compose.yml** — app + Postgres, one `docker compose up` to run everything.
- **GitHub Actions** — on push/PR: build, run tests (Testcontainers), report status badge in README.

---

## 12. Build roadmap

Build in this order so the project is runnable at every step:

1. **Skeleton** — Spring Boot boots, Postgres via docker-compose, Flyway baseline, Actuator health.
2. **Domain + schema** — entities + migrations; admin CRUD for venues and events.
3. **Read side** — event listing + per-event seat availability endpoint.
4. **Booking core** — hold → confirm flow with optimistic locking + unique constraint.
5. **Hold expiry** — lazy expiry on read and/or scheduled sweep.
6. **Auth** — Spring Security + JWT, USER/ADMIN roles.
7. **Hardening** — validation, global exception handler, OpenAPI docs.
8. **Tests** — unit + integration + the concurrency test.
9. **Ops** — Dockerfile, docker-compose, GitHub Actions CI.
10. **Polish** — README with run instructions, sample seed data.

---

## 13. Explicitly out of scope

Keep the project from ballooning. **Not** doing:
- Real payment integration (a mock/stub `PaymentService` is enough).
- Microservices / service mesh — one well-structured monolith is the right call here.
- Message queues / Kafka (a possible *stretch* via an outbox pattern, but not core).
- Any frontend — this is a backend service with an OpenAPI spec.

