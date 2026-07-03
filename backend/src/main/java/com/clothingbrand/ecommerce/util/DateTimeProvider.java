package com.clothingbrand.ecommerce.util;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class DateTimeProvider {

    private Clock clock = Clock.systemUTC();

    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void reset() {
        this.clock = Clock.systemUTC();
    }
}
