package com.troshchii.booking.event;

import com.troshchii.booking.booking.Booking;
import com.troshchii.booking.venue.Seat;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "event_seats",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_event_seat",
        columnNames = {"event_id", "seat_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class EventSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSeatStatus status = EventSeatStatus.AVAILABLE;

    @Column(name = "held_until")
    private Instant heldUntil;
}
