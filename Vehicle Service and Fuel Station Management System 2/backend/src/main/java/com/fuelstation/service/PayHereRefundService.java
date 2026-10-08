package com.fuelstation.service;

import com.fuelstation.model.GatewayRefundAttempt;
import com.fuelstation.repository.GatewayRefundAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;

@Service
public class PayHereRefundService {
    private final GatewayRefundAttemptRepository attempts;
    private final PayHereRefundClient gateway;
    private final TransactionTemplate durable;
    private final Clock clock;
    public PayHereRefundService(GatewayRefundAttemptRepository attempts,PayHereRefundClient gateway,PlatformTransactionManager manager,Clock clock){this.attempts=attempts;this.gateway=gateway;this.clock=clock;durable=new TransactionTemplate(manager);durable.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);}
    /** Caller holds the invoice and payment locks throughout allocation and ledger changes. */
    public void requireNoUnresolvedRefund(Long paymentId) {
        durable.executeWithoutResult(status->{
            if(attempts.findByPaymentId(paymentId).stream().anyMatch(a->Set.of("PENDING","REVIEW","CONFIRMED").contains(a.getState())))
                throw new IllegalStateException("An earlier card refund needs reconciliation before issuing a cash refund.");
        });
    }
    public String refund(Long paymentId,String gatewayId,double amount,String reason,String key) {
        if(key==null||key.isBlank())throw new IllegalArgumentException("Card refunds require a refund request key");
        // Scope an invoice refund's sub-key to its payment as well.
        String id=UUID.nameUUIDFromBytes((paymentId+":"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        GatewayRefundAttempt previous=durable.execute(status->attempts.findById(id).orElse(null));
        if(previous!=null){
            if(!Objects.equals(previous.getGatewayPaymentId(),gatewayId)||Math.abs(previous.getAmount()-amount)>.001||!reason.equals(previous.getReason()))throw new IllegalArgumentException("Refund request key has already been used for different details");
            if(Set.of("CONFIRMED","APPLIED").contains(previous.getState())){afterCommit(id);return previous.getGatewayRefundId();}
            if(!"REJECTED".equals(previous.getState()))throw new IllegalStateException("This refund needs verification with PayHere before any further refund.");
        }
        String token=gateway.accessToken();
        durable.executeWithoutResult(status->{
            if(attempts.findByPaymentId(paymentId).stream().anyMatch(a->Set.of("PENDING","REVIEW","CONFIRMED").contains(a.getState())))throw new IllegalStateException("An earlier card refund needs reconciliation. Verify it with PayHere before issuing another refund.");
            GatewayRefundAttempt a=attempts.findById(id).orElseGet(GatewayRefundAttempt::new);a.setId(id);a.setPaymentId(paymentId);a.setGatewayPaymentId(gatewayId);a.setAmount(amount);a.setReason(reason);a.setState("PENDING");a.setCreatedAt(LocalDateTime.now(clock));attempts.saveAndFlush(a);
        });
        try {
            String refundId=gateway.refund(token,gatewayId,amount,reason);
            update(id,"CONFIRMED",refundId);afterCommit(id);return refundId;
        } catch(PayHereRefundClient.Rejected e){update(id,"REJECTED",null);throw e;}
        catch(RuntimeException e){update(id,"REVIEW",null);throw e;}
    }
    private void update(String id,String state,String refundId){durable.executeWithoutResult(status->{GatewayRefundAttempt a=attempts.findById(id).orElseThrow();a.setState(state);a.setGatewayRefundId(refundId);attempts.saveAndFlush(a);});}
    private void afterCommit(String id){
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){durable.executeWithoutResult(status->{GatewayRefundAttempt a=attempts.findById(id).orElseThrow();a.setState("APPLIED");attempts.saveAndFlush(a);});}});
    }
}
