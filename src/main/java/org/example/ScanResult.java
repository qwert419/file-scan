package org.example;

public class ScanResult {
    private String filePath;
    private String url;
    private int statusCode;
    private long contentSize;
    private String timestamp;

    public ScanResult(String filePath, String url, int statusCode, long contentSize, String timestamp) {
        this.filePath = filePath;
        this.url = url;
        this.statusCode = statusCode;
        this.contentSize = contentSize;
        this.timestamp = timestamp;
    }

    public String getFilePath() { return filePath; }
    public String getUrl() { return url; }
    public int getStatusCode() { return statusCode; }
    public long getContentSize() { return contentSize; }
    public String getTimestamp() { return timestamp; }
} 