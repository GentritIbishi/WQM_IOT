import { Component } from '@angular/core';
import {SensorData, SensorDataService} from "../services/sensor-data.service";

@Component({
  selector: 'app-ai-risk-alerts',
  templateUrl: './ai-risk-alerts.component.html',
  styleUrl: './ai-risk-alerts.component.scss'
})
export class AiRiskAlertsComponent {
  sensorData: SensorData[] = [];
  prediction: number = 0; // Default prediction value

  constructor(private sensorDataService: SensorDataService) { }

  ngOnInit(): void {
    // Automatically fetch data with prediction = 2
    this.fetchSensorDataByPrediction(2);
  }

  fetchSensorDataByPrediction(prediction: number): void {
    this.sensorDataService.getSensorDataByPrediction(prediction).subscribe({
      next: data => {
        this.sensorData = data;
      },
      error: err => {
        console.error('Error fetching sensor data by prediction:', err);
      }
    });
  }

}
