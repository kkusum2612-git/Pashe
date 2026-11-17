package com.pashe.app.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AuthMailService {
    private final JavaMailSender mail;
    private final String frontend;
    private final String from;
    public AuthMailService(JavaMailSender mail, @Value("${pashe.auth.frontend-url}") String frontend, @Value("${pashe.auth.mail-from}") String from) {
        this.mail=mail; this.frontend=frontend; this.from=from;
    }
    void send(String email, String token, boolean reset) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email);
        message.setSubject(reset ? "Reset your Pashe password" : "Verify your Pashe email");
        message.setText((reset ? "Reset your password within 30 minutes: " : "Verify your email within 24 hours: ")
            + frontend + "/#/auth/" + (reset ? "reset" : "verify") + "?token=" + token
            + "\n\nIf you did not request this, you can ignore this message. / আপনি অনুরোধ না করলে এই ইমেইল উপেক্ষা করুন।");
        mail.send(message);
    }
}
