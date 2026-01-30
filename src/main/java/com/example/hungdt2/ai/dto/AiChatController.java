package com.example.hungdt2.ai.dto;

import com.example.hungdt2.ai.dto.AiChatRequest;
import com.example.hungdt2.ai.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final RestClient restClient = RestClient.builder().build();

    @Value("${OPENAI_API_KEY:}")
    private String openaiKey;

    @Value("${OPENAI_MODEL:gpt-4o-mini}")
    private String model;

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public AiChatResponse chat(@RequestBody AiChatRequest req) {

        if (openaiKey == null || openaiKey.isBlank()) {
            return new AiChatResponse("❌ Missing OPENAI_API_KEY (hãy set env trên Render).");
        }

        // Build messages
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "Bạn là trợ lý AI thân thiện, trả lời ngắn gọn bằng tiếng Việt."
        ));

        if (req.getHistory() != null) {
            for (AiChatRequest.HistoryItem h : req.getHistory()) {
                if (h.getRole() != null && h.getContent() != null) {
                    messages.add(Map.of("role", h.getRole(), "content", h.getContent()));
                }
            }
        }

        if (req.getMessage() != null && !req.getMessage().isBlank()) {
            messages.add(Map.of("role", "user", "content", req.getMessage()));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);

        // Call OpenAI
        Map response = restClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        // Parse reply
        try {
            List choices = (List) response.get("choices");
            Map c0 = (Map) choices.get(0);
            Map msg = (Map) c0.get("message");
            String content = (String) msg.get("content");
            return new AiChatResponse(content != null ? content : "");
        } catch (Exception e) {
            return new AiChatResponse("❌ Không parse được response từ OpenAI: " + e.getMessage());
        }
    }
}
