package com.gentritibishi.waterqualitymonitoringbackend.services;

import com.gentritibishi.waterqualitymonitoringbackend.entities.SensorData;
import com.gentritibishi.waterqualitymonitoringbackend.repos.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SensorDataService {
    @Autowired
    private SensorDataRepository sensorDataRepository;

    public List<SensorData> findAll() {
        return sensorDataRepository.findAll();
    }

    public List<SensorData> getLatestDistinctSensorData() {
        // Fetch all readings
        List<SensorData> allReadings = sensorDataRepository.findAll();

        // Stream to filter distinct latitude and longitude, and get the latest by timestamp

        return allReadings.stream()
                .collect(Collectors.groupingBy(
                        reading -> Arrays.asList(reading.getLatitude(), reading.getLongitude()),
                        Collectors.maxBy(Comparator.comparing(SensorData::getTimestamp))
                ))
                .values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<SensorData> getSensorDataByPrediction(int prediction) {
        return sensorDataRepository.findSensorDataByPrediction(prediction);
    }

    public List<SensorData> getSensorDataByLocation(double latitude, double longitude) {
        return sensorDataRepository.findLatestSensorDataByLocation(latitude, longitude);
    }

    public List<SensorData> getSensorDataByPhRange(double minPh, double maxPh) {
        // Assuming you have a repository that interacts with Cassandra
        return sensorDataRepository.findByPhBetween(minPh, maxPh);
    }

    public List<SensorData> getSensorDataByTemperatureRange(double minTemp, double maxTemp) {
        // Converting the temperature to a double if it's stored as a string
        return sensorDataRepository.findByTemperatureBetween(minTemp, maxTemp);
    }

    public List<SensorData> getSensorDataByFlowRange(int minFlow, int maxFlow) {
        return sensorDataRepository.findByFlowBetween(minFlow, maxFlow);
    }

    public List<SensorData> getSensorDataByDateRange(Instant startDate, Instant endDate) {
        return sensorDataRepository.findByTimestampBetween(startDate, endDate);
    }

}

