package com.example.container1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
public class HelloController {

    private static final Logger LOGGER = Logger.getLogger(HelloController.class.getName());
    private static final String STORAGE_DIR = "/anupam_PV_dir";
    private static final String CONTAINER_2_URL = "http://container2:8000/process";

    @PostMapping("/store-file")
    public ResponseEntity<Map<String, Object>> storeFile(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        if (request == null || !request.containsKey("file") || !request.containsKey("data")) {
            response.put("file", null);
            response.put("error", "Invalid JSON input.");
            return ResponseEntity.badRequest().body(response);
        }

        String fileName = request.get("file");
        String content = request.get("data");

        LOGGER.info("B00990335_Anupam");

        if (fileName == null || fileName.trim().isEmpty()) {
            response.put("file", null);
            response.put("error", "Invalid JSON input.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Path directory = Path.of(STORAGE_DIR);
            Files.createDirectories(directory);

            Path filePath = directory.resolve(fileName);
            Files.writeString(filePath, content);

            response.put("file", fileName);
            response.put("message", "Success.");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("file", fileName);
            response.put("error", "Error while storing the file to the storage.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculate(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        if (request == null || !request.containsKey("file") || !request.containsKey("product")) {
            response.put("file", null);
            response.put("error", "Invalid JSON input.");
            return ResponseEntity.badRequest().body(response);
        }

        String fileName = request.get("file");
        String product = request.get("product");

        RestTemplate restTemplate = new RestTemplate();
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("file", fileName);
            requestBody.put("product", product);

            ResponseEntity<Map> container2Response = restTemplate.postForEntity(CONTAINER_2_URL, requestBody,
                    Map.class);
            return ResponseEntity.ok(container2Response.getBody());
        } catch (HttpClientErrorException.BadRequest e) {
            response.put("file", fileName);
            response.put("error", "Invalid request format.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("file", fileName);
            response.put("error", "Error processing request.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
