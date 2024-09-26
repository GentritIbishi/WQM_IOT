import { Component, OnInit } from '@angular/core';
import { latLng, tileLayer, marker, icon, Marker } from 'leaflet';
import { Router } from '@angular/router';
import { SensorData, SensorDataService } from '../services/sensor-data.service';
import { WebSocketService } from '../services/web-socket.service';
import { ChartType, ChartData } from 'chart.js';
import * as L from 'leaflet';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  sensorData: SensorData[] = [];
  locations: any[] = [];
  isWaterQualityGood: boolean = true;
  totalSensors: number = 0;
  activeSensors: number = 0;
  totalLocations: number = 0; // Add a new variable to track total locations
  mapCenter = latLng(0, 0);
  mapZoom = 2;
  mapOptions = {
    layers: [
      tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '© OpenStreetMap contributors'
      })
    ],
    zoom: this.mapZoom,
    center: this.mapCenter
  };
  mapMarkers: Marker[] = [];
  leafletMap: L.Map | null = null; // Store the Leaflet map instance

  alerts: any[] = [];

  // Doughnut chart data for water purity
  public doughnutChartType: ChartType = 'doughnut'; // Define the chart type
  public doughnutChartData: ChartData<'doughnut'> = {
    labels: ['Drinkable', 'NOT Drinkable', 'Anomaly'], // Initial labels for the chart
    datasets: [
      {
        data: [0, 0, 0], // Placeholder for initial data
        backgroundColor: ['#4caf50', '#f44336', '#ffa500'], // Colors for the chart segments
      }
    ]
  };

  constructor(
    private sensorDataService: SensorDataService,
    private webSocketService: WebSocketService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.fetchSensorData();
    this.listenForAnomalies(); // Listen for anomalies
  }

  onMapReady(map: L.Map): void {
    this.leafletMap = map; // Save the Leaflet map instance when it is ready
  }

  fetchSensorData(): void {
    this.sensorDataService.getLatestSensorData().subscribe({
      next: data => {
        this.sensorData = data;
        this.processLocations();
        this.updateMapMarkers();
      },
      error: err => {
        console.error('Error fetching sensor data:', err);
      }
    });
  }

  processLocations(): void {
    const groupedLocations: Record<string, SensorData[]> = this.sensorData.reduce((acc: Record<string, SensorData[]>, data: SensorData) => {
      const key = `${data.latitude},${data.longitude}`;
      if (!acc[key]) {
        acc[key] = [];
      }
      acc[key].push(data);
      return acc;
    }, {});

    let drinkableCount = 0;
    let notDrinkableCount = 0;
    let anomalyCount = 0;

    this.locations = Object.keys(groupedLocations).map(key => {
      const locationData = groupedLocations[key];
      const [latitude, longitude] = key.split(',').map(Number);

      let qualityStatus: string;
      let color: string;

      // Calculate quality status
      if (locationData.every(reading => reading.prediction === 0)) {
        qualityStatus = 'NOT Drinkable';
        color = 'red';
        notDrinkableCount++;
      } else if (locationData.every(reading => reading.prediction === 1)) {
        qualityStatus = 'Drinkable';
        color = 'green';
        drinkableCount++;
      } else if (locationData.every(reading => reading.prediction === 2)) {
        qualityStatus = 'Anomaly';
        color = 'orange';
        anomalyCount++;
      } else {
        qualityStatus = 'Mixed';
        color = 'gray';
      }

      // Calculate active and total sensors for each location
      const totalLocationSensors = locationData.length * 4; // 4 types of sensors
      const activeLocationSensors = locationData.filter(reading =>
        reading.temperature && reading.ph && reading.flow && reading.turbidity
      ).length * 4;

      this.totalSensors += totalLocationSensors;
      this.activeSensors += totalLocationSensors;

      return {
        latitude,
        longitude,
        qualityStatus,
        color,
        readings: locationData
      };
    });

    this.isWaterQualityGood = this.locations.every(location => location.qualityStatus === 'Drinkable');

    // Update total locations
    this.totalLocations = this.locations.length;

    // Update the chart data based on the count of Drinkable, NOT Drinkable, and Anomaly locations
    this.updateDoughnutChartData(drinkableCount, notDrinkableCount, anomalyCount);
  }

  updateDoughnutChartData(drinkableCount: number, notDrinkableCount: number, anomalyCount: number): void {
    this.doughnutChartData.datasets[0].data = [drinkableCount, notDrinkableCount, anomalyCount];
  }

  updateMapMarkers(): void {
    const bounds = new L.LatLngBounds([]); // Initialize an empty bounds object

    this.mapMarkers = this.locations.map(location => {
      const markerInstance = marker([location.latitude, location.longitude], {
        icon: icon({
          iconSize: [25, 41],
          iconAnchor: [13, 41],
          iconUrl: location.qualityStatus === 'Drinkable'
            ? 'assets/greenMarker24x24.png'
            : location.qualityStatus === 'NOT Drinkable'
              ? 'assets/redMarker24x24.png'
              : 'assets/orangeMarker24x24.png'
        })
      }).bindPopup(`<b>Location (${location.latitude.toFixed(2)}, ${location.longitude.toFixed(2)})</b><br>
                    <b>Water Quality:</b> <span style="color: ${location.color}">${location.qualityStatus}</span><br>
                    <b>Temperature:</b> ${location.readings[0].temperature} °C<br>
                    <b>pH:</b> ${location.readings[0].ph}<br>
                    <b>Turbidity:</b> ${location.readings[0].turbidity}`);

      // Extend the bounds to include this marker's position
      bounds.extend([location.latitude, location.longitude]);

      return markerInstance;
    });

    // If there are locations and the map instance is ready, zoom to fit the bounds
    if (this.leafletMap && this.locations.length > 0) {
      this.leafletMap.fitBounds(bounds); // Fit the map to the bounds of the markers
    }
  }

  listenForAnomalies(): void {
    this.webSocketService.messages$.subscribe((message) => {
      try {
        const anomalyData = typeof message === 'string' ? JSON.parse(message) : message;
        console.log('Anomaly data received:', anomalyData);
        this.showAnomalyNotification(anomalyData); // Pass anomaly data to the notification function
      } catch (error) {
        console.error('Error parsing anomaly message:', error);
      }
    });
  }

  showAnomalyNotification(anomalyData: any): void {
    this.alerts.push({
      type: 'warning',
      message: `🚨 Anomaly Detected: ${anomalyData.length} new anomalies detected.`,
      dismissible: true,
      timeout: 10000,
      onClick: () => {
        // Navigate to the desired route when the alert is clicked
        this.router.navigate(['/airiskalerts']);
      }
    });
  }

  closeAlert(index: number): void {
    this.alerts.splice(index, 1); // Remove the alert at the specified index
  }
}
