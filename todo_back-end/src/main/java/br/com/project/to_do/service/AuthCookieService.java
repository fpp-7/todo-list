package br.com.project.to_do.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {

    @Value("${app.security.cookie.access-name:todo_access_token}")
    private String accessCookieName;

    @Value("${app.security.cookie.refresh-name:todo_refresh_token}")
    private String refreshCookieName;

    @Value("${app.security.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${app.security.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${api.security.token.expiration-minutes:15}")
    private long accessTokenExpirationMinutes;

    @Value("${app.security.refresh-token.expiration-days:7}")
    private long refreshTokenExpirationDays;

    public void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                accessCookieName,
                accessToken,
                Duration.ofMinutes(accessTokenExpirationMinutes)
        ).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                refreshCookieName,
                refreshToken,
                Duration.ofDays(refreshTokenExpirationDays)
        ).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                accessCookieName,
                "",
                Duration.ZERO
        ).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                refreshCookieName,
                "",
                Duration.ZERO
        ).toString());
    }

    public Optional<String> resolveAccessToken(HttpServletRequest request) {
        return resolveCookie(request, accessCookieName);
    }

    public Optional<String> resolveRefreshToken(HttpServletRequest request) {
        return resolveCookie(request, refreshCookieName);
    }

    private Optional<String> resolveCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
