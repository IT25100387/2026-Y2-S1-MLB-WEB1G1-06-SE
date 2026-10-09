package com.fuelstation.service;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.impl.BillingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Clock;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRefundValidationTest {
    @Mock InvoiceRepository invoices;
    @Mock PaymentRecordRepository payments;
    @Mock RefundRecordRepository refunds;
    @Mock NotificationService notifications;
    @InjectMocks BillingServiceImpl service;
    Invoice invoice;
    PaymentRecord payment;
    @BeforeEach void setup(){
        ReflectionTestUtils.setField(service,"clock",Clock.systemUTC());
        invoice=new Invoice();invoice.setInvoiceNumber("SP-123");invoice.setNetTotal(100.0);invoice.setAmountPaid(100.0);invoice.setStatus("PAID");
        payment=new PaymentRecord();payment.setId(1L);payment.setPaymentMethod("CASH");payment.setInvoiceNumber("SP-123");payment.setAmount(100.0);payment.setRefundedAmount(0.0);payment.setStatus("SUCCESS");
        when(payments.findById(1L)).thenReturn(Optional.of(payment));
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));
        when(invoices.lockByInvoiceNumber("SP-123")).thenReturn(Optional.of(invoice));
    }
    @Test void allowsOneEditablePartialRefundThenRejectsAllFurtherRefunds(){
        assertTrue(service.refundPayment(1L,25.50,"Customer requested return",null));
        assertEquals(25.50,payment.getRefundedAmount());assertEquals("PARTIALLY_REFUNDED",payment.getStatus());assertEquals(74.50,invoice.getAmountPaid());
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,10.0,"Second returned item",null));
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,"Refund remaining balance"));
        assertEquals(25.50,payment.getRefundedAmount());assertEquals(74.50,invoice.getAmountPaid());
        verify(refunds,times(1)).save(any());verify(notifications,times(1)).invoiceChanged(any(),eq("Refund recorded"));
    }
    @Test void rejectsInvalidAmountsBeforeAnyRecordsAreChanged(){
        for(double amount:new double[]{0,-1,.001,100.01,Double.NaN,Double.POSITIVE_INFINITY})assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,amount,"Customer requested return",null));
        assertEquals(0.0,payment.getRefundedAmount());assertEquals(100.0,invoice.getAmountPaid());
        verify(refunds,never()).save(any());verifyNoInteractions(notifications);verify(payments,never()).save(any());
    }
    @Test void rejectsHistoricalPartialRefundsEvenIfRetainedPaymentsRemain(){
        payment.setRefundedAmount(60.0);payment.setStatus("PARTIALLY_REFUNDED");invoice.setAmountPaid(40.0);invoice.setSettledTotal(100.0);
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,40.01,"Customer requested return",null));
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,40.0,"Customer requested return",null));
        assertEquals(60.0,payment.getRefundedAmount());verify(refunds,never()).save(any());
    }
    @Test void preservesARefundableLastCent(){
        assertTrue(service.refundPayment(1L,99.99,"Customer requested return",null));
        assertEquals("PARTIALLY_REFUNDED",payment.getStatus());assertEquals("PARTIALLY_REFUNDED",invoice.getStatus());
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,.01,"Refund last cent",null));assertEquals(99.99,payment.getRefundedAmount());
    }
    @Test void replayingTheSameRequestDoesNotRefundTwice(){
        when(refunds.findByRequestKey("request-one")).thenReturn(Optional.empty());
        assertTrue(service.refundPayment(1L,100.0,"Customer requested return","request-one"));
        var captured=ArgumentCaptor.forClass(RefundRecord.class);verify(refunds).save(captured.capture());
        when(refunds.findByRequestKey("request-one")).thenReturn(Optional.of(captured.getValue()));
        assertTrue(service.refundPayment(1L,100.0,"Customer requested return","request-one"));
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,50.0,"Customer requested return","request-one"));
        verify(refunds,times(1)).save(any());assertEquals(100.0,payment.getRefundedAmount());
    }
    @Test void failedOrPendingPaymentsCannotBeRefundedAndReasonsAreRequired(){
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,10.0,"x",null));
        for(String status:new String[]{"PENDING","FAILED","REFUNDED"}){payment.setStatus(status);assertFalse(service.refundPayment(1L,10.0,"Customer requested return",null));}
        verify(refunds,never()).save(any());verifyNoInteractions(notifications);
    }
    @Test void partiallyPaidAndProvisionalInvoicesCannotBeRefunded(){
        invoice.setAmountPaid(50.0);invoice.setStatus("PARTIAL");
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,25.0,"Returned item",null));
        invoice.setAmountPaid(100.0);invoice.setInvoiceType("SERVICE");invoice.setFinalized(false);
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,25.0,"Returned item",null));
        verify(payments,never()).save(any());verify(refunds,never()).save(any());
    }
    @Test void anotherPaymentOrTheInvoiceEndpointCannotBypassTheOneRefundRule(){
        assertTrue(service.refundPayment(1L,25.0,"Returned item",null));
        PaymentRecord second=new PaymentRecord();second.setId(2L);second.setInvoiceNumber("SP-123");second.setAmount(50.0);second.setPaymentMethod("CASH");second.setStatus("SUCCESS");
        when(payments.findById(2L)).thenReturn(Optional.of(second));when(payments.lockById(2L)).thenReturn(Optional.of(second));
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(2L,10.0,"Another return",null));
        assertThrows(IllegalArgumentException.class,()->service.refundInvoice("SP-123",10.0,"Another return",null));
        verify(refunds,times(1)).save(any());
    }
    @Test void cashCanBeSelectedForQrPaymentsButQrIsNotARefundMethod(){
        payment.setPaymentMethod("QR");
        assertThrows(IllegalArgumentException.class,()->service.refundPayment(1L,25.0,"Returned item",null,"QR"));
        assertTrue(service.refundPayment(1L,25.0,"Returned item",null,"CASH"));
        var recorded=ArgumentCaptor.forClass(RefundRecord.class);verify(refunds).save(recorded.capture());assertEquals("CASH",recorded.getValue().getPaymentMethod());
    }
}
