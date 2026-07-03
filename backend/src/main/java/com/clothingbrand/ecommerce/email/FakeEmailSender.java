package com.clothingbrand.ecommerce.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(FakeEmailSender.class);
    private final List<TransactionalEmail> sentEmails = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void send(TransactionalEmail email) {
        sentEmails.add(email);
        logger.info("Fake transactional email queued for {}", redactEmail(email.to()));
    }

    public List<TransactionalEmail> getSentEmails() {
        return List.copyOf(sentEmails);
    }

    public void clear() {
        sentEmails.clear();
    }

    private String redactEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        return "***@" + email.substring(email.indexOf('@') + 1);
    }
}
