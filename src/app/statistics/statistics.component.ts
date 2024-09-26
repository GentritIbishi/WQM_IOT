import { Component, OnInit } from '@angular/core';
import { ChartData, ChartOptions } from 'chart.js';
import { SensorData, SensorDataService } from '../services/sensor-data.service';

@Component({
  selector: 'app-statistics',
  templateUrl: './statistics.component.html',
  styleUrls: ['./statistics.component.scss']
})
export class StatisticsComponent implements OnInit {
  // Locations data
  locations: { name: string, latitude: number, longitude: number }[] = [];

  // Pie chart properties
  pieChartData: ChartData<'pie'> = {
    labels: ['Drinkable', 'Not Drinkable'], // Updated labels
    datasets: [{ data: [] }]
  };
  hasPieChartData: boolean = true; // Flag to track pie chart data

  // Line chart properties
  lineChartData: ChartData<'line'> = {
    labels: [],
    datasets: []
  };
  hasLineChartData: boolean = true; // Flag to track line chart data

  lineChartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false
  };

  constructor(private sensorDataService: SensorDataService) {}

  ngOnInit(): void {
    // Fetch locations and initialize component
    this.fetchLocations();
  }

  // Function to get the location name by latitude and longitude
  getAddressByLatitudeAndLongitude(latitude: number, longitude: number): string {
    if (latitude === 42.619447 && longitude === 21.234465) {
      return "Liqeni I Badovcit";
    } else if (latitude === 42.821412 && longitude === 21.308285) {
      return "Liqeni I Batllaves";
    } else if (latitude === 42.961875 && longitude === 20.570339) {
      return "Liqeni I Ujmanit";
    } else if (latitude === 42.486777 && longitude === 20.422350) {
      return "Liqeni I Radoniqit";
    } else {
      return "Unknown";
    }
  }

  // Fetch all unique locations
  fetchLocations(): void {
    this.sensorDataService.getSensorData().subscribe(
      {
        next: (data: SensorData[]) => {
          // Extract unique locations from sensor data
          const uniqueLocations = Array.from(
            new Set(data.map((sensor) => `${sensor.latitude},${sensor.longitude}`))
          ).map((locationKey) => {
            const [latitude, longitude] = locationKey.split(',').map(Number);
            // Use getAddressByLatitudeAndLongitude to get the appropriate name
            const locationName = this.getAddressByLatitudeAndLongitude(latitude, longitude);

            return { name: locationName, latitude, longitude };
          });

          this.locations = uniqueLocations;

          // Automatically select the first location
          if (this.locations.length > 0) {
            this.onLocationSelect(this.locations[0]);
          }
        },
        error: (err) => {
          console.error('Error fetching locations:', err);
        }
      });
  }

  // Handle location selection
  onLocationSelect(location: { name: string, latitude: number, longitude: number }): void {
    this.fetchDataForLocation(location.latitude, location.longitude);
  }

  // Fetch data for the selected location
  fetchDataForLocation(latitude: number, longitude: number): void {
    this.sensorDataService.getSensorDataByLocation(latitude, longitude).subscribe({
      next: (data: SensorData[]) => {
        // Check if there is data for pie chart
        const drinkable = data.filter(d => d.prediction === 1).length;
        const notDrinkable = data.filter(d => d.prediction === 0).length;
        this.hasPieChartData = drinkable > 0 || notDrinkable > 0; // Check if there is any data
        this.pieChartData = {
          labels: ['Drinkable', 'Not Drinkable'],
          datasets: [{ data: this.hasPieChartData ? [drinkable, notDrinkable] : [] }]
        };

        // Check if there is data for line chart
        this.hasLineChartData = data.length > 0;
        this.lineChartData = this.hasLineChartData
          ? {
            labels: data.map(d => new Date(d.timestamp).toLocaleString()),
            datasets: [
              { data: data.map(d => +d.temperature), label: 'Temperature' },
              { data: data.map(d => d.ph), label: 'pH' },
              { data: data.map(d => d.turbidity), label: 'Turbidity' }
            ]
          }
          : { labels: [], datasets: [] };
      },
      error: (err) => {
        console.error('Error fetching sensor data by location:', err);
        this.hasPieChartData = false;
        this.hasLineChartData = false;
      }
    });
  }
}
