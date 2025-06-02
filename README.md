# AI Trip Planner

The AI trip planner is a user-friendly standalone Java 21 Gradle Spring Boot BE service written designed to create tailored travel itineraries. It will have a PostgreSql database. It should have option for non-logged in users to create itiniraries and overview them as a last step but only when logged in (creating an account) you users will be able to save their itinerary/share it via a link/ request to book that itinerary.

The app is intended to connect and work with a mobile application frontend (Flutter) via a JWT in the future but this project focuses only the BE part.

The BE should be abble to support is the following user scenario (via integrating with google AI studio API key to retrieve real time data and process decision makings via integrated prompts with placeholders for the parameters that will be passed in the BE):

1.  **Accept terms of service screen**
2.  **Destination, number of days and budget selection**:
    *   Users choose a specific destination or select preferences (e.g., nature, architecture, ancient civilizations, weather).
    *   If preferences are chosen, three cards display top-rated countries, each featuring an image and a two-sentence overview.
    *   User selects the duration of the trip in days and the expected budget range.
3.  **Interest Mapping**:
    *   Upon selecting a destination, users view a “map of interests” (e.g., nature sightseeing, rock music, theaters, skiing, museums).
    *   Selected interests are stored to personalize subsequent recommendations.
4.  **Daily Activity Planning**:
    *   For each trip day, starting with day 1, the planner presents four activity cards based on user interests.
    *   Each card includes:
        *   City of the activity
        *   Background image
        *   Activity name
        *   Expected duration (hours)
        *   Estimated cost (EUR)
    *   After selecting an activity, if time permits, another set of four cards appears, optimized for minimal distance from the previous location and fitting within the day’s schedule (accounting for travel to accommodations).
    *   For subsequent days, the planner balances proximity with highly attractive, potentially distant activities, ensuring variety and appeal. Budget and the trip duration in days are also taken into consideration – the last day cards should calculate proximity to a point where the trip is convenient to end (near a airport).
5.  **Itinerary Finalization**:
    *   Users continue planning until selecting “End trip planning.”
    *   An overview mode displays all chosen activities, with options to create an account or log in to save the itinerary.
    *   Saved itineraries can be shared via unrestricted links, encouraging social engagement.
