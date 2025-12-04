package org.example.drumboiler.proof;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Minimal KeYmaera X HTTP client, adapted from the Oven example's Prove_helper.
 * Assumes KeYmaera X server is running locally (default http://localhost:8090).
 */
public class KyxProver {
    private static final Logger LOGGER = LoggerFactory.getLogger(KyxProver.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BASE = "http://localhost:8090";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Submit a KyX model (content of a .kyx problem file) to KeYmaera X and return the outcome.
     *
     * @param kyxProblem textual content of a KyX problem
     * @return "true", "false", "unknown", or "error:..." depending on the KeYmaera X response
     */
    public String prove(String kyxProblem) {
        try {
            // 1) Upload model
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BASE + "/user/local/modelupload/undefined"))
                    .POST(HttpRequest.BodyPublishers.ofString(kyxProblem))
                    .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            LOGGER.debug("Upload response: {}", response);
            if (response.statusCode() >= 400) {
                return "error:upload-" + response.statusCode();
            }
            JsonNode json = OBJECT_MAPPER.readTree(response.body());
            if (json.get("type") != null && "error".equals(json.get("type").asText())) {
                return "error:upload-body-" + json.toString();
            }
            JsonNode modelIdNode = json.get("modelId");
            if (modelIdNode == null || modelIdNode.isNull()) {
                LOGGER.warn("No modelId in upload response: {}", response.body());
                return "error:upload-missing-modelId";
            }
            String modelId = modelIdNode.asText();

            // 2) Create proof
            request = HttpRequest.newBuilder()
                    .uri(new URI(BASE + "/models/users/local/model/" + modelId + "/createProof"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"proofDescription\":\"\",\"proofName\":\"\"}"))
                    .build();
            response = client.send(request, BodyHandlers.ofString());
            json = OBJECT_MAPPER.readTree(response.body());
            JsonNode proofIdNode = json.get("id");
            if (proofIdNode == null || proofIdNode.isNull()) {
                LOGGER.warn("No proof id in response: {}", response.body());
                return "error:create-proof-missing-id";
            }
            String proofId = proofIdNode.asText();

            // 3) Kick off auto tactic
            request = HttpRequest.newBuilder()
                    .uri(new URI(BASE + "/proofs/user/local/" + proofId + "/()/doCustomTactic?stepwise=false"))
                    .POST(HttpRequest.BodyPublishers.ofString("auto"))
                    .build();
            response = client.send(request, BodyHandlers.ofString());
            json = OBJECT_MAPPER.readTree(response.body());
            JsonNode taskNode = json.get("taskId");
            if (taskNode == null || taskNode.isNull()) {
                LOGGER.warn("No taskId in response: {}", response.body());
                return "error:tactic-missing-taskId";
            }
            String taskId = taskNode.asText();

            // 4) Poll status
            String status;
            do {
                request = HttpRequest.newBuilder()
                        .uri(new URI(BASE + "/proofs/user/local/" + proofId + "/()/" + taskId + "/status"))
                        .GET()
                        .build();
                response = client.send(request, BodyHandlers.ofString());
                json = OBJECT_MAPPER.readTree(response.body());
                status = json.get("status").asText();
                if (!"done".equals(status)) {
                    TimeUnit.SECONDS.sleep(1);
                }
            } while (!"done".equals(status));

            // 5) Fetch result
            request = HttpRequest.newBuilder()
                    .uri(new URI(BASE + "/proofs/user/local/" + proofId + "/()/" + taskId + "/result"))
                    .GET()
                    .build();
            response = client.send(request, BodyHandlers.ofString());
            json = OBJECT_MAPPER.readTree(response.body());
            if (json.get("newNodes") != null && json.get("newNodes").has(0)) {
                String succedent = json.get("newNodes").elements().next()
                        .get("sequent").get("succ").elements().next()
                        .get("formula").get("json").get("plain").asText();
                if ("false".equals(succedent)) {
                    return "false";
                } else {
                    return "unknown";
                }
            }
            return "true";
        } catch (URISyntaxException e) {
            LOGGER.warn("Bad URI for KeYmaera X endpoint", e);
            return "error:uri";
        } catch (IOException e) {
            LOGGER.warn("I/O error talking to KeYmaera X: {}", e.getMessage());
            return "error:io";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "error:interrupted";
        } catch (Exception e) {
            LOGGER.warn("Unexpected error talking to KeYmaera X: {}", e.getMessage());
            return "error:unexpected";
        }
    }
}
