package com.clothingbrand.ecommerce.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]{1,64}$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            
            if (correlationId == null || correlationId.isBlank() || !VALID_PATTERN.matcher(correlationId).matches()) {
                correlationId = UUID.randomUUID().toString();
            } else {
                correlationId = correlationId.trim();
            }

            MDC.put(MDC_KEY, correlationId);
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            httpRequest.setAttribute(MDC_KEY, correlationId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
