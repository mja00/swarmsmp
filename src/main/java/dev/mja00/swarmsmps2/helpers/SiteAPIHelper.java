package dev.mja00.swarmsmps2.helpers;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.objects.Commands;
import dev.mja00.swarmsmps2.objects.JoinInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SiteAPIHelper {

    private String apiKey;
    private String siteUrl;
    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    private static Logger LOGGER = LogManager.getLogger("SITEAPIHELPER");
    private final Gson gson = new Gson();
    // Create a custom exception for when the API request fails
    public static class APIRequestFailedException extends Exception {
        public APIRequestFailedException(String message) {
            super(message);
        }
    }

    public SiteAPIHelper(String apiKey, String siteUrl) {
        this.apiKey = apiKey;
        this.siteUrl = siteUrl;
    }

    @Nullable
    public String doGetRequest(String url) {
        HttpRequest apiRequest = HttpRequest.newBuilder().GET().uri(URI.create(url)).setHeader("User-Agent", "Swarmsmps2").setHeader("Authorization", "Bearer " + this.apiKey).build();
        CompletableFuture<HttpResponse<String>> response = client.sendAsync(apiRequest, HttpResponse.BodyHandlers.ofString());
        String responseBody;

        try {
            responseBody = response.thenApply(HttpResponse::body).get(SSMPS2Config.SERVER.firstTimeout.get(), TimeUnit.SECONDS);
            return responseBody;
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            if (e instanceof java.util.concurrent.TimeoutException) {
                // Retry it ig lol
                try {
                    CompletableFuture<HttpResponse<String>> response2 = client.sendAsync(apiRequest, HttpResponse.BodyHandlers.ofString());
                    responseBody = response2.thenApply(HttpResponse::body).get(SSMPS2Config.SERVER.secondTimeout.get(), TimeUnit.SECONDS);
                    return responseBody;
                } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e2) {
                    LOGGER.error("Error while sending request to " + url + ": " + e2.getMessage());
                    return null;
                }
            } else {
                // We got some sort of error, log it and return null
                LOGGER.error("Error while sending request to " + url + ": " + e.getMessage());
                return null;
            }
        }
    }

    @Nullable
    public String doDeleteRequest(String url) {
        HttpRequest apiRequest = HttpRequest.newBuilder().DELETE().uri(URI.create(url)).setHeader("User-Agent", "Swarmsmps2").setHeader("Authorization", "Bearer " + this.apiKey).build();
        CompletableFuture<HttpResponse<String>> response = client.sendAsync(apiRequest, HttpResponse.BodyHandlers.ofString());
        String responseBody;

        try {
            responseBody = response.thenApply(HttpResponse::body).get(SSMPS2Config.SERVER.firstTimeout.get(), TimeUnit.SECONDS);
            return responseBody;
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            if (e instanceof java.util.concurrent.TimeoutException) {
                // Retry it ig lol
                try {
                    CompletableFuture<HttpResponse<String>> response2 = client.sendAsync(apiRequest, HttpResponse.BodyHandlers.ofString());
                    responseBody = response2.thenApply(HttpResponse::body).get(SSMPS2Config.SERVER.secondTimeout.get(), TimeUnit.SECONDS);
                    return responseBody;
                } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e2) {
                    LOGGER.error("Error while sending request to " + url + ": " + e2.getMessage());
                }
            } else {
                // We got some sort of error, log it and return null
                LOGGER.error("Error while sending request to " + url + ": " + e.getMessage());
            }
        }
        return null;
    }

    public JoinInfo getJoinInfo(String uuid) throws APIRequestFailedException {
        String endpoint = "whitelist/integration_id:minecraft:" + uuid;
        String getURL = this.siteUrl + endpoint;

        String responseBody = this.doGetRequest(getURL);
        if (responseBody == null) {
            throw new APIRequestFailedException("Request to " + getURL + " failed");
        }
        // Parse the response
        try {
            return gson.fromJson(responseBody, JoinInfo.class);
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new APIRequestFailedException("Failed to parse response.");
        }
    }

    public Commands getCommandInfo(String uuid) throws APIRequestFailedException {
        String endpoint = "commands/integration_id:minecraft:" + uuid;
        String getURL = this.siteUrl + endpoint;

        String responseBody = this.doGetRequest(getURL);
        if (responseBody == null) {
            throw new APIRequestFailedException("Request to " + getURL + " failed");
        }

        // Parse the response, each command will be in a "commands" array
        try {
            return gson.fromJson(responseBody, Commands.class);
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Failed to parse response: " + responseBody + " | Error: " + e.getMessage());
            throw new APIRequestFailedException("Failed to parse response.");
        }
    }

    public void deleteCommand(int commandId) throws APIRequestFailedException {
        String endpoint = "commands/" + commandId;
        String deleteURL = this.siteUrl + endpoint;

        String response = this.doDeleteRequest(deleteURL);
        if (response == null) {
            throw new APIRequestFailedException("Request to " + deleteURL + " failed");
        }

        // Just do a dirty check to see if "success" is true
        JsonReader jr = new JsonReader(new java.io.StringReader(response));
        JsonElement jp = JsonParser.parseReader(jr);
        if (jp.isJsonObject()) {
            JsonObject jo = jp.getAsJsonObject();
            if (jo.has("success")) {
                if (!jo.get("success").getAsBoolean()) {
                    throw new APIRequestFailedException("Request to " + deleteURL + " failed. Response: " + response);
                }
            }
        }
    }


}
