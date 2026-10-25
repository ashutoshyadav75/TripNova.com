package com.tripnova.tripnova.Service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tripnova.tripnova.dto.ItineraryDay;
import com.tripnova.tripnova.dto.TripPlanRequest;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.MutableDataSet;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

@Service
public class ItineraryService {

    private final GeminiService geminiService;

    public ItineraryService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    // =====================================================
    // 1. SPLIT ITINERARY INTO DAYS
    // =====================================================

    public List<ItineraryDay> splitIntoDays(String itinerary) {

        List<ItineraryDay> days = new ArrayList<>();

        if (itinerary == null || itinerary.isBlank()) {

            days.add(new ItineraryDay(
                    "Your Itinerary",
                    "No itinerary content is available."
            ));

            return days;
        }

        Pattern pattern = Pattern.compile(
                "(?im)^\\s*(?:#{1,6}\\s*)?(?:\\*{1,2})?Day\\s+\\d+\\b[^\\r\\n]*"
        );

        Matcher matcher = pattern.matcher(itinerary);

        List<Integer> positions = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        while (matcher.find()) {

            positions.add(matcher.start());

            String title = matcher.group().trim();

            // Remove Markdown heading and bold markers.
            title = title.replaceFirst("^#{1,6}\\s*", "");

            title = title.replaceAll("^\\*{1,2}|\\*{1,2}$", "").trim();

            titles.add(title);
        }

        if (positions.isEmpty()) {

            days.add(new ItineraryDay(
                    "Your Travel Plan",
                    itinerary.trim()
            ));

            return days;
        }

        // Add introduction before the first day, if present.
        String introduction =
                itinerary.substring(0, positions.get(0)).trim();

        if (!introduction.isEmpty()) {

            days.add(new ItineraryDay(
                    "Trip Overview",
                    introduction
            ));
        }

        // Split the itinerary day by day.
        for (int i = 0; i < positions.size(); i++) {

            int start = positions.get(i);

            int end = (i + 1 < positions.size())
                    ? positions.get(i + 1)
                    : itinerary.length();

            String content = itinerary.substring(start, end).trim();

            // Remove the first heading because it is already
            // displayed in the day card heading.
            content = content.replaceFirst(
                    "(?im)^\\s*(?:#{1,6}\\s*)?(?:\\*{1,2})?Day\\s+\\d+\\b[^\\r\\n]*(?:\\*{1,2})?\\s*(?:\\r?\\n|$)",
                    ""
            ).trim();

            days.add(new ItineraryDay(
                    titles.get(i),
                    content
            ));
        }

        return days;
    }


    // =====================================================
    // 2. CONVERT MARKDOWN TO SAFE HTML
    // =====================================================

    public String convertMarkdownToHtml(String markdown) {

        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        MutableDataSet options = new MutableDataSet();

        Parser parser = Parser.builder(options).build();

        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        String html = renderer.render(parser.parse(markdown));

        // Sanitize generated HTML before displaying it.
        PolicyFactory policy = Sanitizers.FORMATTING
                .and(Sanitizers.BLOCKS)
                .and(Sanitizers.LINKS);

        return policy.sanitize(html);
    }


    // =====================================================
    // 3. GENERATE ITINERARY USING GEMINI OR FALLBACK
    // =====================================================

