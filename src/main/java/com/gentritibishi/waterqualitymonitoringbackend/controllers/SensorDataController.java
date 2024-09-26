package com.gentritibishi.waterqualitymonitoringbackend.controllers;

import com.gentritibishi.waterqualitymonitoringbackend.entities.SensorData;
import com.gentritibishi.waterqualitymonitoringbackend.helpers.Functions;
import com.gentritibishi.waterqualitymonitoringbackend.services.EmailSenderService;
import com.gentritibishi.waterqualitymonitoringbackend.services.SensorDataService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.gentritibishi.waterqualitymonitoringbackend.helpers.Constants.LOCATIONS;
import static com.gentritibishi.waterqualitymonitoringbackend.helpers.Functions.*;

@RestController
@RequestMapping("/api/sensors")
public class SensorDataController {

    @Autowired
    private SensorDataService sensorDataService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmailSenderService senderService;

    // Use a thread-safe set to keep track of sent anomalies
    private final Set<SensorData> sentAnomalies = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @GetMapping
    public ResponseEntity<List<SensorData>> getAllSensorData() {
        return ResponseEntity.ok(sensorDataService.findAll());
    }

    @GetMapping("/latest/distinct")
    public ResponseEntity<List<SensorData>> getLatestDistinctSensorData() {
        List<SensorData> latestDistinctSensorData = sensorDataService.getLatestDistinctSensorData();
        return ResponseEntity.ok(latestDistinctSensorData);
    }

    @GetMapping("/byPrediction")
    public ResponseEntity<List<SensorData>> getSensorDataByPrediction(@RequestParam int prediction)  {
        List<SensorData> sensorData = sensorDataService.getSensorDataByPrediction(prediction);
        return ResponseEntity.ok(sensorData);
    }

    @GetMapping("/byLocation")
    public ResponseEntity<List<SensorData>> getSensorDataByLocation(@RequestParam double latitude, @RequestParam double longitude) {
        List<SensorData> sensorData = sensorDataService.getSensorDataByLocation(latitude, longitude);
        return ResponseEntity.ok(sensorData);
    }

