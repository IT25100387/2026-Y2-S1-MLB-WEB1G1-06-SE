package com.fuelstation.repository;

import com.fuelstation.model.FuelSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuelSaleRepository extends JpaRepository<FuelSale, Long> {

    Optional<FuelSale> findByReceiptNumber(String receiptNumber);

    List<FuelSale> findByCustomerUsernameOrderBySaleDateDesc(String customerUsername);

    List<FuelSale> findByVehicleNumberInOrderBySaleDateDesc(List<String> vehicleNumbers);

    List<FuelSale> findAllByOrderBySaleDateDesc();

    @Query("SELECT s FROM FuelSale s WHERE " +
           "(s.customerUsername IS NOT NULL AND LOWER(s.customerUsername) = LOWER(:username)) OR " +
           "(:hasPlates = true AND s.vehicleNumber IS NOT NULL AND s.vehicleNumber IN :vehicleNumbers) OR " +
           "(s.customerName IS NOT NULL AND LOWER(s.customerName) = LOWER(:displayName)) " +
           "ORDER BY s.saleDate DESC")
    List<FuelSale> findCustomerPurchases(
            @Param("username") String username,
            @Param("displayName") String displayName,
            @Param("hasPlates") boolean hasPlates,
            @Param("vehicleNumbers") List<String> vehicleNumbers
    );
}
