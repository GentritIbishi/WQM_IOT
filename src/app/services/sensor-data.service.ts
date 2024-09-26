import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SensorData {
  id: string;             // UUID of the sensor data
  temperature: string;    // Temperature as a string (or number, depending on your backend)
  ph: number;             // pH value as a number
  flow: number;           // Flow information as a string (e.g., "High flow", "Low flow")
  turbidity: number;      // Turbidity as a number
  latitude: number;       // Latitude of the sensor location
  longitude: number;      // Longitude of the sensor location
  address: string;      // Longitude of the sensor location
  timestamp: string;      // Timestamp as a string (use a Date object if you parse it)
  prediction: number;      // Prediction as a string (use a Date object if you parse it)
}


@Injectable({
  providedIn: 'root'
})
export class SensorDataService {
  private baseUrl = 'http://localhost:8080/api/sensors';

  constructor(private http: HttpClient) {}

  getSensorData(): Observable<SensorData[]> {
    return this.http.get<SensorData[]>(`${this.baseUrl}`);
  }

  getLatestSensorData(): Observable<SensorData[]> {
    return this.http.get<SensorData[]>(`${this.baseUrl}/latest/distinct`);
  }

  getSensorDataByPrediction(prediction: number): Observable<SensorData[]> {
    return this.http.get<SensorData[]>(`${this.baseUrl}/byPrediction?prediction=${prediction}`);
  }

  getSensorDataByLocation(latitude: number, longitude: number): Observable<SensorData[]> {
    return this.http.get<SensorData[]>(`${this.baseUrl}/byLocation?latitude=${latitude}&longitude=${longitude}`);
  }

}
