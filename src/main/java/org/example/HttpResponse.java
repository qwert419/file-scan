package org.example;

public class HttpResponse {
    private int statusCode;
    private String content;
    private long size;

    public HttpResponse(int statusCode, String content, long size) {
        this.statusCode = statusCode;
        this.content = content;
        this.size = size;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContent() {
        return content;
    }

    public long getSize() {
        return size;
    }
} 