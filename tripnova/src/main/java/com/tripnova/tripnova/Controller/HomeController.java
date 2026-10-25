package com.tripnova.tripnova.Controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
	import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
	




	    @GetMapping("/")
	    public String home(Model model) {

	        model.addAttribute(
	                "appName",
	                "TripNova"
	        );

	        model.addAttribute(
	                "tagline",
	                "Your Journey, Intelligently Planned."
	        );

	        return "index";
	    }
	}
