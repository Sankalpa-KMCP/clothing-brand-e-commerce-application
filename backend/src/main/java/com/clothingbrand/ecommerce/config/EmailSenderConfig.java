package com.clothingbrand.ecommerce.config;

import com.clothingbrand.ecommerce.email.EmailSender;
import com.clothingbrand.ecommerce.email.FakeEmailSender;
import com.clothingbrand.ecommerce.email.SmtpEmailSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailSenderConfig {

    @Bean
    public EmailSender emailSender(TransactionalEmailProperties properties) {
        if (!properties.isEnabled() || "fake".equalsIgnoreCase(properties.getMode())) {
            return new FakeEmailSender();
        }
        if ("smtp".equalsIgnoreCase(properties.getMode())) {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(properties.getSmtp().getHost());
            mailSender.setPort(properties.getSmtp().getPort());
            mailSender.setUsername(properties.getSmtp().getUsername());
            mailSender.setPassword(properties.getSmtp().getPassword());
            Properties javaMailProperties = mailSender.getJavaMailProperties();
            javaMailProperties.put("mail.smtp.auth", "true");
            javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(properties.getSmtp().isStartTls()));
            return new SmtpEmailSender(mailSender, properties);
        }
        throw new IllegalStateException("Unsupported email mode: " + properties.getMode());
    }
}
