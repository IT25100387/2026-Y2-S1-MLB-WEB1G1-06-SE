package com.fuelstation.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CashierSaleLine {
    private Long partId;
    private String itemName;
    private Integer quantity;
    private Double originalUnitPrice;
    private Double unitPrice;
    private Long usageId;
}
