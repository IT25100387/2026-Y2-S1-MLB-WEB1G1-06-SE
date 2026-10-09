package com.fuelstation.repository;

import com.fuelstation.model.GatewayRefundAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GatewayRefundAttemptRepository extends JpaRepository<GatewayRefundAttempt,String> {
    List<GatewayRefundAttempt> findByPaymentId(Long paymentId);
}
