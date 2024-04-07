package com.example.remotechunkingsb.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class Student implements Serializable {
    private int id;
    private String name;
    private String department;
    private int age;


}
