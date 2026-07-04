package com.clothingbrand.ecommerce.email;

public interface EmailSender {
    void send(TransactionalEmail email);
}
