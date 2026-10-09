package com.fuelstation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Component
public class PayHereRefundClient {
    @Value("${app.payhere.app-id:}") private String appId;
    @Value("${app.payhere.app-secret:}") private String appSecret;
    @Value("${app.payhere.sandbox:true}") private boolean sandbox;
    private final ObjectMapper json;
    private final HttpClient http;
    private String token;
    private long tokenExpires;
    @org.springframework.beans.factory.annotation.Autowired
    public PayHereRefundClient(ObjectMapper json) { this(json,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()); }
    PayHereRefundClient(ObjectMapper json,HttpClient http) { this.json=json;this.http=http; }
    private String base() { return (sandbox?"https://sandbox.payhere.lk":"https://www.payhere.lk")+"/merchant/v1"; }
    /** Obtain authentication before committing an attempt; no refund is sent at this stage. */
    public synchronized String accessToken() {
        if(appId.isBlank()||appSecret.isBlank())throw new IllegalStateException("Card refunds require the PayHere App ID and App Secret in station settings");
        if(token!=null&&System.currentTimeMillis()<tokenExpires)return token;
        try {
            String basic=Base64.getEncoder().encodeToString((appId+":"+appSecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request=HttpRequest.newBuilder(URI.create(base()+"/oauth/token")).timeout(Duration.ofSeconds(20)).header("Authorization","Basic "+basic).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials")).build();
            HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
            JsonNode body=json.readTree(response.body());
            if(response.statusCode()!=200||body.path("access_token").asText().isBlank())throw new IllegalStateException("PayHere refund authentication failed. Check the station API credentials and allowed server IP.");
            token=body.path("access_token").asText();tokenExpires=System.currentTimeMillis()+Math.max(0,body.path("expires_in").asLong()-30)*1000;return token;
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Refund authentication interrupted",e);}
        catch(java.io.IOException e){throw new IllegalStateException("Could not authenticate the refund with PayHere. No refund was sent.",e);}
    }
    public static class Rejected extends RuntimeException { public Rejected(){super("PayHere rejected this refund. No refund was recorded.");} }
    public String refund(String authorization,String paymentId,double amount,String reason) {
        try {
            String body=json.writeValueAsString(Map.of("payment_id",paymentId,"description",reason,"amount",String.format(Locale.ROOT,"%.2f",amount)));
            HttpRequest request=HttpRequest.newBuilder(URI.create(base()+"/payment/refund")).timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+authorization).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
            JsonNode result=json.readTree(response.body());
            if(response.statusCode()==200&&result.path("status").asInt()==1&&!result.path("data").isNull()&&!result.path("data").asText().isBlank())return result.path("data").asText();
            if(response.statusCode()==401||response.statusCode()==403||response.statusCode()==429||(response.statusCode()==200&&result.has("status")&&Set.of(0,-1,-2).contains(result.path("status").asInt())))throw new Rejected();
            throw new IllegalStateException("PayHere returned an uncertain refund result. Verify the refund with PayHere before retrying.");
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Refund interrupted. Verify it with PayHere before retrying.",e);}
        catch(java.io.IOException e){throw new IllegalStateException("Refund confirmation was not received. Verify it with PayHere before retrying.",e);}
    }
}
