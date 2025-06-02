package com.tripplanner.repository;

import com.tripplanner.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    Optional<Itinerary> findByShareableLink(String shareableLink);
}
