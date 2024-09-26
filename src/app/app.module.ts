import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BodyComponent } from './body/body.component';
import { SidenavComponent } from './sidenav/sidenav.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { StatisticsComponent } from './statistics/statistics.component';
import { AiRiskAlertsComponent } from './ai-risk-alerts/ai-risk-alerts.component';
import { HttpClientModule } from '@angular/common/http';
import { NgxEchartsModule } from 'ngx-echarts';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBarModule } from '@angular/material/snack-bar';

// Correct Angular Material module imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatCardModule } from '@angular/material/card';
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {LeafletModule} from "@asymmetrik/ngx-leaflet";
import {FormsModule} from "@angular/forms";
import { NgChartsModule } from 'ng2-charts'; // Import NgChartsModule
import { AlertModule } from '@coreui/angular';

@NgModule({
  declarations: [
    AppComponent,
    BodyComponent,
    SidenavComponent,
    DashboardComponent,
    StatisticsComponent,
    AiRiskAlertsComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    HttpClientModule,
    NgxEchartsModule.forRoot({echarts: () => import('echarts')}),

    // Angular Material modules
    MatToolbarModule,
    MatIconModule,
    MatGridListModule,
    MatCardModule,
    MatProgressSpinner,
    LeafletModule,
    FormsModule,
    NgChartsModule,
    MatSnackBarModule,
    AlertModule

  ],
  providers: [
    provideAnimationsAsync()
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
