package com.amazon.ads.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ApiClientInitializer {

    private final static String CLIENT_ID_HEADER_NAME = "Amazon-Advertising-API-ClientId";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final Logger log = LoggerFactory.getLogger(ApiClientInitializer.class);

    @Value("${api.client.id}")
    private String clientId;

    public String getClientId() {
        return clientId;
    }

    @Value("${api.client.secret}")
    private String clientSecret;

    public String getClientSecret() {
        return clientSecret;
    }

    @Value("${api.refresh.token}")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    private String profileId;

    public String getProfileId() {
        return profileId;
    }

    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;

    public ApiClientInitializer() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void init() {
        try {
            generateAccessToken();
            getProfileId(accessToken);
            scheduler.scheduleAtFixedRate(
                    this::generateAccessToken,
                    1,
                    1,
                    TimeUnit.MINUTES
            );
        } catch (IOException e) {
            log.error("IO exception", e);
        } catch (InterruptedException e) {
            log.error("InterruptedException exception");
        }
    }

    private void generateAccessToken() {
        try {
            log.info(clientId);
            String requestBody = String.format("{" +
                            "\"grant_type\": \"refresh_token\"," +
                            "\"refresh_token\": \"%s\"," +
                            "\"redirect_uri\": \"https://www.example.com/login.php\"," +
                            "\"client_id\": \"%s\"," +
                            "\"client_secret\": \"%s\"" +
                            "}",
                    refreshToken, clientId, clientSecret);

            log.info("Get access token request body: {}", requestBody);

            HttpRequest request;
            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.amazon.com/auth/o2/token"))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("Get auth token response status code: {}, {}, {}", response.statusCode()
                    , " AND Response body: ", response.body());
            if (response.statusCode() == 200) {
                Map<String, Object> responseMap = (new JSONObject(response.body())).toMap();
                updateToken(responseMap);
                log.info("Access token successfully refreshed");
            } else {
                log.error("Failed to refresh token. Status: {}", response.statusCode());
                throw new RuntimeException("Token refresh failed");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void updateToken(Map<String, Object> authResponseMap) {
        this.accessToken = "Bearer " + authResponseMap.get("access_token");
        log.info("Access token has been refreshed: {}", accessToken);
    }


    private void getProfileId(String accessToken) throws IOException, InterruptedException {

        try {
            log.info(clientId);

            HttpRequest request;
            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://advertising-api.amazon.com/v2/profiles"))
                    .GET()
                    .header(AUTHORIZATION_HEADER_NAME, accessToken)
                    .header(CLIENT_ID_HEADER_NAME, clientId)
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("Get auth token response status code: {}, {}, {}", response.statusCode()
                    , " AND Response body: ", response.body());
            if (response.statusCode() == 200) {
                JSONObject profileResponseObject = (JSONObject) (new JSONArray(response.body())).get(0);
                profileId = String.valueOf(profileResponseObject.getLong("profileId"));
                log.info("profile Id: {}", profileId);
            } else {
                log.error("Failed to get profile. Status: {}", response.statusCode());
                throw new RuntimeException("Token refresh failed");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
