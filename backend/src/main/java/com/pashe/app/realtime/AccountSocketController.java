package com.pashe.app.realtime;

import java.util.Map;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class AccountSocketController {
    @MessageMapping("/account/ping")
    @SendToUser(value="/queue/account",broadcast=false)
    public Map<String,String> ping() { return Map.of("status","connected"); }
}