    public String generateItinerary(TripPlanRequest request) {

        String prompt = """
                You are TripNova, an expert travel planner.

                Create a practical, personalized, day-wise travel itinerary.

                Destination: %s
                Start date: %s
                End date: %s
                Total budget: %s
                Number of travelers: %s
                Interests: %s

                Requirements:

                1. Organize the itinerary day by day.
                2. Suggest suitable sightseeing places and activities.
                3. Suggest meal options and approximate local travel costs.
                4. Keep the plan within the stated budget where possible.
                5. Include practical travel tips.
                6. Do not invent exact opening hours or guarantee prices.
                7. Clearly label estimates and mention when details need verification.
                8. Format the answer with clear headings and readable sections.
                9. If the budget is unrealistic, explain why and suggest adjustments.
                10. Cover EVERY date from the start date through the end date, inclusive.
                11. Never shorten a long trip to only one or two days.
                12. If the trip is longer than 14 days, organize it into weekly sections
                    while still covering every date.
                13. Calculate the trip duration from the supplied dates.

                Every day must start on a new line with a heading like:

                ## Day 1: Arrival and Local Exploration

                Use clear headings for Morning, Afternoon, Evening and Travel Tips.
                Keep each day's content separate from the next day.
                """.formatted(
                    request.getDestination(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getBudget(),
                    request.getTravelers(),
                    request.getInterests()
                );

        try {

            // First attempt: generate itinerary using Gemini.
            System.out.println(
                    "TripNova: Attempting Gemini itinerary generation..."
            );

            return geminiService.generateItinerary(prompt);

        } catch (RuntimeException e) {

            // If Gemini fails, use the fallback itinerary.
            System.err.println(
                    "TripNova: Gemini unavailable. "
                    + "Using fallback itinerary. Reason: "
                    + e.getMessage()
            );

            return buildFallbackItinerary(request);
        }
    }


    // =====================================================
    // 4. FALLBACK ITINERARY
    // =====================================================

    private String buildFallbackItinerary(TripPlanRequest request) {

        String destination = request.getDestination();

        LocalDate startDate;
        LocalDate endDate;

        /*
         * This implementation expects getStartDate() and
         * getEndDate() to return LocalDate values.
         *
         * If your DTO uses String dates, adjust the parsing
         * according to the date format in TripPlanRequest.
         */

        try {

            startDate = request.getStartDate();
            endDate = request.getEndDate();

        } catch (Exception e) {

            // Return a basic fallback if dates cannot be read.
            return buildBasicFallback(request);
        }

        if (startDate == null || endDate == null) {
            return buildBasicFallback(request);
        }

        if (endDate.isBefore(startDate)) {
            return buildBasicFallback(request);
        }

        long duration = ChronoUnit.DAYS.between(
                startDate,
                endDate
        ) + 1;

        // Prevent an accidentally enormous response.
        if (duration > 60) {
            duration = 60;
        }

        StringBuilder itinerary = new StringBuilder();

        itinerary.append("# Sample Itinerary for ")
                .append(destination)
                .append("\n\n");

        itinerary.append(
                "> IMPORTANT: Gemini AI is temporarily unavailable. "
                + "This is a general sample itinerary, not an "
                + "AI-generated or verified travel plan.\n\n"
        );

        itinerary.append("**Destination:** ")
                .append(destination)
                .append("\n\n");

        itinerary.append("**Travel Dates:** ")
                .append(startDate)
                .append(" to ")
                .append(endDate)
                .append("\n\n");

        itinerary.append("**Budget:** ")
                .append(request.getBudget())
                .append("\n\n");

        itinerary.append("**Travelers:** ")
                .append(request.getTravelers())
                .append("\n\n");

        itinerary.append("**Interests:** ")
                .append(request.getInterests())
                .append("\n\n");

        itinerary.append("---\n\n");

        // Generate one sample section for every travel date.
        for (int day = 1; day <= duration; day++) {

            LocalDate currentDate = startDate.plusDays(day - 1);

            itinerary.append("## Day ")
                    .append(day)
                    .append(": ")
                    .append(day == 1
                            ? "Arrival and Local Exploration"
                            : day == duration
                                    ? "Final Exploration and Departure"
                                    : "Explore ")
                    .append(day == 1 || day == duration
                            ? ""
                            : destination)
                    .append("\n\n");

            itinerary.append("**Date:** ")
                    .append(currentDate)
                    .append("\n\n");

            itinerary.append("### Morning\n\n");

            if (day == 1) {

                itinerary.append(
                        "- Arrive at your destination and settle in.\n"
                );

                itinerary.append(
                        "- Check your accommodation and plan the day.\n\n"
                );

            } else {

                itinerary.append(
                        "- Have breakfast and review your planned activities.\n"
                );

                itinerary.append(
                        "- Research a suitable nearby attraction.\n\n"
                );
            }

            itinerary.append("### Afternoon\n\n");

            itinerary.append(
                    "- Explore a nearby attraction after verifying its location.\n"
            );

            itinerary.append(
                    "- Have lunch and take a break.\n\n"
            );

            itinerary.append("### Evening\n\n");

            itinerary.append(
                    "- Consider visiting a local market or public area.\n"
            );

            itinerary.append(
                    "- Review transport options for the next day.\n\n"
            );

            itinerary.append("### Travel Tips\n\n");

            itinerary.append(
                    "- Verify opening hours, ticket prices and transport details.\n"
            );

            itinerary.append(
                    "- Keep track of your spending and travel documents.\n\n"
            );

            itinerary.append("---\n\n");
        }

        itinerary.append("## Important Notice\n\n");

        itinerary.append(
                "This fallback plan uses general travel suggestions. "
                + "It does not verify destination-specific attractions, "
                + "live hotel availability, current prices or opening hours. "
                + "Please verify details before travelling. "
                + "You can retry AI generation when the Gemini API quota "
                + "becomes available again."
        );

        return itinerary.toString();
    }


    // =====================================================
    // 5. BASIC FALLBACK WHEN DATE VALUES ARE MISSING
    // =====================================================

    private String buildBasicFallback(TripPlanRequest request) {

        String destination = request.getDestination();

        return """
                # Sample Travel Plan for %s

                > Gemini AI is temporarily unavailable. This is a
                > general sample plan, not an AI-generated itinerary.

                ## Travel Suggestions

                ### Morning
                - Have breakfast and review your travel plans.
                - Research attractions that match your interests.

                ### Afternoon
                - Visit a suitable attraction after verifying its location.
                - Take a break for lunch.

                ### Evening
                - Consider exploring a local market or public area.
                - Plan your next day's activities.

                ## Important Notice

                This is a basic fallback plan. Verify locations, opening
                hours, ticket prices and transportation before travelling.
                Retry AI generation when the Gemini API is available.
                """.formatted(destination);
    }
}