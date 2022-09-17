package com.cwl.cell.apigateway.bet;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
@EnableConfigurationProperties(OrderDestinations.class)
public class OrderConfiguration {

  @Bean
  public RouteLocator orderProxyRouting(RouteLocatorBuilder builder, OrderDestinations orderDestinations) {
    return builder.routes()
            .route(r -> r.path("/3DBet/**").and().method("GET").uri(orderDestinations.getOrderServiceUrl()))
            .route(r -> r.path("/test/**").and().method("GET").uri(orderDestinations.getOrderServiceUrl()))
            .build();
  }

  @Bean
  public RouterFunction<ServerResponse> orderHandlerRouting(OrderHandlers orderHandlers) {
    return RouterFunctions.route(GET("/3DBet/bet/{stationID}/{num}/{money}/{orderID}"), orderHandlers::getOrderDetails)
                          .andRoute(GET("/test/bet/{id}"), orderHandlers::test);
  }


  @Bean
  public WebClient webClient() {

    ConnectionProvider provider =
            ConnectionProvider.builder("custom")
                    .maxConnections(50)
                    .maxIdleTime(Duration.ofSeconds(10))
                    .maxLifeTime(Duration.ofSeconds(20))
                    .pendingAcquireTimeout(Duration.ofSeconds(60))
                    //.evictInBackground(Duration.ofSeconds(5))
                    .evictInBackground(Duration.ofSeconds(120))
                    .build();

    HttpClient client = HttpClient.create(provider);

    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client)).build();

    //return WebClient.create();
  }
}
