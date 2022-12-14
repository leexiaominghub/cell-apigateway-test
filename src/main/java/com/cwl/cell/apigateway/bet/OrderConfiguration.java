package com.cwl.cell.apigateway.bet;

import io.netty.channel.group.ChannelGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerState;
import reactor.netty.resources.ConnectionPoolMetrics;
import reactor.netty.resources.ConnectionProvider;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.all;

@Configuration
@EnableConfigurationProperties(OrderDestinations.class)
@Slf4j
public class OrderConfiguration {

  @Bean
  public RouteLocator orderProxyRouting(RouteLocatorBuilder builder, OrderDestinations orderDestinations) {
    return builder.routes()
            .route(r -> r.path("/3DBet/**").and().method("GET").uri(orderDestinations.getOrderServiceUrl()))
            .route(r -> r.path("/onlyCloud/**").and().method("GET").uri(orderDestinations.getOrderServiceUrl()))
            .route(r -> r.path("/test/**").and().method("GET").uri(orderDestinations.getOrderServiceUrl()))
            .build();
  }

  @Bean
  public RouterFunction<ServerResponse> orderHandlerRouting(OrderHandlers orderHandlers) {
    return RouterFunctions.route(GET("/3DBet/bet/{stationID}/{num}/{money}/{orderID}"), orderHandlers::getOrderDetails)
            .andRoute(GET("/test/bet/{id}"), orderHandlers::test)
            .andRoute(GET("/test/local/{id}"), orderHandlers::testLocal);
  }

/*
  public static class ExceptionHandler extends AbstractErrorWebExceptionHandler {

    public ExceptionHandler(ErrorAttributes errorAttributes, WebProperties webProperties, ApplicationContext applicationContext,
                            ObjectProvider<ViewResolver> viewResolvers, ServerCodecConfigurer serverCodecConfigurer) {
      super(errorAttributes, webProperties.getResources(), applicationContext);
      this.setViewResolvers(viewResolvers.orderedStream().collect(Collectors.toList()));
      this.setMessageWriters(serverCodecConfigurer.getWriters());
      this.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
      return RouterFunctions.route(GET("/test/bet/**"), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
      // ???????????????????????????
      Map<String, Object> errorPropertiesMap = getErrorAttributes(request,
              ErrorAttributeOptions.of(ErrorAttributeOptions.Include.EXCEPTION)); // org.springframework.boot.web.reactive.error.DefaultErrorAttributes.getErrorAttributes(org.springframework.web.reactive.function.server.ServerRequest, org.springframework.boot.web.error.ErrorAttributeOptions)?????????????????????
              //ErrorAttributeOptions.defaults()); // ????????????
      errorPropertiesMap.put("cause", getError(request).getCause().getClass().getName());
      // ??????????????????????????????
      //Test t = new Test("?????????????????????");

      return ServerResponse.status(HttpStatus.BAD_REQUEST)
              .contentType(MediaType.APPLICATION_JSON)
              .body(BodyInserters.fromValue(errorPropertiesMap)); // ??????
    }
  }
*/

  public static class CustomizeDefaultErrorWebExceptionHandler extends DefaultErrorWebExceptionHandler {

    public CustomizeDefaultErrorWebExceptionHandler(ErrorAttributes errorAttributes, WebProperties.Resources resources,
                                                    ErrorProperties errorProperties, ApplicationContext applicationContext) {
      super(errorAttributes, resources, errorProperties, applicationContext);
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
      return RouterFunctions.route(GET("/test/bet/**"), this::customizeRenderErrorResponse)
              .andRoute(acceptsTextHtml(), this::renderErrorView)
              .andRoute(all(), this::renderErrorResponse);
    }

    protected Mono<ServerResponse> customizeRenderErrorResponse(ServerRequest request) {
      Map<String, Object> error = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL));
      Throwable throwable = getError(request);
      while (throwable.getCause() != null) {
        throwable = throwable.getCause();
      }
      error.put("Error cause", throwable.getClass().getName());
      return ServerResponse.status(getHttpStatus(error)).contentType(MediaType.APPLICATION_JSON)
              .body(BodyInserters.fromValue(error));
    }
  }

  @Bean
  @Order(-1)
  public CustomizeDefaultErrorWebExceptionHandler customizeDefaultErrorWebExceptionHandler(ErrorAttributes errorAttributes, WebProperties webProperties,
                                                                                           ApplicationContext applicationContext,
                                                                                           ObjectProvider<ViewResolver> viewResolvers,
                                                                                           ServerCodecConfigurer serverCodecConfigurer,
                                                                                           ServerProperties serverProperties) {
    CustomizeDefaultErrorWebExceptionHandler exceptionHandler = new CustomizeDefaultErrorWebExceptionHandler(errorAttributes,
            webProperties.getResources(), serverProperties.getError(), applicationContext);
    exceptionHandler.setViewResolvers(viewResolvers.orderedStream().collect(Collectors.toList()));
    exceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
    exceptionHandler.setMessageReaders(serverCodecConfigurer.getReaders());

    return exceptionHandler;
  }

  Optional<ChannelGroup> channelGroup;
  ConnectionPoolMetrics metrics;
  @Bean
  public WebClient webClient(HttpClientProperties httpClientProperties, HttpClient httpClient) {

/*
    ConnectionProvider provider =
            ConnectionProvider.builder("custom")
                    .maxConnections(50) // ??????500???CPU?????????2??????
                    .maxIdleTime(Duration.ofSeconds(30)) // ??????????????????????????????-1
                    .maxLifeTime(Duration.ofSeconds(300)) // ??????-1
                    //.pendingAcquireTimeout(Duration.ofSeconds(60)) // ???????????????????????????????????????, ??????45s
                    //.evictInBackground(Duration.ofSeconds(30)) // ??????????????????server???????????????????????????
                    .metrics(true, () -> (poolName, id, remoteAddress, metrics) -> {
                      log.info("lxm metric alc: " + metrics.allocatedSize() + " idle: " + metrics.idleSize() + " pending: " + metrics.pendingAcquireSize() + " acq: " + metrics.acquiredSize());
                      this.metrics = metrics; })
                    .build();

    HttpClient client = HttpClient.create(provider) // ????????????????????????????????????httpclient???????????????
            //.doOnConnect(httpClientConfig -> this.channelGroup = Optional.ofNullable(httpClientConfig.channelGroup())) // ??????null
            //.doOnDisconnected(connection -> log.info("disconnect " +connection)) // ??????????????????????????????????????????????????????
            //.disableRetry(true) // ??????????????????????????????
            //.responseTimeout(Duration.ofSeconds(9)) // ???????????????????????????????????????????????????
            .responseTimeout(httpClientProperties.getResponseTimeout()) // ???????????????????????????????????????????????????
            .observe((connection, newState) -> { // httpclient ?????????????????????
              log.info("lxm " + connection + ": " + newState);
              log.info("lxm metric alc: " + metrics.allocatedSize() + " idle: " + metrics.idleSize() + " pending: " + metrics.pendingAcquireSize() + " acq: " + metrics.acquiredSize());

            //});
    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client)).build();
*/


    //return WebClient.create();
    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
  }
}
