package com.fujitsu.trialtask.deliveryfee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "vehicle")
public class Vehicle {
    @Id
    @Column(name = "id")
    Long id;

    @Column(name = "type")
    String type;
}
