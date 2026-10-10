package com.busguard.rag;

import java.util.*;
import java.util.stream.Collectors;

public class RAGRetriever {

    private final KnowledgeBase knowledgeBase;

    public RAGRetriever() {
        this.knowledgeBase = new KnowledgeBase();
    }

    public RAGRetriever(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public List<KnowledgeDocument> retrieveRelevantDocs(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return knowledgeBase.getDocuments().stream().limit(topK).collect(Collectors.toList());
        }

        Set<String> queryTokens = tokenize(query);

        Map<KnowledgeDocument, Double> scores = new HashMap<>();

        for (KnowledgeDocument doc : knowledgeBase.getDocuments()) {
            double score = computeRelevanceScore(queryTokens, doc);
            scores.put(doc, score);
        }

        // Sort descending by score
        return scores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .limit(topK)
                .collect(Collectors.toList());
    }

    public String buildRAGContextString(String query, int topK) {
        List<KnowledgeDocument> docs = retrieveRelevantDocs(query, topK);
        StringBuilder sb = new StringBuilder("=== RETRIEVED RAPID KL RAG KNOWLEDGE BASE CONTEXT ===\n");
        for (KnowledgeDocument doc : docs) {
            sb.append(String.format("[%s - %s]: %s\nContent: %s\n\n",
                    doc.getDocId(), doc.getCategory(), doc.getTitle(), doc.getContent()));
        }
        return sb.toString();
    }

    private double computeRelevanceScore(Set<String> queryTokens, KnowledgeDocument doc) {
        Set<String> docTokens = tokenize(doc.getTitle() + " " + doc.getCategory() + " " + doc.getContent());
        
        int matches = 0;
        for (String q : queryTokens) {
            if (docTokens.contains(q)) {
                matches++;
            }
        }

        if (docTokens.isEmpty()) return 0.0;
        return (double) matches / Math.sqrt(docTokens.size() + 1);
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Collections.emptySet();
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        Set<String> set = new HashSet<>();
        for (String w : words) {
            if (w.length() > 2) {
                set.add(w);
            }
        }
        return set;
    }
}
