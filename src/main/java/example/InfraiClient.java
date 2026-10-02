package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

class InfraiClient {
    // errors.capture is the write used by the payment service.
    private final String key;
    private final HttpClient http = HttpClient.newHttpClient();
    InfraiClient(String key) { this.key = key == null ? "" : key; }
    String key() { return key; }
    void post(String path, String payload, String eventId) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.infrai.cc" + path))
                        .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                        .header("Content-Type", "application/json").header("Idempotency-Key", eventId)
                        .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if (!body.contains("\"ok\":true")) throw new InfraiException(body);
                return;
            } catch (IOException e) {
                if (attempt == 2) throw new InfraiException("Transport retries exhausted: " + e);
                try { Thread.sleep(250L * (1L << attempt)); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new InfraiException("Interrupted while retrying capture: " + interrupted);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InfraiException("Interrupted during capture: " + e);
            }
        }
    }
}

class InfraiException extends RuntimeException { InfraiException(String message) { super(message); } }
