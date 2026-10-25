package com.tripnova.tripnova.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tripnova.tripnova.Entity.Trip;

@Repository
public interface TripRepository extends JpaRepository< Trip  ,Long >  {

    List<Trip> findAllByUser_EmailIgnoreCaseOrderByIdDesc(
            String email);

    Optional<Trip> findByIdAndUser_EmailIgnoreCase(
            Long id,
            String email);
}
