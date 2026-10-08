package com.fuelstation.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuelstation.controller.InputValidationAdvice;
import com.fuelstation.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;

class InputValidationTest {
    private final ObjectMapper mapper=new ObjectMapper();
    @Test void normalizesWhitespaceAndKeepsPasswordsAndTokensExact() throws Exception {
        var node=mapper.readTree("{\"fullName\":\"  Kasun   Dias  \",\"phoneNumber\":\"0771234567\",\"notes\":\" First   line  \\n Next   line \",\"password\":\"  a long  passphrase  \",\"token\":\"  exact  token  \",\"items\":[{\"partName\":\" Oil   filter \"}]}");
        InputValidation.normalize(node);
        assertEquals("Kasun Dias",node.get("fullName").asText());
        assertEquals("+94771234567",node.get("phoneNumber").asText());
        assertEquals("First line\nNext line",node.get("notes").asText());
        assertEquals("  a long  passphrase  ",node.get("password").asText());
        assertEquals("  exact  token  ",node.get("token").asText());
        assertEquals("Oil filter",node.get("items").get(0).get("partName").asText());
    }
    @Test void restrictsNamesAndSupportsUnicode() {
        assertEquals("José Silva",InputValidation.name("  José   Silva  ","Name",true));
        for(String value:new String[]{"Kasun123","Kasun@Dias","Kasun-Dias"})assertThrows(IllegalArgumentException.class,()->InputValidation.name(value,"Name",true));
    }
    @Test void normalizesLocalPhonesAndRejectsInvalidInternationalNumbers() {
        for(String value:new String[]{"0771234567","771234567","+94 77 123 4567","0094771234567"})assertEquals("+94771234567",InputValidation.phone(value,"Phone",true));
        assertEquals("+14155552671",InputValidation.phone("+14155552671","Phone",true));
        for(String value:new String[]{"+940771234567","+9477123456","+1234567890123456","phone"})assertThrows(IllegalArgumentException.class,()->InputValidation.phone(value,"Phone",true));
        assertEquals("",InputValidation.phone("","Phone",false));
    }
    @Test void bindsNormalizedValuesBeforeTheyReachTheVehicleController() throws Exception {
        var request=new MockHttpServletRequest();request.setRequestURI("/api/workshop/vehicles/registry/add");
        var clock=Clock.fixed(Instant.parse("2026-10-07T00:00:00Z"),ZoneId.of("Asia/Colombo"));
        var advice=new InputValidationAdvice(mapper,request,clock);
        var input=new MockHttpInputMessage("{\"licensePlate\":\" ABC-1234 \",\"make\":\" Toyota \",\"model\":\" Corolla \",\"ownerName\":\" Kasun   Dias \",\"ownerContact\":\"0771234567\"}".getBytes(StandardCharsets.UTF_8));
        input.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var normalized=advice.beforeBodyRead(input,null,null,null);
        byte[] body=normalized.getBody().readAllBytes();
        assertEquals(body.length,normalized.getHeaders().getContentLength());
        var vehicle=mapper.readValue(body,Vehicle.class);
        assertEquals("Kasun Dias",vehicle.getOwnerName());assertEquals("Toyota",vehicle.getMake());
        assertEquals("ABC-1234",vehicle.getLicensePlate());assertEquals("+94771234567",vehicle.getOwnerContact());
    }
    @Test void reportsFieldErrorsForInvalidCatalogAndContactValues() throws Exception {
        var node=mapper.readTree("{\"ownerName\":\"Kasun@Dias\",\"ownerContact\":\"+940771234567\",\"model\":\"Corolla<script>\"}");
        var error=assertThrows(InputValidation.InvalidFields.class,()->InputValidation.validate(node,"/api/workshop/vehicles/registry/add"));
        assertTrue(error.fields().containsKey("ownerName"));assertTrue(error.fields().containsKey("ownerContact"));assertTrue(error.fields().containsKey("model"));
    }
    @Test void leavesGatewayNotificationsUntouched() throws Exception {
        var request=new MockHttpServletRequest();request.setRequestURI("/api/billing/card/notify");
        var advice=new InputValidationAdvice(mapper,request,Clock.systemUTC());
        var input=new MockHttpInputMessage("{\"phone\":\"0771234567\",\"signature\":\" exact signature \"}".getBytes(StandardCharsets.UTF_8));
        input.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        assertSame(input,advice.beforeBodyRead(input,null,null,null));
    }
    @Test void checksEmailLengthAndMalformedLocalAndDomainParts() {
        String longest="a".repeat(64)+"@"+"b".repeat(63)+"."+"c".repeat(63)+"."+"d".repeat(57)+".com";
        assertEquals(254,longest.length());assertEquals(longest,InputValidation.email(longest,true));
        assertEquals("customer+orders@example.co.uk",InputValidation.email("customer+orders@example.co.uk",true));
        for(String value:new String[]{"a".repeat(65)+"@example.com","name@"+"a".repeat(64)+".com",".name@example.com","name.@example.com","na..me@example.com","name@@example.com","name@-example.com","name@example-.com",longest+"a"})assertThrows(IllegalArgumentException.class,()->InputValidation.email(value,true),value);
    }
    @Test void checksActualInternationalNumberingPatterns() {
        for(String value:new String[]{"+94112345678","+442079460018","+919876543210"})assertEquals(value,InputValidation.phone(value,"Phone",true));
        for(String value:new String[]{"+94121234567","+999123456789","+1415555","+947712345678"})assertThrows(IllegalArgumentException.class,()->InputValidation.phone(value,"Phone",true),value);
    }
    @Test void acceptsEightCharacterPasswordsAndPreservesByteAndControlLimits() {
        assertDoesNotThrow(()->InputValidation.password("Abcd1234",true));
        assertDoesNotThrow(()->InputValidation.password("é".repeat(8),true));
        assertThrows(IllegalArgumentException.class,()->InputValidation.password("Abcd123",true));
        assertThrows(IllegalArgumentException.class,()->InputValidation.password("é".repeat(37),true));
        assertThrows(IllegalArgumentException.class,()->InputValidation.password("abcd\n1234",true));
        assertDoesNotThrow(()->InputValidation.password("short",false));
    }
    @Test void validatesMeaningfulAddressesWithoutRequiringAHouseNumber() {
        for(String value:new String[]{"25/3, Temple Road, Colombo","Colombo","12 ශ්‍රී මාවත","Apartment #4, St. John’s Road"})assertEquals(value,InputValidation.address(value,"Address",255,true));
        for(String value:new String[]{"A","12345",".....","Temple... Road","Road !!!","a".repeat(256)})assertThrows(IllegalArgumentException.class,()->InputValidation.address(value,"Address",255,true),value);
        assertEquals("",InputValidation.address("","Address",255,false));
    }
    @Test void validatesAndCanonicalizesDeliveryWindows() throws Exception {
        for(String value:new String[]{"Daily, 09:00–12:00","Every Monday, 9 am to noon","Weekdays, 00:00-23:59"})assertDoesNotThrow(()->DeliverySchedule.validate(value));
        for(String value:new String[]{"Soon","Daily, 25:00–26:00","Daily, 12:00–12:00","Weekends, 18:00–09:00","Every Funday, 09:00–12:00","Daily, –"})assertThrows(IllegalArgumentException.class,()->DeliverySchedule.validate(value),value);
        var node=mapper.readTree("{\"deliverySchedule\":\"Every Monday, 9 am to noon\"}");InputValidation.normalize(node);
        assertEquals("Every Monday, 09:00–12:00",node.path("deliverySchedule").asText());
        InputValidation.validate(node,"/api/inventory/suppliers");
        var invalid=mapper.readTree("{\"deliverySchedule\":\"Daily, 18:00–09:00\"}");
        assertTrue(assertThrows(InputValidation.InvalidFields.class,()->InputValidation.validate(invalid,"/api/inventory/suppliers")).fields().containsKey("deliverySchedule"));
    }
}
