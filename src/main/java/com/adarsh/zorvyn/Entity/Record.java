package com.adarsh.zorvyn.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Data
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private double amount;
    @Enumerated(EnumType.STRING)
    private Type type;
    private String category;
    private Date date;
    private String note;
    @ManyToOne
    private User createdBy;

}
