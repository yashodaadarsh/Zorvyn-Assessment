package com.adarsh.zorvyn.Request;

import com.adarsh.zorvyn.Entity.Type;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.Date;

@Data
public class RecordRequest {

    @Positive(message = "Amount must be greater than zero")
    private double amount;

    @NotNull(message = "Type is required")
    private Type type;

    @NotBlank(message = "Category is required")
    private String category;

    private Date date;

    private String note;
}