package com.fuelstation.controller;

import com.fuelstation.service.BillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentRefundHttpTest {
    private final BillingService billing=mock(BillingService.class);
    private MockMvc mvc;
    @BeforeEach void setup(){
        var controller=new BillingRestController();ReflectionTestUtils.setField(controller,"billingService",billing);
        mvc=MockMvcBuilders.standaloneSetup(controller).build();
    }
    @Test void acceptsPartialAmountsAndForwardsTheRetryKey() throws Exception {
        when(billing.refundPayment(1L,25.50,"Returned item","refund-request")).thenReturn(true);
        mvc.perform(post("/api/billing/refund-payment/1").param("amount","25.50").param("reason","Returned item").header("Idempotency-Key","refund-request")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(billing).refundPayment(1L,25.50,"Returned item","refund-request");
    }
    @Test void rejectsInvalidDecimalAmountsBeforeCallingTheService() throws Exception {
        for(String value:new String[]{"0","-1","0.001","1e2","NaN","Infinity","abc",""})mvc.perform(post("/api/billing/refund-payment/1").param("amount",value).param("reason","Returned item")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(billing);
    }
    @Test void retainsTheExistingFullRefundRequestFormat() throws Exception {
        when(billing.refundPayment(1L,null,"Returned item",null)).thenReturn(true);
        mvc.perform(post("/api/billing/refund-payment/1").param("reason","Returned item")).andExpect(status().isOk());
        verify(billing).refundPayment(1L,null,"Returned item",null);
    }
    @Test void forwardsTheChosenCashOrCardRefundMethod() throws Exception {
        when(billing.refundPayment(1L,25.50,"Returned item","refund-request","CARD")).thenReturn(true);
        mvc.perform(post("/api/billing/refund-payment/1").param("amount","25.50").param("reason","Returned item").param("paymentMethod","CARD").header("Idempotency-Key","refund-request")).andExpect(status().isOk());
        verify(billing).refundPayment(1L,25.50,"Returned item","refund-request","CARD");
    }
    @Test void invoiceRefundRejectsInvalidDecimalAmountsBeforeReadingTheInvoice() throws Exception {
        for(String value:new String[]{"0","-1","0.001","1e2","NaN","Infinity","abc",""})mvc.perform(post("/api/billing/refund/1").param("amount",value).param("reason","Returned item")).andExpect(status().isBadRequest());
        verifyNoInteractions(billing);
    }
}
