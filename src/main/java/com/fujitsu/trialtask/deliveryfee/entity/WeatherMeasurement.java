package com.fujitsu.trialtask.deliveryfee.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @Column(name = "station_name")
    private String stationName;

    @Column(name = "wmo_code")
    private Integer WMOCode;

    @Column(name = "air_temperature")
    private Float airTemperature;

    @Column(name = "wind_speed")
    private Float windSpeed;

    @Column(name = "phenomenon")
    private String phenomenon;
}
