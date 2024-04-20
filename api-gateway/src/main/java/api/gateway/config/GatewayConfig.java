package api.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p
                        .path("/category/**")
                        .uri("lb://exam-server"))
                .route(p -> p
                        .path("/question/**")
                        .uri("lb://exam-server"))
                .route(p -> p
                        .path("/quiz/**")
                        .uri("lb://exam-server"))
                .route(p -> p
                        .path("/user/**")
                        .uri("lb://user-server"))
                .route(p -> p
                        .path("/**")
                        .uri("lb://user-server"))

                .build();
    }
}


