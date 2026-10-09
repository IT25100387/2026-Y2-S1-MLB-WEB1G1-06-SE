package com.fuelstation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.http.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PayHereRefundClientTest {
    HttpClient http=mock(HttpClient.class);
    PayHereRefundClient client;
    @BeforeEach void setup(){client=new PayHereRefundClient(new ObjectMapper(),http);ReflectionTestUtils.setField(client,"appId","test-app");ReflectionTestUtils.setField(client,"appSecret","test-secret");ReflectionTestUtils.setField(client,"sandbox",true);}
    @SuppressWarnings("unchecked") HttpResponse<String> response(int code,String body){HttpResponse<String> r=mock(HttpResponse.class);when(r.statusCode()).thenReturn(code);when(r.body()).thenReturn(body);return r;}
    @Test void usesOAuthAndOriginalPaymentReferenceWithPartialAmount() throws Exception {
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(i->{HttpRequest request=i.getArgument(0);
            if(request.uri().getPath().endsWith("/oauth/token")){assertEquals("https://sandbox.payhere.lk/merchant/v1/oauth/token",request.uri().toString());assertEquals("Basic dGVzdC1hcHA6dGVzdC1zZWNyZXQ=",request.headers().firstValue("Authorization").orElseThrow());assertEquals("grant_type=client_credentials",body(request));return response(200,"{\"access_token\":\"token\",\"expires_in\":599}");}
            assertEquals("https://sandbox.payhere.lk/merchant/v1/payment/refund",request.uri().toString());assertEquals("Bearer token",request.headers().firstValue("Authorization").orElseThrow());var payload=new ObjectMapper().readTree(body(request));assertEquals("payment-100",payload.get("payment_id").asText());assertEquals("25.50",payload.get("amount").asText());assertFalse(payload.has("authorization_token"));return response(200,"{\"status\":1,\"data\":560034010257}");
        });
        String token=client.accessToken();assertEquals(token,client.accessToken());assertEquals("560034010257",client.refund(token,"payment-100",25.50,"Returned item"));verify(http,times(2)).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    @Test void rejectsMissingCredentialsBeforeCallingGateway(){ReflectionTestUtils.setField(client,"appId","");assertThrows(IllegalStateException.class,client::accessToken);verifyNoInteractions(http);}
    @Test void returnsDefiniteRejectionWithoutTreatingItAsSuccess() throws Exception {var rejected=response(200,"{\"status\":-1,\"data\":null}");when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(rejected);assertThrows(PayHereRefundClient.Rejected.class,()->client.refund("token","payment-100",25.50,"Returned item"));}
    @Test void missingRefundReferenceOrTimeoutIsUncertain() throws Exception {
        var uncertain=response(200,"{\"status\":1,\"data\":null}");when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(uncertain).thenThrow(new IOException("Timeout"));
        assertThrows(IllegalStateException.class,()->client.refund("token","payment-100",25.50,"Returned item"));assertThrows(IllegalStateException.class,()->client.refund("token","payment-100",25.50,"Returned item"));
    }
    private String body(HttpRequest request){StringBuilder text=new StringBuilder();request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){public void onSubscribe(Flow.Subscription s){s.request(Long.MAX_VALUE);}public void onNext(ByteBuffer b){text.append(java.nio.charset.StandardCharsets.UTF_8.decode(b));}public void onError(Throwable t){throw new RuntimeException(t);}public void onComplete(){}});return text.toString();}
}
