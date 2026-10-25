
package com.tripnova.tripnova.Controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tripnova.tripnova.Entity.Trip;
import com.tripnova.tripnova.Service.ItineraryService;
import com.tripnova.tripnova.Service.TripService;
import com.tripnova.tripnova.dto.TripPlanRequest;

@Controller
public class TripPlannerController {

    private final ItineraryService itineraryService;
    private final TripService tripService;

    public TripPlannerController(
            ItineraryService itineraryService,
            TripService tripService) {
        this.itineraryService = itineraryService;
        this.tripService = tripService;
    }

    @GetMapping("/plan-trip")
    public String showPlanner(
            @RequestParam(required = false) Long tripId,
            Model model,
            Principal principal) {

        TripPlanRequest request = new TripPlanRequest();

        if (tripId != null) {
            Trip trip = tripService.getTripForUser(tripId, principal.getName());

            request.setDestination(trip.getDestination());
            request.setStartDate(trip.getStartDate());
            request.setEndDate(trip.getEndDate());
            request.setBudget(trip.getBudget());
            request.setTravelers(trip.getTravelers());
            request.setInterests(trip.getInterests());

            model.addAttribute("tripId", trip.getId());
        }

        model.addAttribute("tripPlanRequest", request);
        return "plan-trip";
    }

    @PostMapping("/plan-trip")
    public String generatePlan(
            @ModelAttribute("tripPlanRequest") TripPlanRequest request,
            @RequestParam(required = false) Long tripId,
            Model model,
            Principal principal) {

        // Validate destination
        if (request.getDestination() == null
                || request.getDestination().isBlank()) {
            model.addAttribute("errorMessage",
                    "Please enter a destination.");
            return "plan-trip";
        }

        // Validate dates
        if (request.getStartDate() == null
                || request.getEndDate() == null
                || request.getEndDate().isBefore(request.getStartDate())) {
            model.addAttribute("errorMessage",
                    "Please select valid travel dates.");
            return "plan-trip";
        }

        // Validate budget
        if (request.getBudget() == null
                || request.getBudget().compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("errorMessage",
                    "Budget must be greater than zero.");
            return "plan-trip";
        }

        // Validate travelers
        if (request.getTravelers() == null
                || request.getTravelers() < 1
                || request.getTravelers() > 50) {
            model.addAttribute("errorMessage",
                    "Travelers must be between 1 and 50.");
            return "plan-trip";
        }

        // Validate interests
        if (request.getInterests() == null
                || request.getInterests().isBlank()) {
            model.addAttribute("errorMessage",
                    "Please enter your travel interests.");
            return "plan-trip";
        }

        // Generate the AI itinerary
        long aiStartTime = System.currentTimeMillis();

        String itinerary = itineraryService.generateItinerary(request);

        long aiEndTime = System.currentTimeMillis();

        System.out.println("TripNova AI Generation Time: "
                + (aiEndTime - aiStartTime) + " ms");

        System.out.println("========== TRIPNOVA DEBUG ==========");
        System.out.println("Trip ID received: " + tripId);
        System.out.println("Itinerary is null: " + (itinerary == null));
        System.out.println("Itinerary length: "
                + (itinerary == null ? 0 : itinerary.length()));
        System.out.println("====================================");
        Trip savedTrip;
        
        
        
        long saveStartTime = System.currentTimeMillis();

        if (tripId != null) {
            // Update the existing trip owned by the logged-in user
            Trip updatedTrip = new Trip();
            updatedTrip.setDestination(request.getDestination().trim());
            updatedTrip.setStartDate(request.getStartDate());
            updatedTrip.setEndDate(request.getEndDate());
            updatedTrip.setBudget(request.getBudget());
            updatedTrip.setTravelers(request.getTravelers());
            updatedTrip.setInterests(request.getInterests().trim());
            updatedTrip.setItinerary(itinerary);

            savedTrip = tripService.updateTripForUser(
                    tripId,
                    updatedTrip,
                    principal.getName()
            );

        } else {
            // Create a new trip
            Trip trip = new Trip();
            trip.setDestination(request.getDestination().trim());
            trip.setStartDate(request.getStartDate());
            trip.setEndDate(request.getEndDate());
            trip.setBudget(request.getBudget());
            trip.setTravelers(request.getTravelers());
            trip.setInterests(request.getInterests().trim());
            trip.setItinerary(itinerary);

            savedTrip = tripService.saveTrip(
                    trip,
                    principal.getName()
            );
        }
        
        long saveEndTime = System.currentTimeMillis();

        System.out.println("TripNova Database Save Time: "
                + (saveEndTime - saveStartTime) + " ms");

        model.addAttribute("tripPlanRequest", request);
        model.addAttribute("itinerary", itinerary);
        
        long processingStartTime = System.currentTimeMillis();
        var itineraryDays = itineraryService.splitIntoDays(itinerary);

        for (var day : itineraryDays) {
            day.setHtmlContent(
                    itineraryService.convertMarkdownToHtml(day.getContent())
            );
        }
        
        long processingEndTime = System.currentTimeMillis();

        System.out.println("TripNova Itinerary Processing Time: "
                + (processingEndTime - processingStartTime) + " ms");

        model.addAttribute("itineraryDays", itineraryDays);
        model.addAttribute("savedTripId", savedTrip.getId());

        return "itinerary-result";
    }
}
