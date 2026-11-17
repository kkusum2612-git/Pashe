package com.pashe.app.realtime;

import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class RealtimeConfig implements WebSocketMessageBrokerConfigurer {
    private final String origin;
    private final JdbcTemplate db;
    public RealtimeConfig(@Value("${pashe.auth.frontend-url}") String origin,JdbcTemplate db) { this.origin=origin; this.db=db; }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/v1/ws").setAllowedOrigins(origin).addInterceptors(new HandshakeInterceptor() {
            @Override public boolean beforeHandshake(ServerHttpRequest req,ServerHttpResponse res,WebSocketHandler handler,Map<String,Object> attributes) {
                if (!(req instanceof ServletServerHttpRequest servlet)) return false;
                CsrfToken token=(CsrfToken)servlet.getServletRequest().getAttribute(CsrfToken.class.getName());
                if (token==null) return false;
                attributes.put("csrf",token.getToken()); return true;
            }
            @Override public void afterHandshake(ServerHttpRequest req,ServerHttpResponse res,WebSocketHandler handler,Exception error) {}
        });
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(8192).setSendBufferSizeLimit(32768).setSendTimeLimit(10000);
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message,MessageChannel channel) {
                var frame=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
                if (frame==null || frame.getCommand()==null) return message;
                if (frame.getCommand()==StompCommand.DISCONNECT) return message;
                if (!(frame.getUser() instanceof JwtAuthenticationToken principal)) throw new AccessDeniedException("Authentication required");
                var jwt=principal.getToken();
                Integer active=db.queryForObject("SELECT count(*) FROM auth_sessions s JOIN users u ON u.id=s.user_id WHERE s.id=? AND s.user_id=? AND s.revoked_at IS NULL AND s.expires_at>CURRENT_TIMESTAMP AND u.enabled AND u.email_verified",Integer.class,UUID.fromString(jwt.getClaimAsString("sid")),UUID.fromString(jwt.getSubject()));
                if (jwt.getExpiresAt()==null || jwt.getExpiresAt().isBefore(Instant.now()) || active==null || active!=1) throw new AccessDeniedException("Session expired");
                if (frame.getCommand()==StompCommand.CONNECT) {
                    Object expected=frame.getSessionAttributes()==null ? null : frame.getSessionAttributes().get("csrf");
                    if (expected==null || !expected.equals(frame.getFirstNativeHeader("X-CSRF-TOKEN"))) throw new AccessDeniedException("CSRF required");
                } else if (frame.getCommand()==StompCommand.SEND) {
                    if (!"/app/account/ping".equals(frame.getDestination())) throw new AccessDeniedException("Destination denied");
                } else if (frame.getCommand()==StompCommand.SUBSCRIBE) {
                    if (!"/user/queue/account".equals(frame.getDestination())) throw new AccessDeniedException("Subscription denied");
                } else if (frame.getCommand()!=StompCommand.UNSUBSCRIBE) throw new AccessDeniedException("Command denied");
                return message;
            }
        });
    }
}
