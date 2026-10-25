package com.tripnova.tripnova.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;


import com.tripnova.tripnova.Entity.*;
import com.tripnova.tripnova.Repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
public class TripService {

	private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public TripService(
            TripRepository tripRepository,
            UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    // Save a new trip for the logged-in user
    @Transactional
    public Trip saveTrip(Trip trip, String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Logged-in user not found"
                        ));

        trip.setUser(user);

        return tripRepository.save(trip);
    }

    // Get only the logged-in user's trips
    @Transactional(readOnly = true)
    public List<Trip> getTripsForUser(String email) {

        return tripRepository
                .findAllByUser_EmailIgnoreCaseOrderByIdDesc(email);
    }

    // Find a trip only if it belongs to the logged-in user
    @Transactional(readOnly = true)
    public Trip getTripForUser(Long id, String email) {

        return tripRepository
                .findByIdAndUser_EmailIgnoreCase(id, email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Trip not found"
                        ));
    }

    // Update an existing trip without changing its owner
    @Transactional
    public Trip updateTripForUser(
            Long id,
            Trip updatedTrip,
            String email) {

        Trip existingTrip = tripRepository
                .findByIdAndUser_EmailIgnoreCase(id, email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Trip not found"
                        ));

        existingTrip.setDestination(updatedTrip.getDestination());
        existingTrip.setStartDate(updatedTrip.getStartDate());
        existingTrip.setEndDate(updatedTrip.getEndDate());
        existingTrip.setBudget(updatedTrip.getBudget());
        existingTrip.setTravelers(updatedTrip.getTravelers());
        existingTrip.setInterests(updatedTrip.getInterests());
        existingTrip.setItinerary(updatedTrip.getItinerary());
        existingTrip.setItinerary(updatedTrip.getItinerary());

        return tripRepository.save(existingTrip);
    }

    // Delete only the logged-in user's trip
    @Transactional
    public void deleteTripForUser(Long id, String email) {

        Trip trip = tripRepository
                .findByIdAndUser_EmailIgnoreCase(id, email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Trip not found"
                        ));

        tripRepository.delete(trip);
    }
}

