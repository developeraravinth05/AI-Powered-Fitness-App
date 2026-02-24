package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j

public class ActivityAIService {

    @Autowired
    GeminiService geminiService;

    public Recommendation generateRecommendation(Activity activity) {
        String prompt = createPromptForActivity(activity);
        String aiResponse = geminiService.getAnswer(prompt);
        log.info("Response from AI:{}", aiResponse);
       return processAIResponse(activity, aiResponse);

    }

    private Recommendation processAIResponse(Activity activity, String aiResponse) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);
            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String jsonContent = textNode.asText()
                    .replaceAll("'''json\\n", "")
                    .replaceAll("\\n'''", "")
                    .trim();

            log.info("parsed response from ai" + jsonContent);
            JsonNode analysisJson = mapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");
            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis, analysisNode, "overall", "Overall");
            addAnalysisSection(fullAnalysis, analysisNode, "pace", "Pace");
            addAnalysisSection(fullAnalysis, analysisNode, "heartRate", "HeartRate");
            addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurned", "CaloriesBurned");

            List<String> improvements =extractImprovement(analysisJson.path("improvements"));
            List<String> suggestions =extractSuggestions(analysisJson.path("suggestions"));
            List<String> safety =extractSafetyGuidelines(analysisJson.path("safety"));


          return Recommendation.builder()
                  .activityId(activity.getId())
                  .userId(activity.getUserId())
                  .activityType(activity.getType())
                  .recommendation(fullAnalysis.toString().trim())
                  .improvements(improvements)
                  .suggestions(suggestions)
                  .safety(safety)
                  .createdAt(LocalDateTime.now())
                  .build();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return createDefaultRecommendation(activity);
    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .recommendation("unable to generate detail analysis")
                .suggestions(Collections.singletonList("continue with goal"))
                .improvements(Collections.singletonList("consult a trainer"))
                .safety(Collections.singletonList("warm up always"))
                .createdAt(LocalDateTime.now())
                .build();
    }


    private List<String> extractSafetyGuidelines(JsonNode safetyNode) {
        List<String> safety = new ArrayList<>();
        if (safetyNode.isArray()) {
            safetyNode.forEach(item -> safety.add(item.asText()));
        }
        return safety.isEmpty() ?
                Collections.singletonList("follow general saftey guidelines") :
                safety;
    }


    private List<String> extractSuggestions(JsonNode suggestionNode) {
        List<String> suggestions = new ArrayList<>();
        if (suggestionNode.isArray()) {
            suggestionNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s", workout, description));

            });
        }
        return suggestions.isEmpty() ?
                Collections.singletonList("No specific suggestions provided") :
                suggestions;
    }
    private List<String> extractImprovement(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();
        if (improvementsNode.isArray()) {
            improvementsNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String detail = improvement.path("recommendation").asText();
                improvements.add(String.format("%s: %s", area, detail));

            });
        }
        return improvements.isEmpty() ?
                Collections.singletonList("No specific improvements provided") :
                improvements;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if (!analysisNode.path(key).isMissingNode()) {
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }


    }


    private String createPromptForActivity(Activity activity) {
        return String.format(
                """
                        You are a professional AI fitness coach.
                                 
                        Analyze the activity data and generate structured fitness feedback.
                                 
                        Activity Data:
                        Activity ID: {{activityId}}
                        User ID: {{userId}}
                        Activity Type: {{activityType}}
                        Duration: {{duration}} minutes
                        Calories Burned: {{caloriesBurned}}
                        Average Heart Rate: {{heartRate}}
                        Goal: {{goal}}
                                 
                        Instructions:
                        - Adapt analysis and advice based on activity type.
                        - For cardio → evaluate endurance, pace, and heart rate zones.
                        - For strength training → evaluate intensity and muscle engagement.
                        - Personalize based on user goal (weight loss, muscle gain, endurance, general fitness).
                        - Keep insights practical and safe.
                                 
                        Return ONLY valid JSON in this exact structure:
                                 
                        {
                          "analysis": {
                            "overall": "brief overall performance summary",
                            "pace": "evaluation of pacing or workout intensity",
                            "heartRate": "analysis of heart rate performance",
                            "caloriesBurned": "calorie efficiency analysis"
                          },
                                 
                          "improvements": [
                            {
                              "area": "specific improvement area",
                              "recommendation": "how to improve"
                            }
                          ],
                                 
                          "suggestions": [
                            {
                              "workout": "workout name",
                              "description": "short description"
                            }
                          ],
                                 
                          "safety": [
                            "simple safety tip 1",
                            "simple safety tip 2"
                          ]
                        }
                                 
                        Do not include markdown.
                        Do not include explanation outside JSON.
                        Ensure all fields are present.
                        Keep responses concise.
                            
                        """
        );
    }

}
