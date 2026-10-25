package com.tripnova.tripnova.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TripForm {

	 @NotBlank(message  = "Please enter a destination")
	    private String destination;

	    @NotNull(message = "Please select a start date")
	    private LocalDate startDate;

	    @NotNull(message = "Please select an end date")
	    private LocalDate endDate;

	    @NotNull(message = "Please enter your budget")
	    @DecimalMin(value = "1.00", message = "Budget must be greater than zero")
	    private BigDecimal budget;

	    @NotNull(message = "Please enter the number of travelers")
	    @Min(value = 1, message = "At least one traveler is required")
	    private Integer travelers;

	    @NotBlank(message = "Please select your interests")
	    private String interests;

	    public String getDestination() {
	        return destination;
	    }

	    public void setDestination(String destination) {
	        this.destination = destination;
	    }

	    public LocalDate getStartDate() {
	        return startDate;
	    }

	    public void setStartDate(LocalDate startDate) {
	        this.startDate = startDate;
	    }

	    public LocalDate getEndDate() {
	        return endDate;
	    }

	    public void setEndDate(LocalDate endDate) {
	        this.endDate = endDate;
	    }

	    public BigDecimal getBudget() {
	        return budget;
	    }

	    public void setBudget(BigDecimal budget) {
	        this.budget = budget;
	    }

	    public Integer getTravelers() {
	        return travelers;
	    }

	    public void setTravelers(Integer travelers) {
	        this.travelers = travelers;
	    }

	    public String getInterests() {
	        return interests;
	    }

	    public void setInterests(String interests) {
	        this.interests = interests;
	    }
}
