package com.fuelstation.controller;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fuelstation.model.AppUser;
import com.fuelstation.util.InputValidation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import java.lang.reflect.Type;
import java.time.Clock;
import java.time.LocalDate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@ControllerAdvice(basePackages="com.fuelstation.controller")
public class InputValidationAdvice extends RequestBodyAdviceAdapter {
    private final ObjectMapper mapper;
    private final HttpServletRequest request;
    private final Clock clock;
    public InputValidationAdvice(ObjectMapper mapper,HttpServletRequest request,Clock clock) {this.mapper=mapper;this.request=request;this.clock=clock;}
    @Override public boolean supports(MethodParameter p,Type type,Class<? extends HttpMessageConverter<?>> converter) {return true;}
    @Override public HttpInputMessage beforeBodyRead(HttpInputMessage input,MethodParameter parameter,Type target,Class<? extends HttpMessageConverter<?>> converter) throws IOException {
        MediaType type=input.getHeaders().getContentType();
        if(type==null||!MediaType.APPLICATION_JSON.isCompatibleWith(type))return input;
        byte[] bytes=input.getBody().readAllBytes();
        JsonNode raw;
        try{raw=mapper.readTree(bytes);}catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new org.springframework.http.converter.HttpMessageNotReadableException("Enter valid JSON values",error,input);}
        if(raw!=null)InputValidation.validate(raw,request.getRequestURI(),LocalDate.now(clock));
        return new HttpInputMessage(){public InputStream getBody(){return new ByteArrayInputStream(bytes);}public HttpHeaders getHeaders(){return input.getHeaders();}};
    }
    @Override public Object afterBodyRead(Object body,HttpInputMessage input,MethodParameter parameter,Type target,Class<? extends HttpMessageConverter<?>> converter) {
        // PayHere callbacks have their own signature/amount verification and are not user forms.
        if(request.getRequestURI().endsWith("/card/notify"))return body;
        JsonNode node=mapper.valueToTree(body);
        if(body instanceof AppUser user&&user.getPassword()!=null&&node instanceof ObjectNode object)object.put("password",user.getPassword());
        InputValidation.validate(node,request.getRequestURI(),LocalDate.now(clock));return body;
    }
    @Bean static Jackson2ObjectMapperBuilderCustomizer rejectFractionalIntegers() {return builder -> builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);}
}
