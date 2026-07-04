package com.clothingbrand.ecommerce.email;

import com.clothingbrand.ecommerce.config.TransactionalEmailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final TransactionalEmailProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, TransactionalEmailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(TransactionalEmail email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFromAddress());
        message.setTo(email.to());
        message.setSubject(email.subject());
        message.setText(email.textBody());
        mailSender.send(message);
    }
}
