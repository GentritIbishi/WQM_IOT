package com.gentritibishi.waterqualitymonitoringbackend.repos;


import com.gentritibishi.waterqualitymonitoringbackend.entities.SensorData;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

public interface SensorDataRepository extends CassandraRepository<SensorData, UUID> {

    @Query(value = "SELECT * FROM readings WHERE prediction = ?0 ALLOW FILTERING")
    List<SensorData> findSensorDataByPrediction(int prediction);

    @Query(value = "SELECT * FROM readings WHERE latitude = ?0 AND longitude = ?1 ALLOW FILTERING")
    List<SensorData> findLatestSensorDataByLocation(double latitude, double longitude);

    List<SensorData> findByPhBetween(double minPh, double maxPh);

    List<SensorData> findByTemperatureBetween(double minTemp, double maxTemp);

    List<SensorData> findByFlowBetween(int minFlow, int maxFlow);

    List<SensorData> findByTimestampBetween(Instant startDate, Instant endDate);
}

