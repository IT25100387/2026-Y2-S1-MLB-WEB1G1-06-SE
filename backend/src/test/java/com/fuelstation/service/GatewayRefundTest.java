package com.fuelstation.service;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.impl.BillingServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.Clock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GatewayRefundTest {
    GatewayRefundAttemptRepository attempts=mock(GatewayRefundAttemptRepository.class);
    PayHereRefundClient gateway=mock(PayHereRefundClient.class);
    PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
    Map<String,GatewayRefundAttempt> stored=new HashMap<>();
    PayHereRefundService refunds;
    @BeforeEach void setup(){
        when(manager.getTransaction(any())).thenAnswer(i->new SimpleTransactionStatus());
        refunds=new PayHereRefundService(attempts,gateway,manager,Clock.systemUTC());
        when(attempts.findById(anyString())).thenAnswer(i->Optional.ofNullable(stored.get(i.getArgument(0))));
        when(attempts.findByPaymentId(anyLong())).thenAnswer(i->stored.values().stream().filter(a->a.getPaymentId().equals(i.getArgument(0))).toList());
        when(attempts.saveAndFlush(any())).thenAnswer(i->{GatewayRefundAttempt a=i.getArgument(0);stored.put(a.getId(),a);return a;});
        when(gateway.accessToken()).thenReturn("token");when(gateway.refund(anyString(),anyString(),anyDouble(),anyString())).thenReturn("refund-100");
    }
    String refund(String key){return refunds.refund(1L,"payment-100",25.50,"Returned item",key);}
    @Test void confirmedGatewayRefundIsReusedAfterLedgerRollback(){
        assertEquals("refund-100",refund("request-one"));assertEquals("CONFIRMED",stored.values().iterator().next().getState());
        assertEquals("refund-100",refund("request-one"));verify(gateway,times(1)).refund("token","payment-100",25.50,"Returned item");
        assertThrows(IllegalStateException.class,()->refund("request-two"));verify(gateway,times(1)).refund(anyString(),anyString(),anyDouble(),anyString());
        assertThrows(IllegalStateException.class,()->refunds.requireNoUnresolvedRefund(1L));
    }
    @Test void uncertainRefundIsNotSentAgainUnderAnyKey(){
        when(gateway.refund(anyString(),anyString(),anyDouble(),anyString())).thenThrow(new IllegalStateException("Timeout"));
        assertThrows(IllegalStateException.class,()->refund("request-one"));assertEquals("REVIEW",stored.values().iterator().next().getState());
        assertThrows(IllegalStateException.class,()->refund("request-one"));assertThrows(IllegalStateException.class,()->refund("request-two"));verify(gateway,times(1)).refund(anyString(),anyString(),anyDouble(),anyString());
        assertThrows(IllegalStateException.class,()->refunds.requireNoUnresolvedRefund(1L));
    }
    @Test void definitiveRejectionCanBeRetriedWithTheSameKey(){
        when(gateway.refund(anyString(),anyString(),anyDouble(),anyString())).thenThrow(new PayHereRefundClient.Rejected()).thenReturn("refund-100");
        assertThrows(PayHereRefundClient.Rejected.class,()->refund("request-one"));assertEquals("REJECTED",stored.values().iterator().next().getState());
        assertEquals("refund-100",refund("request-one"));
    }
    @Test void authenticationFailureSendsNoRefundAndCreatesNoAttempt(){
        when(gateway.accessToken()).thenThrow(new IllegalStateException("Not configured"));assertThrows(IllegalStateException.class,()->refund("request-one"));assertTrue(stored.isEmpty());verify(gateway,never()).refund(anyString(),anyString(),anyDouble(),anyString());
    }
    @Test void changingAnExistingRefundRequestIsRejected(){
        refund("request-one");assertThrows(IllegalArgumentException.class,()->refunds.refund(1L,"payment-100",10.0,"Returned item","request-one"));assertThrows(IllegalArgumentException.class,()->refunds.refund(1L,"payment-100",25.50,"Different reason","request-one"));assertThrows(IllegalArgumentException.class,()->refund(null));
    }
    @Test void cardRefundFailureLeavesPaymentAndInvoiceUnchanged(){
        BillingServiceImpl billing=new BillingServiceImpl();InvoiceRepository invoices=mock(InvoiceRepository.class);PaymentRecordRepository payments=mock(PaymentRecordRepository.class);RefundRecordRepository ledger=mock(RefundRecordRepository.class);
        ReflectionTestUtils.setField(billing,"invoiceRepository",invoices);ReflectionTestUtils.setField(billing,"paymentRecordRepository",payments);ReflectionTestUtils.setField(billing,"refundRepository",ledger);ReflectionTestUtils.setField(billing,"cardRefunds",refunds);
        Invoice invoice=new Invoice();invoice.setInvoiceNumber("SP-100");invoice.setNetTotal(100.0);invoice.setAmountPaid(100.0);invoice.setStatus("PAID");PaymentRecord payment=new PaymentRecord();payment.setId(1L);payment.setInvoiceNumber("SP-100");payment.setAmount(100.0);payment.setRefundedAmount(0.0);payment.setPaymentMethod("CARD");payment.setGatewayPaymentId("payment-100");payment.setStatus("SUCCESS");
        when(payments.findById(1L)).thenReturn(Optional.of(payment));when(payments.lockById(1L)).thenReturn(Optional.of(payment));when(invoices.lockByInvoiceNumber("SP-100")).thenReturn(Optional.of(invoice));
        when(gateway.refund(anyString(),anyString(),anyDouble(),anyString())).thenThrow(new IllegalStateException("Timeout"));
        assertThrows(IllegalStateException.class,()->billing.refundPayment(1L,25.50,"Returned item","request-one"));assertEquals(100.0,invoice.getAmountPaid());assertEquals(0.0,payment.getRefundedAmount());verify(payments,never()).save(any());verify(ledger,never()).save(any());
    }
}
