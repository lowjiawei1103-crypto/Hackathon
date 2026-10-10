package com.busguard.rag;

public class KnowledgeDocument {
    private String docId;
    private String category;
    private String title;
    private String content;

    public KnowledgeDocument(String docId, String category, String title, String content) {
        this.docId = docId;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public String getDocId() { return docId; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
}
