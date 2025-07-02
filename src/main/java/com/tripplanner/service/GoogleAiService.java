package com.tripplanner.service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class GoogleAiService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAiService.class);

    private final VertexAI vertexAI;
    private final String modelName;

    @Autowired
    public GoogleAiService(VertexAI vertexAI, @Value("${google.ai.model-name}") String modelName) {
        this.vertexAI = vertexAI;
        this.modelName = modelName;
    }

    public String generateContent(String textPrompt) {
        logger.info("Sending prompt to Vertex AI Gemini (length: {}): {}", textPrompt.length(), textPrompt);
        try {
            GenerativeModel model = new GenerativeModel(modelName, this.vertexAI);
            GenerateContentResponse response = model.generateContent(textPrompt);
            String result = ResponseHandler.getText(response);
            logger.info("Received response from Vertex AI Gemini.");
            return result;
        } catch (IOException e) {
            logger.error("Error generating content with Vertex AI: ", e);
            return "{\"error\": \"Could not get a response from AI service: " + e.getMessage().replace("\"", "\\\"") + "\"}";
        } catch (Exception e) {
            logger.error("Unexpected error generating content: ", e);
            return "{\"error\": \"Unexpected error: " + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }

    public String suggestCities(List<String> preferences) {
        String preferencesString = String.join(", ", preferences);
        String prompt = String.format(
            "You are an expert travel advisor.\n"
                + "Suggest exactly three distinct cities for a tourist whose preferences are: [%s].\n"
                + "Keep the response in a strict JSON format as the one below. Do not allow invalid JSON to be returned."
                + "For each city, provide:\n"
                + "- country: The name of the country (String).\n"
                + "- city: The name of the city (String).\n"
                + "- overview: A compelling two-sentence overview of why it matches the preferences (String).\n"
                + "- imageUrl: Try to provide a direct URL to a publicly usable, high-quality, and directly embeddable image that represents the country and its highlighted aspects (e.g., from Wikimedia Commons, Pexels, Unsplash, or similar open-license sources). If you cannot find a suitable real image URL, then use the specific string 'placeholder_image_for_COUNTRY_NAME' where COUNTRY_NAME is the actual name of the country (e.g., 'placeholder_image_for_Italy'). (String).\n"
                + "Format the output STRICTLY as a single JSON array of objects. Do not include any text outside this JSON array.\n"
                + "Example if real image found for Italy but not Greece:\n"
                + "[\n"
                + "  {\"country\": \"Italy\", \"city\": \"Milan\",\"overview\": \"Italy offers stunning architecture and "
                + "ancient Roman ruins. "
                + "Its sunny Mediterranean weather is perfect for exploring beautiful coastlines.\", \"imageUrl\": \"https://example.com/italy.jpg\"},\n"
                + "  {\"country\": \"Greece\",\"city\": \"Athens\", \"overview\": \"Greece is renowned for its ancient "
                + "civilizations like the Acropolis in Athens. It also boasts beautiful islands with sunny weather.\", \"imageUrl\": \"placeholder_image_for_Greece\"}\n"
                + "]",
            preferencesString
        );
        return generateContent(prompt);
    }

    public String suggestActivities(
        String destination,
        List<String> interests,
        int numberOfAdults,
        int numberOfChildren,
        LocalDate fromDate,
        LocalDate toDate,
        int dayNumber,
        int totalTripDays,
        List<String> previousActivitiesToday,
        double availableHoursToday,
        String lastActivityCity,
        String lastActivityName,
        String departureCity
    ) {
        if (destination == null || destination.isEmpty()) {
            throw new IllegalArgumentException("Destination cannot be null or empty");
        }
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate cannot be null");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate cannot be before fromDate");
        }
        if (interests == null) {
            interests = List.of();
        }
        if (previousActivitiesToday == null) {
            previousActivitiesToday = List.of();
        }

        String interestsString = String.join(", ", interests);
        String previousActivitiesString = String.join(", ", previousActivitiesToday);

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an expert travel planner AI.\n");
        promptBuilder.append(String.format(
            "For a trip to %s for a group of %d adult(s) and %d child(ren) with interests in [%s], from %s to %s, on Day %d of a %d-day trip:\n",
            destination, numberOfAdults, numberOfChildren, interestsString, fromDate, toDate, dayNumber, totalTripDays
        ));

        promptBuilder.append(String.format("The group has %.1f hours available for activities for the rest of today.\n", availableHoursToday));

        if (lastActivityName != null && !lastActivityName.isEmpty()) {
            promptBuilder.append(String.format(
                "The last activity selected today was '%s' in '%s'. Please suggest subsequent activities that are geographically convenient if possible and fit the remaining time.\n",
                lastActivityName, (lastActivityCity != null ? lastActivityCity : destination)
            ));
        } else {
            promptBuilder.append("This is the first set of suggestions for today, or the previous activity context is not available. Please suggest initial activities for the day.\n");
        }

        promptBuilder.append(String.format(
            "Consider the following activities already selected for today and try to avoid suggesting them again: [%s].\n",
            previousActivitiesString
        ));

        if (dayNumber == totalTripDays) {
            String departureLocation = (departureCity != null && !departureCity.isEmpty()) ? departureCity : destination;
            promptBuilder.append(String.format(
                "Since this is the last day (Day %d of %d), prioritize activities that are convenient for departure, such as those near a major airport in %s, and fit within the available hours.\n",
                dayNumber, totalTripDays, departureLocation
            ));
        }

        promptBuilder.append(String.format(
            "Please suggest up to 3 distinct activities suitable for %d adult(s) and %d child(ren). Each suggested activity's 'expectedDurationHours' must be less than or equal to the available %.1f hours.\n",
            numberOfAdults, numberOfChildren, availableHoursToday
        ));
        promptBuilder.append("If multiple activities are suggested, their combined duration is not constrained, but individual activities must be plannable within the remaining time. Prioritize variety and user interests.\n");

        promptBuilder.append("Keep the response in a strict JSON format as the one below. Do not allow invalid JSON to be returned.\n");
        promptBuilder.append("For each activity, provide the following details in JSON format:\n"
            + "- name: Name of the activity (String)\n"
            + "- city: The city where the activity is located. This might be %s or a nearby city. (String)\n"
            + "- description: A brief, engaging one-sentence description of the activity. (String)\n"
            + "- expectedDurationHours: Estimated duration of the activity in hours (Number, e.g., 2.5). This MUST be less than or equal to the available hours for the day.\n"
            + "- estimatedCostEUR: Estimated cost of the activity in EUR (Number, e.g., 50).\n"
            + "- address: Full address of the activity in the format - city, country, street name, street number (String)\n"
            + "Return the output as a single JSON array of activity objects. If no activities can fit the criteria (especially time), return an empty JSON array [].\n"
            + "Example:\n"
            + "[\n"
            + "  {\"name\": \"Example Museum Visit\", \"city\": \"%s\", \"description\": \"Explore fascinating exhibits.\", \"expectedDurationHours\": 2, \"estimatedCostEUR\": 15, \"address\": \"%s, Example Country, Museum Street, 123\"}\n"
            + "]\n"
            + "Do not include any explanatory text outside of the JSON array.");

        String prompt = String.format(promptBuilder.toString(),
            destination,         // For city in activity description
            destination,         // For city in example
            destination          // For city in example address
        );
        return generateContent(prompt);
    }
}