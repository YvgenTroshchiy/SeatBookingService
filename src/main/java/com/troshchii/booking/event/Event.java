package com.troshchii.booking.event;

import com.troshchii.booking.venue.Venue;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private String title;

    @Convert(converter = EventTypeConverter.class)
    @Column(nullable = false)
    private EventType type;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
}
