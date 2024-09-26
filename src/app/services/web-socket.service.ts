// web-socket.service.ts
import { Injectable } from '@angular/core';
import { Stomp } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: any;
  private messageSubject: Subject<any> = new Subject<any>();

  constructor() {
    this.connect();
  }

  private connect() {
    const socket = new SockJS('http://localhost:8080/notifications'); // Use SockJS endpoint
    this.stompClient = Stomp.over(socket);

    this.stompClient.connect({}, (frame: any) => {
      console.log('Connected: ' + frame);

      // Subscribe to the topic you are using in the backend
      this.stompClient.subscribe('/topic/anomalies', (message: any) => {
        this.messageSubject.next(JSON.parse(message.body));
      });
    }, (error: any) => {
      console.error('WebSocket connection error:', error);
    });
  }

  // Expose the observable to subscribe to messages
  get messages$(): Observable<any> {
    return this.messageSubject.asObservable();
  }
}
