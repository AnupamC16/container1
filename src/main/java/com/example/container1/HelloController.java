package com.example.container1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    private static final String MOUNTED_VOLUME_PATH = "/data"; // PV mount path
    private static final String CONTAINER_2_URL = "http://container2:8000/process"; // Container 2 service in GKE

    @PostMapping("/store-file")
    public ResponseEntity<Map<String, Object>> storeFile(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String fileName = request.get("file");
        String data = request.get("data");

        if (fileName == null || fileName.trim().isEmpty()) {
            response.put("file", null);
            response.put("error", "Invalid JSON input.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            File file = new File(MOUNTED_VOLUME_PATH, fileName);
            Files.writeString(file.toPath(), data);
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

        // Check file existence
        File file = new File(MOUNTED_VOLUME_PATH, fileName);
        if (!file.exists()) {
            response.put("file", fileName);
            response.put("error", "File not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Read file content
        String fileContent;
        try {
            fileContent = Files.readString(file.toPath());
        } catch (IOException e) {
            response.put("file", fileName);
            response.put("error", "Error reading file.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // Prepare request to Container 2
        Map<String, String> request = new HashMap<>();
        request.put("file_content", fileContent);
        request.put("product", product);

        // Call Container 2
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
                response.put("error", "input file not in CSV format.");
                return ResponseEntity.badRequest().body(response);
            } else {
                response.put("file", fileName);
                response.put("error", "Unexpected response from processing service.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            response.put("file", fileName);
            response.put("error", "Failed to communicate with processing service.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
