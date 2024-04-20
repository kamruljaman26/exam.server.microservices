package api.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements Ordered, WebFilter {

    @Autowired
    private WebClient webClient;

    private final List<String> excludedPaths = List.of("/generate-token","/eureka/**");

    @Override
    public int getOrder() {
        return -100; // High precedence
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Check if the request path is in the excluded list
        if (excludedPaths.contains(path)) {
            return chain.filter(exchange); // Bypass JWT authentication
        }

        // Continue with existing JWT authentication logic
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(authentication -> authentication.isAuthenticated())
                .flatMap(authentication -> webClient.post()
                        .uri("http://user-service/validate")
                        .header("Authorization", exchange.getRequest().getHeaders().getFirst("Authorization"))
                        .retrieve()
                        .bodyToMono(Boolean.class)
                        .flatMap(isValid -> {
                            if (isValid) {
                                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                        .header("X-auth-user", authentication.getName())
                                        .build();
                                return chain.filter(exchange.mutate().request(modifiedRequest).build());
                            } else {
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                            }
                        }))
                .switchIfEmpty(chain.filter(exchange)); // Proceed without authentication if not authenticated
    }
}
