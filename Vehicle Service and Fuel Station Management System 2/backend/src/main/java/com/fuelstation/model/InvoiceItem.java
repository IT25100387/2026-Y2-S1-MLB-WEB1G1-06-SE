package com.fuelstation.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class InvoiceItem {
    private String itemName;
    private Long partId;
    private Integer quantity;
    private Double unitPrice;
    private Double totalAmount;
}
