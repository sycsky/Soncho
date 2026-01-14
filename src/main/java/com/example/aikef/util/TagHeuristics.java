package com.example.aikef.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TagHeuristics {
    public static List<String> suggest(String text) {
        List<String> tags = new ArrayList<>();
        if (text == null) return tags;
        
        String lower = text.toLowerCase(Locale.ROOT);
        
        if (lower.contains("refund") || lower.contains("money back") || lower.contains("return") || lower.contains("退款")) {
            tags.add("Refund");
        }
        if (lower.contains("shipping") || lower.contains("delivery") || lower.contains("track") || lower.contains("物流") || lower.contains("快递")) {
            tags.add("Shipping");
        }
        if (lower.contains("broken") || lower.contains("damage") || lower.contains("not working") || lower.contains("坏") || lower.contains("破损")) {
            tags.add("Defect");
        }
        if (lower.contains("price") || lower.contains("cost") || lower.contains("expensive") || lower.contains("价格") || lower.contains("贵")) {
            tags.add("Pricing");
        }
        if (lower.contains("thank") || lower.contains("good") || lower.contains("great") || lower.contains("谢谢") || lower.contains("好")) {
            tags.add("Positive");
        }
        if (lower.contains("bad") || lower.contains("slow") || lower.contains("worst") || lower.contains("差") || lower.contains("慢")) {
            tags.add("Complaint");
        }
        
        return tags;
    }
}
