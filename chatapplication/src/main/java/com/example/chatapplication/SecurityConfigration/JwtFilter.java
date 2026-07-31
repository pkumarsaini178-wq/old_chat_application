package com.example.chatapplication.SecurityConfigration;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.chatapplication.ResigtrationFolder.ChatSingin;
import com.example.chatapplication.ResigtrationFolder.ChatSinginRepo;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUntil jwtUntil;

    @Autowired
    private com.example.chatapplication.TokenBlacklistService tokenBlacklistService;

    @Autowired
    private ChatSinginRepo chatSinginRepo;

    // Refresh token if older than 6 days (within the 7-day window)
    private static final long REFRESH_THRESHOLD_MS = 6 * 24 * 60 * 60 * 1000L;
    private static final long ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain ) throws jakarta.servlet.ServletException, java.io.IOException
                                    {
                                        String token=null;
                                        Cookie[] cookies=request.getCookies();
                                        if(cookies!=null)
                                        {
                                            for(Cookie cookie: cookies)
                                            {
                                                if("jwt".equals(cookie.getName()))
                                                {
                                                    token=cookie.getValue();
                                                    break;
                                                }
                                            }
                                        }
                                         if(token!=null)
                                         {
                                             if(jwtUntil.Token_is_vailid(token) && !tokenBlacklistService.isTokenBlacklisted(token))
                                             {
                                                String email=jwtUntil.FechEmailfromToke(token);  
                                                java.util.Optional<ChatSingin> userOpt = chatSinginRepo.findByuseremail(email);

                                                boolean isUserActive = false;
                                                if (userOpt.isPresent()) {
                                                    ChatSingin user = userOpt.get();
                                                    if (Boolean.TRUE.equals(user.getIsBlocked())) {
                                                        if (user.getBlockExpiry() != null && java.time.LocalDateTime.now().isAfter(user.getBlockExpiry())) {
                                                            // Block has expired, automatically unblock
                                                            user.setIsBlocked(false);
                                                            user.setBlockExpiry(null);
                                                            chatSinginRepo.save(user);
                                                            isUserActive = true;
                                                        } else {
                                                            isUserActive = false;
                                                        }
                                                    } else {
                                                        isUserActive = true;
                                                    }
                                                }

                                                if (isUserActive) {
                                                    if(SecurityContextHolder.getContext().getAuthentication()==null)
                                                    {
                                                        UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken( email,null,new ArrayList<>());
                                                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                                        SecurityContextHolder.getContext().setAuthentication(auth);
                                                    }

                                                    // Sliding-window token refresh: if token is older than 6 days, issue a fresh one
                                                    long tokenAge = jwtUntil.getTokenAge(token);
                                                    if (tokenAge > REFRESH_THRESHOLD_MS) {
                                                        String newToken = jwtUntil.gunrateToken(email);
                                                        String setCookieHeader = "jwt=" + newToken
                                                                + "; Path=/"
                                                                + "; Max-Age=" + (ONE_WEEK_MS / 1000)
                                                                + "; HttpOnly; Secure; SameSite=None";
                                                        response.addHeader("Set-Cookie", setCookieHeader);
                                                    }
                                                } else {
                                                    // User is blocked or deleted: clear security context & clear cookie
                                                    SecurityContextHolder.clearContext();
                                                    String clearCookieHeader = "jwt=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None";
                                                    response.addHeader("Set-Cookie", clearCookieHeader);
                                                }
                                            }
                                            else
                                            {
                                                // Clear invalid cookie to prevent console flooding
                                                SecurityContextHolder.clearContext();
                                                String clearCookieHeader = "jwt=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None";
                                                response.addHeader("Set-Cookie", clearCookieHeader);
                                            }
                                        }
                                        chain.doFilter(request, response);
                                        
                                    }

}
