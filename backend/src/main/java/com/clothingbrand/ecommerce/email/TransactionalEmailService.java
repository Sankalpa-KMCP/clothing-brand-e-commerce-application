package com.clothingbrand.ecommerce.email;

import com.clothingbrand.ecommerce.config.AccountSecurityProperties;
import com.clothingbrand.ecommerce.config.TransactionalEmailProperties;
import org.springframework.stereotype.Service;

@Service
public class TransactionalEmailService {

    private final EmailSender emailSender;
    private final TransactionalEmailProperties emailProperties;
    private final AccountSecurityProperties accountProperties;

    public TransactionalEmailService(EmailSender emailSender,
                                     TransactionalEmailProperties emailProperties,
                                     AccountSecurityProperties accountProperties) {
        this.emailSender = emailSender;
        this.emailProperties = emailProperties;
        this.accountProperties = accountProperties;
    }

    public void sendVerificationEmail(String to, String rawToken) {
        if (!emailProperties.isEnabled()) {
            return;
        }
        String link = accountProperties.getFrontendBaseUrl().replaceAll("/$", "") + "/verify-email?token=" + rawToken;
        emailSender.send(new TransactionalEmail(
                to,
                "Verify your email",
                "Verify your THREAD & Co. account using this link: " + link
        ));
    }

    public void sendPasswordResetEmail(String to, String rawToken) {
        if (!emailProperties.isEnabled()) {
            return;
        }
        String link = accountProperties.getFrontendBaseUrl().replaceAll("/$", "") + "/reset-password?token=" + rawToken;
        emailSender.send(new TransactionalEmail(
                to,
                "Reset your password",
                "Reset your THREAD & Co. password using this link: " + link
        ));
    }
}
