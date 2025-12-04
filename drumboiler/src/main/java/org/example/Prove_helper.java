package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lightweight copy of the HTTP prover helper used in other modules.
 * Talks to a running KeYmaeraX server (default http://localhost:8090).
 */
public class Prove_helper {
    static ObjectMapper objectMapper = new ObjectMapper();
    static final Logger logger = LoggerFactory.getLogger(Prove_helper.class);

    public static String prove(String proofRequest) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8090/user/local/modelupload/undefined"))
                    .POST(HttpRequest.BodyPublishers.ofString(proofRequest))
                    .build();
            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response;
            try {
                response = client.send(request, BodyHandlers.ofString());
            } catch (IOException e) {
                logger.warn("Exception occurred connecting to KeYmaeraX: {}", e.getClass());
                throw e;
            }
            if (response.statusCode()>=400)
                throw new RuntimeException("Received error response from KeYmaeraX. " + response.toString());
            String body = response.body();
            JsonNode json = objectMapper.readTree(body);
            if (json.get("type") != null && json.get("type").asText().equals("error"))
                throw new RuntimeException(String.format("Received error from KeYmaeraX: %1s", body));
            String modelId = json.get("modelId").asText();

            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8090/models/users/local/model/"+modelId+"/createProof"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"proofDescription\":\"\", \"proofName\":\"\"}"))
                    .build();
            response = HttpClient.newBuilder()
                    .build()
                    .send(request, BodyHandlers.ofString());
            body = response.body();
            json = objectMapper.readTree(body);
            String id = json.get("id").asText();

            // seems necessary but response is irrelevant
            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8090/proofs/user/local/" + id))
                    .GET()
                    .build();
            response = HttpClient.newBuilder()
                    .build()
                    .send(request, BodyHandlers.ofString());

            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8090/proofs/user/local/" + id + "/()/doCustomTactic?stepwise=false"))
                    .POST(HttpRequest.BodyPublishers.ofString("auto"))
                    .build();
            response = HttpClient.newBuilder()
                    .build()
                    .send(request, BodyHandlers.ofString());
            body = response.body();
            json = objectMapper.readTree(body);
            String taskId = json.get("taskId").asText();

            String status = "";
            do {
                request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:8090/proofs/user/local/" + id + "/()/" + taskId + "/status"))
                        .GET()
                        .build();
                response = HttpClient.newBuilder()
                        .build()
                        .send(request, BodyHandlers.ofString());
                body = response.body();
                json = objectMapper.readTree(body);
                status = json.get("status").asText();
                if (!status.equals("done")) {
                    TimeUnit.SECONDS.sleep(1);
                }
            } while (!status.equals("done"));

            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8090/proofs/user/local/" + id + "/()/" + taskId + "/result"))
                    .GET()
                    .build();
            response = HttpClient.newBuilder()
                    .build()
                    .send(request, BodyHandlers.ofString());
            body = response.body();
            json = objectMapper.readTree(body);
            if (json.get("newNodes") != null && json.get("newNodes").has(0)) {
                String succedent = json.get("newNodes").elements().next().get("sequent").get("succ").elements().next().get("formula").get("json").get("plain").asText();
                if (succedent.equals("false")) {
                    return "false";
                } else {
                    return "unknown";
                }
            } else {
                return "true";
            }


        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return "unknown";
    }
}
