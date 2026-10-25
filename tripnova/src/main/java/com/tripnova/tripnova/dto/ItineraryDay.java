package com.tripnova.tripnova.dto;

public class ItineraryDay {

    private String title;
    private String content;
    private String htmlContent;

    public ItineraryDay(String title, String content) {
        this.title = title;
        this.content = content;
        this.htmlContent = content;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }
}