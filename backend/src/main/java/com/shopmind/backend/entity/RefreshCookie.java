package com.shopmind.backend.entity;



import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** The refresh token lives in an httpOnly cookie so JavaScript (and XSS) cannot read it. */
@Component
public class RefreshCookie {

    public static final String NAME = "refresh_token";

    @Value("${cookie.secure:false}")
    private boolean secure;

    @Value("${jwt.refresh-expiration-days:14}")
    private long days;

    public RefreshCookie() {}

    public RefreshCookie(boolean secure, long days) {
        this.secure = secure;
        this.days = days;
    }

    public void set(HttpServletResponse res, String value) {
        res.addHeader(HttpHeaders.SET_COOKIE, build(value, Duration.ofDays(days)).toString());
    }

    public void clear(HttpServletResponse res) {
        res.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
