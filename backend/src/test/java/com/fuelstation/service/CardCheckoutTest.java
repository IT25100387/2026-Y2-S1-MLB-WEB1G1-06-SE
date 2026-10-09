package com.fuelstation.service;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.*;
import java.util.*;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CardCheckoutTest {
    CashierPOSService pos=mock(CashierPOSService.class);
    CashierSaleRepository sales=mock(CashierSaleRepository.class);
    InvoiceRepository invoices=mock(InvoiceRepository.class);
    SparePartRepository parts=mock(SparePartRepository.class);
    JobPartUsageRepository usages=mock(JobPartUsageRepository.class);
    PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
    Map<String,CashierSale> stored=new HashMap<>();
    CashierCardService service;
    Invoice invoice;
    @BeforeEach void setup(){
        when(manager.getTransaction(any())).thenAnswer(i->new SimpleTransactionStatus());
        service=new CashierCardService(pos,sales,invoices,parts,usages,mock(BillingService.class),mock(NotificationService.class),Clock.systemUTC(),manager);
        for(var setting:Map.of("merchant","merchant","secret","secret","notifyUrl","https://station.example/api/pos/card/notify","frontend","https://station.example","guestEmail","station@example.com","guestPhone","+94771234567","guestAddress","25 Main Street","guestCity","Colombo","guestCountry","Sri Lanka").entrySet())ReflectionTestUtils.setField(service,setting.getKey(),setting.getValue());
        invoice=new Invoice();invoice.setId(1L);invoice.setInvoiceNumber("SP-100");invoice.setInvoiceType("SPARE_PART");invoice.setNetTotal(100.0);invoice.setAmountPaid(0.0);invoice.setStatus("PENDING");invoice.setFinalized(true);invoice.setCustomerUsername("owner");
        when(invoices.lockByInvoiceNumber("SP-100")).thenReturn(Optional.of(invoice));when(invoices.findById(1L)).thenReturn(Optional.of(invoice));
        when(sales.findById(anyString())).thenAnswer(i->Optional.ofNullable(stored.get(i.getArgument(0))));when(sales.lockById(anyString())).thenAnswer(i->Optional.ofNullable(stored.get(i.getArgument(0))));
        when(sales.saveAndFlush(any())).thenAnswer(i->{CashierSale sale=i.getArgument(0);stored.put(sale.getId(),sale);return sale;});
        when(pos.receipt(any())).thenAnswer(i->{CashierSale s=i.getArgument(0);return Map.of("completed","SUCCESS".equals(s.getState()),"state",s.getState());});
    }
    Object start(){return service.startInvoice(new CashierCardService.InvoiceRequest("SP-100",100.0),"checkout-100","owner",true);}
    @Test void opensAnonymousCheckoutWithoutCollectingCustomerDetails(){
        Map<?,?> response=(Map<?,?>)start();Map<?,?> payment=(Map<?,?>)response.get("payment");
        assertEquals("Guest",payment.get("first_name"));assertEquals("Customer",payment.get("last_name"));assertEquals("station@example.com",payment.get("email"));assertEquals("100.00",payment.get("amount"));assertEquals("INVOICE",stored.get("checkout-100").getCheckoutType());
        verify(pos,never()).settle(any());
    }
    @Test void rejectsInvalidInvoiceAmountsBeforePreparingCheckout(){
        for(double amount:new double[]{0,-1,100.01,1.001,Double.NaN})assertThrows(IllegalArgumentException.class,()->service.startInvoice(new CashierCardService.InvoiceRequest("SP-100",amount),"checkout-100","owner",true));
        assertTrue(stored.isEmpty());
    }
    @Test void rejectsForeignInvoicesAndForeignCheckoutStatus(){
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.startInvoice(new CashierCardService.InvoiceRequest("SP-100",100.0),"checkout-100","other",true));
        start();assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.status("checkout-100","other"));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.cancelUnprocessed("checkout-100","other"));
    }
    @Test void preservesPartialAdvanceAndFinalSettlementRules(){
        invoice.setInvoiceType("SERVICE");invoice.setFinalized(false);
        assertThrows(IllegalArgumentException.class,this::start);
        service.startInvoice(new CashierCardService.InvoiceRequest("SP-100",25.0),"checkout-100","owner",true);assertEquals(25.0,stored.get("checkout-100").getPaymentAmount());
    }
    @Test void pendingCheckoutBlocksASecondCheckoutAndReusesIdenticalRequest(){
        start();when(sales.findByInvoiceId(1L)).thenReturn(List.copyOf(stored.values()));
        start();verify(sales,times(1)).saveAndFlush(any());
        assertThrows(IllegalArgumentException.class,()->service.startInvoice(new CashierCardService.InvoiceRequest("SP-100",100.0),"checkout-200","owner",true));
    }
    @Test void missingStationProfileDoesNotPrepareAPayment(){
        ReflectionTestUtils.setField(service,"guestEmail","");assertThrows(IllegalStateException.class,this::start);assertTrue(stored.isEmpty());
    }
    @Test void closingInvoiceCheckoutDoesNotVoidTheInvoiceOrReleaseParts(){
        start();service.cancelUnprocessed("checkout-100","owner");assertEquals("PENDING",invoice.getStatus());assertEquals("CANCELLED",stored.get("checkout-100").getState());verifyNoInteractions(parts,usages);
    }
    @Test void onlySignedMatchingNotificationSettlesAndReplayDoesNotSettleTwice(){
        start();Map<String,String> callback=notification();callback.put("md5sig","00000000000000000000000000000000");assertThrows(IllegalArgumentException.class,()->service.notify(callback));verify(pos,never()).settle(any());
        doAnswer(i->{((CashierSale)i.getArgument(0)).setState("SUCCESS");return null;}).when(pos).settle(any());
        service.notify(notification());service.notify(notification());verify(pos,times(1)).settle(any());assertEquals("gateway-100",stored.get("checkout-100").getGatewayPaymentId());
    }
    private String md5(String s){try{return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("MD5").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new RuntimeException(e);}}
    private void demo(){ReflectionTestUtils.setField(service,"merchant","");ReflectionTestUtils.setField(service,"guestEmail","");ReflectionTestUtils.setField(service,"frontend","http://localhost:5173");ReflectionTestUtils.setField(service,"sandbox",true);ReflectionTestUtils.setField(service,"demoEnabled",true);}
    @Test void localDemoFinishesAtAReceiptWithoutSettlingTheInvoice(){
        demo();Map<?,?> started=(Map<?,?>)start();assertEquals(true,started.get("demoCheckout"));assertEquals("DEMO_PENDING",stored.get("checkout-100").getState());
        Map<?,?> receipt=(Map<?,?>)service.completeDemo("checkout-100","owner");assertEquals(true,receipt.get("completed"));assertEquals(true,receipt.get("demo"));assertEquals("PAID",((Map<?,?>)receipt.get("invoice")).get("status"));assertEquals(0.0,invoice.getAmountPaid());assertEquals("PENDING",invoice.getStatus());verify(pos,never()).settle(any());verify(invoices,never()).save(any());verifyNoInteractions(parts,usages);
        service.completeDemo("checkout-100","owner");assertEquals(0.0,invoice.getAmountPaid());
    }
    @Test void demoStillEnforcesCheckoutAndInvoiceOwnership(){demo();assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.startInvoice(new CashierCardService.InvoiceRequest("SP-100",100.0),"checkout-100","other",true));start();assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.completeDemo("checkout-100","other"));}
    @Test void demoIsUnavailableForLiveModeAndPublicFrontendHosts(){
        demo();for(String host:List.of("localhost","127.0.0.1","[::1]")){ReflectionTestUtils.setField(service,"frontend","http://"+host+":5173");assertEquals("DEMO",((Map<?,?>)service.configuration()).get("mode"));}
        demo();ReflectionTestUtils.setField(service,"sandbox",false);assertEquals(false,((Map<?,?>)service.configuration()).get("configured"));assertThrows(IllegalStateException.class,this::start);
        demo();ReflectionTestUtils.setField(service,"frontend","https://station.example");assertEquals(false,((Map<?,?>)service.configuration()).get("configured"));assertThrows(IllegalStateException.class,this::start);
        ReflectionTestUtils.setField(service,"merchant","merchant");ReflectionTestUtils.setField(service,"guestEmail","station@example.com");ReflectionTestUtils.setField(service,"notifyUrl","https://[::1]/api/pos/card/notify");assertEquals("UNAVAILABLE",((Map<?,?>)service.configuration()).get("mode"));
    }
    @Test void validPayHereConfigurationTakesPriorityAndCannotBeCompletedThroughDemo(){
        ReflectionTestUtils.setField(service,"frontend","http://localhost:5173");ReflectionTestUtils.setField(service,"sandbox",true);ReflectionTestUtils.setField(service,"demoEnabled",true);assertEquals("PAYHERE",((Map<?,?>)service.configuration()).get("mode"));start();assertThrows(IllegalArgumentException.class,()->service.completeDemo("checkout-100","owner"));verify(pos,never()).settle(any());
    }
    @Test void closingADemoNeverReleasesOrChangesStock(){demo();start();service.cancelUnprocessed("checkout-100","owner");assertEquals("CANCELLED",stored.get("checkout-100").getState());assertEquals(false,((Map<?,?>)service.completeDemo("checkout-100","owner")).get("completed"));verifyNoInteractions(parts,usages);}
    @Test void aRealSignedNotificationCannotSettleADemoCheckout(){
        demo();start();ReflectionTestUtils.setField(service,"merchant","merchant");assertThrows(IllegalArgumentException.class,()->service.notify(notification()));verify(pos,never()).settle(any());assertEquals("DEMO_PENDING",stored.get("checkout-100").getState());
    }
    private Map<String,String> notification(){return new HashMap<>(Map.of("merchant_id","merchant","order_id","checkout-100","payhere_amount","100.00","payhere_currency","LKR","status_code","2","payment_id","gateway-100","md5sig",md5("merchantcheckout-100100.00LKR2"+md5("secret"))));}
}
