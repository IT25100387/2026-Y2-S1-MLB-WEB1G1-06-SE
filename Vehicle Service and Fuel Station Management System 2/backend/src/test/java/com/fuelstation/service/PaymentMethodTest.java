package com.fuelstation.service;

import com.fuelstation.model.PaymentRecord;
import com.fuelstation.model.Invoice;
import com.fuelstation.model.CashierSale;
import com.fuelstation.repository.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.time.Clock;
import com.fuelstation.service.impl.BillingServiceImpl;
import com.fuelstation.util.Rules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PaymentMethodTest {
    @Test void onlyCashCardAndQrAreAcceptedForNewSettlements(){
        for(String method:new String[]{"CASH","CARD","QR"})assertEquals(method,Rules.paymentMethod(method));
        for(String method:new String[]{"BANK","BANK TRANSFER","CHEQUE","OTHER"})assertThrows(IllegalArgumentException.class,()->Rules.paymentMethod(method));
    }
    @Test void cardCannotBeRecordedByCallingManualPaymentApi(){
        BillingServiceImpl billing=new BillingServiceImpl();PaymentRecord p=new PaymentRecord();p.setPaymentMethod("CARD");p.setGatewayPaymentId("forged");p.setAmount(100.0);
        assertThrows(IllegalArgumentException.class,()->billing.processPayment(p));
    }
    @Test void verifiedCardConfirmationSettlesTheLedgerAndAllowsALastCentPayment(){
        for(String method:List.of("CASH","CARD")){
            BillingServiceImpl billing=new BillingServiceImpl();InvoiceRepository invoices=mock(InvoiceRepository.class);PaymentRecordRepository payments=mock(PaymentRecordRepository.class);CashierSaleRepository sales=mock(CashierSaleRepository.class);
            ReflectionTestUtils.setField(billing,"invoiceRepository",invoices);ReflectionTestUtils.setField(billing,"paymentRecordRepository",payments);ReflectionTestUtils.setField(billing,"cardSales",sales);ReflectionTestUtils.setField(billing,"notifications",mock(NotificationService.class));ReflectionTestUtils.setField(billing,"clock",Clock.systemUTC());
            Invoice invoice=new Invoice();invoice.setId(1L);invoice.setInvoiceNumber("SP-100");invoice.setInvoiceType("SPARE_PART");invoice.setFinalized(true);invoice.setNetTotal(.01);invoice.setAmountPaid(0.0);invoice.setStatus("PENDING");when(invoices.lockByInvoiceNumber("SP-100")).thenReturn(Optional.of(invoice));when(payments.save(any())).thenAnswer(i->i.getArgument(0));
            PaymentRecord payment=new PaymentRecord();payment.setInvoiceNumber("SP-100");payment.setInvoiceId(1L);payment.setRequestKey("checkout-100");payment.setAmount(.01);payment.setPaymentMethod(method);
            CashierSale sale=new CashierSale();sale.setId("checkout-100");sale.setInvoiceId(1L);sale.setPaymentMethod("CARD");sale.setPaymentAmount(.01);sale.setState("PENDING");sale.setGatewayPaymentId("gateway-100");when(sales.findById("checkout-100")).thenReturn(Optional.of(sale));
            if("CARD".equals(method)){when(sales.findByInvoiceId(1L)).thenReturn(List.of(sale));PaymentRecord recorded=billing.processGatewayPayment(payment,"gateway-100");assertEquals("gateway-100",recorded.getGatewayPaymentId());}else billing.processPayment(payment);
            assertEquals(.01,invoice.getAmountPaid());assertEquals("PAID",invoice.getStatus());verify(payments,times(1)).save(any());
        }
    }
}
