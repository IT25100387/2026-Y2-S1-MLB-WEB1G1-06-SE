package com.fuelstation.service;

import com.fuelstation.repository.GatewayRefundAttemptRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DataJpaTest(showSql=false,properties={"spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({PayHereRefundService.class,GatewayRefundTransactionTest.Config.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class GatewayRefundTransactionTest {
    @TestConfiguration static class Config { @Bean Clock clock(){return Clock.systemUTC();} }
    @MockBean PayHereRefundClient gateway;
    @Autowired PayHereRefundService refunds;
    @Autowired GatewayRefundAttemptRepository attempts;
    @Autowired PlatformTransactionManager manager;
    @BeforeEach void setup(){attempts.deleteAll();when(gateway.accessToken()).thenReturn("token");when(gateway.refund(anyString(),anyString(),anyDouble(),anyString())).thenReturn("refund-100");}
    @Test void gatewayConfirmationSurvivesLedgerRollbackAndIsAppliedOnceOnRecovery(){
        TransactionTemplate ledger=new TransactionTemplate(manager);
        assertThrows(IllegalStateException.class,()->ledger.execute(status->{refunds.refund(1L,"payment-100",25.50,"Returned item","request-one");throw new IllegalStateException("Ledger write failed");}));
        assertEquals("CONFIRMED",attempts.findAll().get(0).getState());
        assertEquals("refund-100",ledger.execute(status->refunds.refund(1L,"payment-100",25.50,"Returned item","request-one")));
        assertEquals("APPLIED",attempts.findAll().get(0).getState());verify(gateway,times(1)).refund("token","payment-100",25.50,"Returned item");
    }
    @Test void timeoutLeavesADurableReviewRecordAfterRollbackAndBlocksAnotherRequest(){
        when(gateway.refund(anyString(),anyString(),anyDouble(),anyString())).thenThrow(new IllegalStateException("Timeout"));TransactionTemplate ledger=new TransactionTemplate(manager);
        assertThrows(IllegalStateException.class,()->ledger.execute(status->refunds.refund(1L,"payment-100",25.50,"Returned item","request-one")));
        assertEquals("REVIEW",attempts.findAll().get(0).getState());assertThrows(IllegalStateException.class,()->ledger.execute(status->refunds.refund(1L,"payment-100",25.50,"Returned item","request-two")));verify(gateway,times(1)).refund(anyString(),anyString(),anyDouble(),anyString());
    }
}
