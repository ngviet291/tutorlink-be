package org.group3.tutorlink.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.group3.tutorlink.common.utils.AppUtil;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrackingContextFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "requestId";
    public static final String SESSION_ID = "sessionId";
    private final AppUtil appUtil;

    public TrackingContextFilter(AppUtil appUtil) {
        this.appUtil = appUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = Optional.ofNullable(req.getHeader("X-Request-Id"))
                .filter(s -> !s.isBlank() && s.length() <= 64)
                .orElse(appUtil.generateUUID().toString());
        String sessionId = req.getHeader("X-Session-Id");

        MDC.put(REQUEST_ID, requestId);
        if (sessionId != null && sessionId.length() <= 64) {
            MDC.put(SESSION_ID, sessionId);
        }
        res.setHeader("X-Request-Id", requestId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(REQUEST_ID);
            MDC.remove(SESSION_ID);
        }
    }
}