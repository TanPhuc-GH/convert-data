package com.tool.convertdata.storage.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "phone", indexes = @Index(name = "phone_index", columnList = "phone"))
public class Phone {
    @Id
    private Double id;
    private Double phone;
    private Integer status;
    private Integer gender;
    private Integer location;
}