package com.example.container1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
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
    private static final String CONTAINER_2_URL = "http://container-2-service:8000/process";

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
    public ResponseEntity<Map<String, Object>> calculate(@RequestBody Map<String, String> input) {
        Map<String, Object> response = new HashMap<>();

        // Validate input
        if (!input.containsKey("file") || input.get("file") == null || input.get("file").trim().isEmpty()) {
            response.put("file", null);
            response.put("error", "Invalid JSON input.");
            return ResponseEntity.badRequest().body(response);
        }

        String fileName = input.get("file");
        String product = input.get("product");

        File file = new File(STORAGE_DIR, fileName);
        if (!file.exists()) {
            response.put("file", fileName);
            response.put("error", "File not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        String fileContent;
        try {
            fileContent = Files.readString(file.toPath());
        } catch (IOException e) {
            response.put("file", fileName);
            response.put("error", "Error reading file.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        Map<String, String> request = new HashMap<>();
        request.put("file", fileName);
        request.put("product", product);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<Map> container2Response = restTemplate.postForEntity(CONTAINER_2_URL, request, Map.class);
            Map<String, Object> result = container2Response.getBody();

            if (container2Response.getStatusCode() == HttpStatus.OK && result != null && result.containsKey("sum")) {
                response.put("file", fileName);
                response.put("sum", result.get("sum"));
                return ResponseEntity.ok(response);
            } else if (result != null && result.containsKey("error")) {
                response.put("file", fileName);
                response.put("error", "Input file not in CSV format..");
                return ResponseEntity.badRequest().body(response);
            } else {
                response.put("file", fileName);
                response.put("error", "Unexpected response from processing service.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            response.put("file", fileName);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}