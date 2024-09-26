// Updated code with analysis and optimization

package com.gentritibishi;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.json.JSONObject;
import scala.Tuple2;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class SensorDataStreamProcessor {

    public static final double Z_SCORE_THRESHOLD = 3.0;  // Threshold for anomaly detection
    public static String path = "src/main/resources/AI_WQM_Dataset.csv";

    public static void main(String[] args) throws Exception {

        // AI

        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(path));
        Instances data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);

        NumericToNominal convert = new NumericToNominal();
        String[] options = new String[]{"-R", Integer.toString(data.classIndex() + 1)};
        convert.setOptions(options);
        convert.setInputFormat(data);
        Instances newData = Filter.useFilter(data, convert);

        // Logistic Regression classifier
        Logistic logistic = new Logistic();
        logistic.buildClassifier(newData);

        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("temperature"));
        attributes.add(new Attribute("ph"));
        attributes.add(new Attribute("flow"));
        attributes.add(new Attribute("turbidity"));

        ArrayList<String> predictions = new ArrayList<>();
        predictions.add("Not Drinkable");
        predictions.add("Drinkable");
        attributes.add(new Attribute("prediction", predictions));

        Instances testDataset = new Instances("TestInstances", attributes, 0);
        testDataset.setClassIndex(testDataset.numAttributes() - 1);

        // Configure Spark
        SparkConf conf = new SparkConf().setAppName("SensorDataProcessor")
                .setMaster("local[4]")  // Set for 4 cores for higher parallelism
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")  // Kryo serialization for performance
                .set("spark.dynamicAllocation.enabled", "true")  // Enable dynamic resource allocation
                .set("spark.dynamicAllocation.minExecutors", "1")
                .set("spark.dynamicAllocation.maxExecutors", "10");

        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(5)); // Lower latency by reducing batch interval

        // Configure Kafka
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", "localhost:9092");
        kafkaParams.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaParams.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaParams.put("group.id", "sensor-data-group");
        kafkaParams.put("auto.offset.reset", "latest");
        kafkaParams.put("enable.auto.commit", "false");

        Set<String> topics = Collections.singleton("SensorTopic");

        JavaInputDStream<ConsumerRecord<String, String>> stream =
                KafkaUtils.createDirectStream(
                        jssc,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams)
                );

        // Define schema for DataFrame
        StructType schema = new StructType(new StructField[]{
                new StructField("id", DataTypes.StringType, false, Metadata.empty()),
                new StructField("temperature", DataTypes.StringType, false, Metadata.empty()),
                new StructField("ph", DataTypes.DoubleType, false, Metadata.empty()),
                new StructField("flow", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("turbidity", DataTypes.DoubleType, false, Metadata.empty()),
                new StructField("latitude", DataTypes.DoubleType, false, Metadata.empty()),
                new StructField("longitude", DataTypes.DoubleType, false, Metadata.empty()),
                new StructField("address", DataTypes.StringType, false, Metadata.empty()),
                new StructField("timestamp", DataTypes.TimestampType, false, Metadata.empty()),
                new StructField("prediction", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("time_execution", DataTypes.LongType, false, Metadata.empty())  // Execution time
        });

        // Memory and JVM Metrics
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        // Stream processing
        stream.foreachRDD(rdd -> {
            if (!rdd.isEmpty()) {
                long batchStartTime = System.currentTimeMillis();  // Record batch processing start time

                // Get memory usage before processing
                MemoryUsage heapMemoryUsageBefore = memoryMXBean.getHeapMemoryUsage();
                long usedMemoryBefore = heapMemoryUsageBefore.getUsed();

                SparkSession spark = SparkSession.builder().config(rdd.context().getConf()).getOrCreate();
                long recordsProcessed = rdd.count();  // Count records

                List<Row> rows = rdd.map(record -> {
                    long receiveTime = System.currentTimeMillis();  // Record receive time

                    try {
                        // JSON parsing logic
                        System.out.println("Checking: " + record.value());
                        JSONObject json = new JSONObject("{" + record.value() + "}");
                        String temperatureStr = json.getString("temperature").replaceAll(" Celsius", "").trim();
                        double temperature = Double.parseDouble(temperatureStr);
                        double ph = json.getDouble("ph");
                        Integer flow = json.getInt("flow");
                        double turbidity = json.getDouble("turbidity");
                        double latitude = json.getDouble("latitude");
                        double longitude = json.getDouble("longitude");
                        int prediction = 0;  // Default prediction
                        String address = getAddress(latitude, longitude);

                        // AI
                        double[] values = {temperature, ph, flow, turbidity};
                        testDataset.add(new DenseInstance(1.0, values));

                        double[] means = new double[attributes.size() - 1];
                        double[] stdDevs = new double[attributes.size() - 1];
                        calculateMeansAndStdDevs(newData, means, stdDevs);

                        for (int i = 0; i < testDataset.numInstances(); i++) {
                            Instance instance = testDataset.instance(i);
                            if (isAnomaly(instance, means, stdDevs)) {
                                prediction = 2;  // Anomaly detected
                            } else {
                                prediction = (int) logistic.classifyInstance(instance);  // Classify
                            }
                        }

                        updateCSV(temperature, ph, flow, turbidity, prediction);  // Update CSV

                        long totalExecutionTime = receiveTime - batchStartTime;  // Calculate execution time
                        return RowFactory.create(
                                UUID.randomUUID().toString(),
                                temperatureStr,
                                ph,
                                flow,
                                turbidity,
                                latitude,
                                longitude,
                                address,
                                new Timestamp(System.currentTimeMillis()),
                                prediction,
                                totalExecutionTime
                        );
                    } catch (Exception e) {
                        System.out.println("Error processing record: " + e.getMessage());
                        return null;
                    }
                }).filter(Objects::nonNull).collect();

                if (!rows.isEmpty()) {
                    Dataset<Row> dataFrame = spark.createDataFrame(rows, schema);
                    dataFrame.write()
                            .format("org.apache.spark.sql.cassandra")
                            .option("keyspace", "sensor_data")
                            .option("table", "readings")
                            .mode(SaveMode.Append)
                            .save();

                    System.out.println("Saved " + rows.size() + " records to Cassandra");
                }

                long batchEndTime = System.currentTimeMillis();
                long processingTime = batchEndTime - batchStartTime;
                double throughput = (double) recordsProcessed / (processingTime / 1000.0);  // Throughput

                // Get memory usage after processing
                MemoryUsage heapMemoryUsageAfter = memoryMXBean.getHeapMemoryUsage();
                long usedMemoryAfter = heapMemoryUsageAfter.getUsed();
                long memoryUsedInBatch = usedMemoryAfter - usedMemoryBefore;

                // Log performance metrics
                System.out.println("Batch Processing Time (ms): " + processingTime);
                System.out.println("Throughput (records/second): " + throughput);
                System.out.println("Memory Used (bytes): " + memoryUsedInBatch);
            } else {
                System.out.println("Received empty RDD");
            }
        });

        jssc.start();
        jssc.awaitTermination();
    }

    private static void calculateMeansAndStdDevs(Instances data, double[] means, double[] stdDevs) {
        for (int i = 0; i < data.numAttributes() - 1; i++) {
            means[i] = data.meanOrMode(i);
            stdDevs[i] = Math.sqrt(data.variance(i));
        }
    }

    private static boolean isAnomaly(Instance instance, double[] means, double[] stdDevs) {
        for (int i = 0; i < instance.numAttributes() - 1; i++) {
            double value = instance.value(i);
            double zScore = (value - means[i]) / stdDevs[i];
            if (zScore > Z_SCORE_THRESHOLD || zScore < -Z_SCORE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update the CSV file with the new sensor data for the next prediction.
     * This function appends a new record to the existing CSV dataset.
     *
     * @param temperature the temperature value to append
     * @param ph the pH value to append
     * @param flow the flow value to append
     * @param turbidity the turbidity value to append
     * @param prediction the label indicating the class (0 = Not Drinkable, 1 = Drinkable)
     */
    public static void updateCSV(double temperature, double ph, int flow, double turbidity, int prediction) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {
            String newRecord = String.format("%f,%f,%d,%f,%d", temperature, ph, flow, turbidity, prediction);
            writer.write(newRecord);
            writer.newLine();
            writer.flush();
            System.out.println("CSV file updated with new record: " + newRecord);
        } catch (IOException e) {
            System.out.println("Error updating CSV file: " + e.getMessage());
        }
    }

    public static String getAddress(double latitude, double longitude) {
        if (latitude == 42.619447 && longitude == 21.234465) {
            return "Liqeni I Badovcit";
        } else if (latitude == 42.821412 && longitude == 21.308285) {
            return "Liqeni I Batllaves";
        } else if (latitude == 42.961875 && longitude == 20.570339) {
            return "Liqeni I Ujmanit";
        } else if (latitude == 42.486777 && longitude == 20.422350) {
            return "Liqeni I Radoniqit";
        } else {
            return "Unknown";
        }
    }
}
