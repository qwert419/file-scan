package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;

public class HttpFetcher {
    private static final int TIMEOUT = 10000;

    public static HttpResponse fetchUrlWithResponse(String url, ProxyConfig proxyConfig) throws IOException {
        if (proxyConfig != null && proxyConfig.isEnabled()) {
            return fetchWithProxy(url, proxyConfig);
        } else {
            return fetchWithoutProxy(url);
        }
    }

    private static HttpResponse fetchWithoutProxy(String url) throws IOException {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .setRedirectsEnabled(false)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .disableRedirectHandling()
                .build()) {
            
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                long contentLength = 0;
                
                if (entity != null) {
                    // 读取整个响应体来计算大小
                    byte[] content = EntityUtils.toByteArray(entity);
                    contentLength = content.length;
                }
                
                return new HttpResponse(statusCode, "", contentLength);
            }
        }
    }

    private static HttpResponse fetchWithProxy(String url, ProxyConfig proxyConfig) throws IOException {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .setRedirectsEnabled(false)
                .setProxy(proxyConfig.getHttpHost())
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .disableRedirectHandling()
                .build()) {
            
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                long contentLength = 0;
                
                if (entity != null) {
                    // 读取整个响应体来计算大小
                    byte[] content = EntityUtils.toByteArray(entity);
                    contentLength = content.length;
                }
                
                return new HttpResponse(statusCode, "", contentLength);
            }
        }
    }
}