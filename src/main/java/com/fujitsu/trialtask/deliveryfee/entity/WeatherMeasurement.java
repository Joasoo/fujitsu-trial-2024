package com.fujitsu.trialtask.deliveryfee.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "weather_measurement")
public class WeatherMeasurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "timestamp")
    private Timestamp timestamp;

    @ManyToOne
    @JoinColumn(name = "weather_station_wmo_code")
    private WeatherStation weatherStation;

    @Column(name = "air_temperature")
    private Float airTemperature;

    @Column(name = "wind_speed")
    private Float windSpeed;

    @Column(name = "phenomenon")
    private String phenomenon;

    @Override
    public String toString() {
        return new StringBuilder().append("----- Weather Measurement -----\n")
                .append("ID: ").append(id).append("\n")
                .append("Timestamp: ").append(timestamp).append("\n")
                .append("Name: ").append(weatherStation.getName()).append("\n")
                .append("WMO: ").append(weatherStation.getWMOcode()).append("\n")
                .append("Air temperature: ").append(airTemperature).append("\n")
                .append("Wind Speed: ").append(windSpeed).append("\n")
                .append("Phenomenon: ").append(phenomenon)
                .toString();
    }
}
