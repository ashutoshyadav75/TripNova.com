package com.tripnova.tripnova.Controller;





import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


import java.security.Principal;


import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


import com.tripnova.tripnova.Entity.Trip;
import com.tripnova.tripnova.Service.TripService;
import com.tripnova.tripnova.dto.TripForm;

import jakarta.validation.Valid;



@Controller
public class TripController {
	

	    private final TripService tripService;

	    public TripController(TripService tripService) {
	        this.tripService = tripService;
	    }

	    // Display the create-trip form
	    @GetMapping("/trips/new")
	    public String showTripForm(Model model) {

	        model.addAttribute("tripForm", new TripForm());

	        return "create-trip";
	    }

	    // Create a new trip for the logged-in user
	    @PostMapping("/trips")
	    public String createTrip(
	            @Valid @ModelAttribute("tripForm") TripForm tripForm,
	            BindingResult bindingResult,
	            Principal principal) {

	        validateDates(tripForm, bindingResult);

	        if (bindingResult.hasErrors()) {
	            return "create-trip";
	        }

	        Trip trip = new Trip();

	        trip.setDestination(tripForm.getDestination().trim());
	        trip.setStartDate(tripForm.getStartDate());
	        trip.setEndDate(tripForm.getEndDate());
	        trip.setBudget(tripForm.getBudget());
	        trip.setTravelers(tripForm.getTravelers());
	        trip.setInterests(tripForm.getInterests());

	        Trip savedTrip = tripService.saveTrip(
	                trip,
	                principal.getName()
	        );

	        return "redirect:/trips/" + savedTrip.getId();
	    }

	    // Display only the logged-in user's trips
	    @GetMapping("/trips")
	    public String listTrips(
	            Model model,
	            Principal principal) {

	        model.addAttribute(
	                "trips",
	                tripService.getTripsForUser(principal.getName())
	        );

	        return "trips";
	    }

	    // Display a trip only if it belongs to the logged-in user
	 // Display a trip and its saved itinerary
	 // Only the logged-in user's trip can be viewed
	 @GetMapping("/trips/{id}")
	 public String tripDetails(
	         @PathVariable Long id,
	         Model model,
	         Principal principal) {

		 Trip trip = tripService.getTripForUser(id, principal.getName());

		 model.addAttribute("trip", trip);
		 model.addAttribute("itinerary", trip.getItinerary());

		 return "trip-details";
	    
	 }

	    // Display the edit form with existing trip details
	    @GetMapping("/trips/{id}/edit")
	    public String showEditForm(
	            @PathVariable Long id,
	            Model model,
	            Principal principal) {

	        Trip trip = tripService.getTripForUser(
	                id,
	                principal.getName()
	        );

	        TripForm form = new TripForm();

	        form.setDestination(trip.getDestination());
	        form.setStartDate(trip.getStartDate());
	        form.setEndDate(trip.getEndDate());
	        form.setBudget(trip.getBudget());
	        form.setTravelers(trip.getTravelers());
	        form.setInterests(trip.getInterests());

	        model.addAttribute("tripForm", form);
	        model.addAttribute("tripId", id);

	        return "edit-trip";
	    }

	    // Update only the logged-in user's trip
	    @PostMapping("/trips/{id}/edit")
	    public String updateTrip(
	            @PathVariable Long id,
	            @Valid @ModelAttribute("tripForm") TripForm form,
	            BindingResult bindingResult,
	            Model model,
	            Principal principal) {

	        validateDates(form, bindingResult);

	        if (bindingResult.hasErrors()) {
	            model.addAttribute("tripId", id);
	            return "edit-trip";
	        }

	        Trip updatedTrip = new Trip();

	        updatedTrip.setDestination(form.getDestination().trim());
	        updatedTrip.setStartDate(form.getStartDate());
	        updatedTrip.setEndDate(form.getEndDate());
	        updatedTrip.setBudget(form.getBudget());
	        updatedTrip.setTravelers(form.getTravelers());
	        updatedTrip.setInterests(form.getInterests());

	        tripService.updateTripForUser(
	                id,
	                updatedTrip,
	                principal.getName()
	        );

	        return "redirect:/trips/" + id;
	    }

	    // Delete only the logged-in user's trip
	    @PostMapping("/trips/{id}/delete")
	    public String deleteTrip(
	            @PathVariable Long id,
	            Principal principal) {

	        tripService.deleteTripForUser(
	                id,
	                principal.getName()
	        );

	        return "redirect:/trips";
	    }

	    // Shared date validation
	    private void validateDates(
	            TripForm form,
	            BindingResult bindingResult) {

	        if (form.getStartDate() != null
	                && form.getEndDate() != null
	                && form.getEndDate().isBefore(form.getStartDate())) {

	            bindingResult.rejectValue(
	                    "endDate",
	                    "date.invalid",
	                    "End date cannot be before the start date"
	            );
	        }
	    }

}
