package com.gpu.sharing.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiParsingService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String parseWorkload(String userInput) {
        String apiKey = geminiApiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            // Check system environment directly as fallback
            apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = System.getProperty("GEMINI_API_KEY");
            }
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("Gemini API Key is missing. Using local fallback parser.");
            return localFallbackParse(userInput);
        }

        try {
            String systemPrompt = "You are a workload mapping assistant. Your job is to match a user's natural language request (in English or Korean) to the closest predefined workload ID from the following list:\n" +
                    "w0: resnet50-train (batch32)\n" +
                    "w1: resnet50-train (batch64)\n" +
                    "w2: resnet50-train (batch128)\n" +
                    "w3: bert-base-cased-train (batch8)\n" +
                    "w4: bert-base-cased-train (batch16)\n" +
                    "w5: bert-base-cased-train (batch32)\n" +
                    "w6: openai-whisper-large-v2-inf (batch4)\n" +
                    "w7: openai-whisper-large-v2-inf (batch8)\n" +
                    "w8: openai-whisper-large-v2-inf (batch16)\n" +
                    "w9: google-mobilenet_v2-inf (batch16)\n" +
                    "w10: google-mobilenet_v2-inf (batch32)\n" +
                    "w11: google-mobilenet_v2-inf (batch64)\n" +
                    "w12: google-vit-base-patch16-224-inf (batch8)\n" +
                    "w13: google-vit-base-patch16-224-inf (batch16)\n" +
                    "w14: google-vit-base-patch16-224-inf (batch32)\n" +
                    "w15: bert-base-cased-inf (batch16)\n" +
                    "w16: bert-base-cased-inf (batch32)\n" +
                    "w17: bert-base-cased-inf (batch64)\n\n" +
                    "Respond ONLY with the workload ID (e.g., w2). Do not include any other text, markdown, punctuation, or explanations.";

            String payload = "{"
                    + "\"contents\": [{"
                    +   "\"role\": \"user\","
                    +   "\"parts\": [{\"text\": \"" + escapeJson(systemPrompt + "\n\nUser input: " + userInput) + "\"}]"
                    + "}]"
                    + "}";

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Simple regex to find workload id pattern (w followed by 1 or 2 digits)
                Pattern pattern = Pattern.compile("w\\d{1,2}");
                Matcher matcher = pattern.matcher(body);
                if (matcher.find()) {
                    String matchedId = matcher.group();
                    int idVal = Integer.parseInt(matchedId.substring(1));
                    if (idVal >= 0 && idVal <= 17) {
                        return matchedId;
                    }
                }
            } else {
                System.out.println("Gemini API call failed with status: " + response.statusCode() + ". Body: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error invoking Gemini API: " + e.getMessage());
        }

        return localFallbackParse(userInput);
    }

    private String localFallbackParse(String userInput) {
        String input = userInput.toLowerCase();
        
        // Determine category
        boolean isResnet = input.contains("resnet") || input.contains("restnet") || input.contains("레즈넷") || input.contains("레스트넷");
        boolean isWhisper = input.contains("whisper") || input.contains("위스퍼");
        boolean isMobilenet = input.contains("mobilenet") || input.contains("모바일넷");
        boolean isVit = input.contains("vit") || input.contains("비전") || input.contains("vision");
        boolean isBert = input.contains("bert") || input.contains("버트");
        
        boolean isTrain = input.contains("train") || input.contains("학습") || input.contains("훈련") || input.contains("learning");
        
        // Find batch size
        int batch = 0;
        Pattern pattern = Pattern.compile("\\b(batch|배치)?\\s*(\\d+)\\b");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            batch = Integer.parseInt(matcher.group(2));
        }

        if (isResnet) {
            if (batch >= 128) return "w2";
            if (batch >= 64) return "w1";
            return "w0";
        } else if (isWhisper) {
            if (batch >= 16) return "w8";
            if (batch >= 8) return "w7";
            return "w6";
        } else if (isMobilenet) {
            if (batch >= 64) return "w11";
            if (batch >= 32) return "w10";
            return "w9";
        } else if (isVit) {
            if (batch >= 32) return "w14";
            if (batch >= 16) return "w13";
            return "w12";
        } else if (isBert) {
            if (isTrain) {
                if (batch >= 32) return "w5";
                if (batch >= 16) return "w4";
                return "w3";
            } else {
                if (batch >= 64) return "w17";
                if (batch >= 32) return "w16";
                return "w15";
            }
        }

        // Default default
        return "w0";
    }

    private String escapeJson(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