    // Periodically check for anomalies and send notifications
    @Scheduled(fixedRate = 5000) // Check every 5 seconds
    public void checkForAnomalies() {
        List<SensorData> anomalies = sensorDataService.getSensorDataByPrediction(2); // Find anomalies where prediction = 2

        // Filter out already sent anomalies
        List<SensorData> newAnomalies = anomalies.stream()
                .filter(anomaly -> !sentAnomalies.contains(anomaly))
                .collect(Collectors.toList());

        if (!newAnomalies.isEmpty()) {
            // Send anomaly notification to clients
            try {
                for (SensorData anomaly : newAnomalies) {
                    triggerMail(anomaly);  // Trigger mail with formatted data
                }
            } catch (MessageHandlingException e) {
                String message = e.getMessage();
                System.out.println(message);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
            messagingTemplate.convertAndSend("/topic/anomalies", newAnomalies);

            // Add the new anomalies to the sent set
            sentAnomalies.addAll(newAnomalies);
        }
    }

    public void triggerMail(SensorData sensorData) throws MessagingException {
        String formattedBody = String.format(
                "<html>" +
                        "<body>" +
                        "<div style='font-family: Arial, sans-serif; color: #333;'>" +
                        "<h2 style='color: #4CAF50;'>Anomaly Detected in Water Quality Monitoring System</h2>" +
                        "<p style='font-size: 16px;'>The following anomaly has been detected:</p>" +
                        "<table style='width: 100%%; border-collapse: collapse; font-size: 14px;'>" +
                        "<tr style='background-color: #f2f2f2;'>" +
                        "<th style='border: 1px solid #ddd; padding: 8px;'>Field</th>" +
                        "<th style='border: 1px solid #ddd; padding: 8px;'>Value</th>" +
                        "</tr>" +
                        "<tr><td style='border: 1px solid #ddd; padding: 8px;'><b>ID</b></td><td style='border: 1px solid #ddd; padding: 8px;'>%s</td></tr>" +
                        "<tr><td style='border: 1px solid #ddd; padding: 8px;'><b>Temperature</b></td><td style='border: 1px solid #ddd; padding: 8px;'>%s</td></tr>" +
                        "<tr><td style='border: 1px solid #ddd; padding: 8px;'><b>pH Level</b></td><td style='border: 1px solid #ddd; padding: 8px;'>%.2f</td></tr>" +
                        "<tr><td style='border: 1px solid #ddd; padding: 8px;'><b>Flow Rate</b></td><td style='border: 1px solid #ddd; padding: 8px;'>%d</td></tr>" +
                        "<tr><td style='border: 1px solid #ddd; padding: 8px;'><b>Turbidity</b></td><td style='border: 1px solid #ddd; padding: 8px;'>%.2f</td></tr>" +
                        "<tr><td style='border: 1px solid #ddd; padding: 8px;'><b>Latitude</b></td><td style='border: 1px solid #ddd; padding: 8px;'>%.6f</td></tr>" +
                        "<tr><td style='border: 1px solid #ddd; padding: 8px;'><b>Longitude</b></td><td style='border: 1px solid #ddd; padding: 8px;'>%.6f</td></tr>" +
                        "<tr><td style='border: 1px solid #ddd; padding: 8px;'><b>Address</b></td><td style='border: 1px solid #ddd; padding: 8px;'>%s</td></tr>" +
                        "<tr><td style='border: 1px solid #ddd; padding: 8px;'><b>Timestamp</b></td><td style='border: 1px solid #ddd; padding: 8px;'>%s</td></tr>" +
                        "</table>" +
                        "<p style='font-size: 14px;'>Please take the necessary action.</p>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                sensorData.getId().toString(),
                sensorData.getTemperature(),
                sensorData.getPh(),
                sensorData.getFlow(),
                sensorData.getTurbidity(),
                sensorData.getLatitude(),
                sensorData.getLongitude(),
                sensorData.getAddress(),
                sensorData.getTimestamp().toString(),
                sensorData.getPrediction()
        );

        // Send the email
        senderService.sendHtmlEmail("gentritibishi@gmail.com",
                "WQM AI Detection Anomaly",
                formattedBody);
    }

    // This method is triggered when the application is ready
    @EventListener(ApplicationReadyEvent.class)
    public void triggerMailOnStartup(ApplicationReadyEvent event) throws MessagingException {
        // HTML formatted email body
        String startupMessage = "<html>" +
                "<body>" +
                "<div style='font-family: Arial, sans-serif; color: #333; padding: 10px;'>" +
                "<h1 style='color: #4CAF50;'>Water Quality Monitoring System</h1>" +
                "<p style='font-size: 16px;'>Hello,</p>" +
                "<p style='font-size: 16px;'>We are happy to inform you that the <b>Water Quality Monitoring System</b> has started successfully.</p>" +
                "<div style='background-color: #f0f0f0; padding: 15px; margin: 20px 0; border-radius: 5px;'>" +
                "<p style='font-size: 14px; color: #555;'>Start Time: <b>" + java.time.Instant.now() + "</b></p>" +
                "</div>" +
                "<p style='font-size: 14px;'>You will now receive updates about any detected anomalies.</p>" +
                "<p style='font-size: 14px;'>Thank you for using our system!</p>" +
                "<hr>" +
                "<footer style='font-size: 12px; color: #777;'>Water Quality Monitoring System Team</footer>" +
                "</div>" +
                "</body>" +
                "</html>";

        // Send HTML email when the application starts
        senderService.sendHtmlEmail("gentritibishi@gmail.com",
                "WQM AI Monitoring System Started",
                startupMessage);
    }

    @GetMapping("/byPhRange")
    public ResponseEntity<List<SensorData>> getSensorDataByPhRange(@RequestParam double minPh, @RequestParam double maxPh) {
        List<SensorData> sensorData = sensorDataService.getSensorDataByPhRange(minPh, maxPh);
        return ResponseEntity.ok(sensorData);
    }

    @GetMapping("/byTemperatureRange")
    public ResponseEntity<List<SensorData>> getSensorDataByTemperatureRange(@RequestParam double minTemp, @RequestParam double maxTemp) {
        List<SensorData> sensorData = sensorDataService.getSensorDataByTemperatureRange(minTemp, maxTemp);
        return ResponseEntity.ok(sensorData);
    }

    @GetMapping("/byFlowRange")
    public ResponseEntity<List<SensorData>> getSensorDataByFlowRange(@RequestParam int minFlow, @RequestParam int maxFlow) {
        List<SensorData> sensorData = sensorDataService.getSensorDataByFlowRange(minFlow, maxFlow);
        return ResponseEntity.ok(sensorData);
    }

    @GetMapping("/byDateRange")
    public ResponseEntity<List<SensorData>> getSensorDataByDateRange(@RequestParam String startDate, @RequestParam String endDate) {
        List<SensorData> sensorData = sensorDataService.getSensorDataByDateRange(Instant.parse(startDate), Instant.parse(endDate));
        return ResponseEntity.ok(sensorData);
    }

}
