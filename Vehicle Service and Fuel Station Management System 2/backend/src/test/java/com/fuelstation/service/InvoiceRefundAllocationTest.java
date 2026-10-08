package com.fuelstation.service;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.impl.BillingServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class InvoiceRefundAllocationTest {
    BillingServiceImpl billing=new BillingServiceImpl();
    InvoiceRepository invoices=mock(InvoiceRepository.class);
    PaymentRecordRepository payments=mock(PaymentRecordRepository.class);
    PayHereRefundService gateway=mock(PayHereRefundService.class);
    RefundRecordRepository refunds=mock(RefundRecordRepository.class);
    Invoice invoice;
    @BeforeEach void setup(){ReflectionTestUtils.setField(billing,"invoiceRepository",invoices);ReflectionTestUtils.setField(billing,"paymentRecordRepository",payments);ReflectionTestUtils.setField(billing,"refundRepository",refunds);ReflectionTestUtils.setField(billing,"cardRefunds",gateway);ReflectionTestUtils.setField(billing,"clock",java.time.Clock.systemUTC());ReflectionTestUtils.setField(billing,"notifications",mock(NotificationService.class));invoice=new Invoice();invoice.setInvoiceNumber("SP-100");invoice.setNetTotal(100.0);invoice.setAmountPaid(100.0);invoice.setStatus("PAID");when(invoices.lockByInvoiceNumber("SP-100")).thenReturn(Optional.of(invoice));}
    PaymentRecord payment(long id,String method,double amount){PaymentRecord p=new PaymentRecord();p.setId(id);p.setInvoiceNumber("SP-100");p.setPaymentMethod(method);p.setAmount(amount);p.setRefundedAmount(0.0);p.setStatus("SUCCESS");p.setGatewayPaymentId("payment-"+id);when(payments.lockById(id)).thenReturn(Optional.of(p));return p;}
    @Test void validatesAllRefundMethodsBeforeSendingAnyGatewayRefund(){PaymentRecord card=payment(1L,"CARD",50),historical=payment(2L,"BANK",50);when(payments.findByInvoiceNumberOrderByIdAsc("SP-100")).thenReturn(List.of(card,historical));assertThrows(IllegalArgumentException.class,()->billing.refundInvoice("SP-100",100.0,"Returned items","request-one"));verifyNoInteractions(gateway);assertEquals(0.0,card.getRefundedAmount());}
    @Test void failedPaymentsDoNotIncreaseRefundableAmount(){var failed=payment(1L,"CASH",100);failed.setStatus("FAILED");when(payments.findByInvoiceNumberOrderByIdAsc("SP-100")).thenReturn(List.of(failed));assertThrows(IllegalStateException.class,()->billing.refundInvoice("SP-100",100.0,"Returned items","request-one"));verifyNoInteractions(gateway);}
    @Test void missingGatewayReferencesAreRejectedBeforeRefundingEarlierPayments(){PaymentRecord card=payment(1L,"CARD",50),historical=payment(2L,"CARD",50);historical.setGatewayPaymentId(null);when(payments.findByInvoiceNumberOrderByIdAsc("SP-100")).thenReturn(List.of(card,historical));assertThrows(IllegalArgumentException.class,()->billing.refundInvoice("SP-100",100.0,"Returned items","request-one"));verifyNoInteractions(gateway);}
    @Test void oneInvoiceRefundCanAllocateAcrossPaymentsAndReplayWithoutRefundingAgain(){
        PaymentRecord first=payment(1L,"CASH",40),second=payment(2L,"CASH",60);when(payments.findByInvoiceNumberOrderByIdAsc("SP-100")).thenReturn(List.of(first,second));
        assertTrue(billing.refundInvoice("SP-100",100.0,"Returned items","request-one","CASH"));
        assertEquals("REFUNDED",invoice.getStatus());assertEquals(0.0,invoice.getAmountPaid());assertEquals(40.0,first.getRefundedAmount());assertEquals(60.0,second.getRefundedAmount());
        var records=org.mockito.ArgumentCaptor.forClass(RefundRecord.class);verify(refunds,times(3)).save(records.capture());RefundRecord marker=records.getAllValues().get(2);when(refunds.findByRequestKey("request-one")).thenReturn(Optional.of(marker));
        assertTrue(billing.refundInvoice("SP-100",100.0,"Returned items","request-one","CASH"));
        assertThrows(IllegalArgumentException.class,()->billing.refundInvoice("SP-100",10.0,"Another return","request-two","CASH"));
        verify(refunds,times(3)).save(any());verifyNoInteractions(gateway);
    }
    @Test void recordedRefundHistoryPreventsAnotherRefundEvenWithStaleInvoiceTotals(){
        RefundRecord old=new RefundRecord();old.setAmount(10.0);when(refunds.findByInvoiceNumberOrderByIdAsc("SP-100")).thenReturn(List.of(old));
        assertThrows(IllegalArgumentException.class,()->billing.refundInvoice("SP-100",25.0,"Returned items","request-one","CASH"));verify(payments,never()).save(any());verifyNoInteractions(gateway);
    }
    @Test void choosingCashDoesNotBypassAnUncertainEarlierCardRefund(){
        PaymentRecord card=payment(1L,"CARD",100);when(payments.findByInvoiceNumberOrderByIdAsc("SP-100")).thenReturn(List.of(card));doThrow(new IllegalStateException("Needs reconciliation")).when(gateway).requireNoUnresolvedRefund(1L);
        assertThrows(IllegalStateException.class,()->billing.refundInvoice("SP-100",25.0,"Returned items","request-one","CASH"));verify(payments,never()).save(any());verify(refunds,never()).save(any());
    }
}
