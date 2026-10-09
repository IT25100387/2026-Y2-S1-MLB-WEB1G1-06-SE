package com.fuelstation.controller;
import com.fuelstation.service.CashierCardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/pos/card")
public class CashierCardRestController {
    private final CashierCardService cards;
    public CashierCardRestController(CashierCardService cards){this.cards=cards;}
    @GetMapping("/configuration") public Object configuration(){return cards.configuration();}
    @PostMapping("/start") public Object start(@RequestBody CashierCardService.CardRequest input,@RequestHeader("Idempotency-Key") String key,Authentication auth){return cards.start(input,key,auth.getName());}
    @GetMapping("/{id}") public Object status(@PathVariable String id,Authentication auth){return cards.status(id,auth.getName());}
    @PostMapping("/{id}/cancel-unprocessed") public Object cancel(@PathVariable String id,Authentication auth){return cards.cancelUnprocessed(id,auth.getName());}
    @PostMapping("/{id}/demo-complete") public Object demo(@PathVariable String id,Authentication auth){return cards.completeDemo(id,auth.getName());}
    @PostMapping(value="/notify",consumes="application/x-www-form-urlencoded") public Object notify(@RequestParam Map<String,String> values){cards.notify(values);return Map.of("received",true);}
}
