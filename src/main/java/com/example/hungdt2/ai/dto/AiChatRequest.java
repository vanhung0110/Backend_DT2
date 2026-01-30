package com.example.hungdt2.ai.dto;

import java.util.List;

public class AiChatRequest {
    private String message;
    private List<HistoryItem> history;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<HistoryItem> getHistory() { return history; }
    public void setHistory(List<HistoryItem> history) { this.history = history; }

    public static class HistoryItem {
        private String role;    // "user" | "assistant"
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
