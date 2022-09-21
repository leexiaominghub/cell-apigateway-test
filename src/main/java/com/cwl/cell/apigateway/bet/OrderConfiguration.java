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
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .route(r -> r.path("/test/**").and().method("GET").uri(orderDestinations.getOrderServiceUrl()))
            .build();
  }

  @Bean
  public RouterFunction<ServerResponse> orderHandlerRouting(OrderHandlers orderHandlers) {
    return RouterFunctions.route(GET("/3DBet/bet/{stationID}/{num}/{money}/{orderID}"), orderHandlers::getOrderDetails)
            .andRoute(GET("/test/bet/{id}"), orderHandlers::test);
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
      // 这个是返回默认异常
      Map<String, Object> errorPropertiesMap = getErrorAttributes(request,
              ErrorAttributeOptions.of(ErrorAttributeOptions.Include.EXCEPTION)); // org.springframework.boot.web.reactive.error.DefaultErrorAttributes.getErrorAttributes(org.springframework.web.reactive.function.server.ServerRequest, org.springframework.boot.web.error.ErrorAttributeOptions)会去掉一些属性
              //ErrorAttributeOptions.defaults()); // 默认是空
      errorPropertiesMap.put("cause", getError(request).getCause().getClass().getName());
      // 可以自定义异常返回类
      //Test t = new Test("自定义异常处理");

      return ServerResponse.status(HttpStatus.BAD_REQUEST)
              .contentType(MediaType.APPLICATION_JSON)
              .body(BodyInserters.fromValue(errorPropertiesMap)); // 这边
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
    CustomizeDefaultErrorWebExceptionHandler exceptionHandler = new CustomizeDefaultErrorWebExceptionHandler(errorAttributes, webProperties.getResources(), serverProperties.getError(), applicationContext);
    exceptionHandler.setViewResolvers(viewResolvers.orderedStream().collect(Collectors.toList()));
    exceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
    exceptionHandler.setMessageReaders(serverCodecConfigurer.getReaders());

    return exceptionHandler;
  }

  Optional<ChannelGroup> channelGroup;
  ConnectionPoolMetrics metrics;
  @Bean
  public WebClient webClient() {

    ConnectionProvider provider =
            ConnectionProvider.builder("custom")
                    .maxConnections(50) // 默认500或CPU个数的2倍。
                    .maxIdleTime(Duration.ofSeconds(30)) // 未被使用的时间，默认-1
                    .maxLifeTime(Duration.ofSeconds(300)) // 默认-1
                    //.pendingAcquireTimeout(Duration.ofSeconds(60)) // 向连接池获取连接的超时时间, 默认45s
                    //.evictInBackground(Duration.ofSeconds(30)) // 应该不会清理server的连接，默认不开启
                    //.metrics(true, () -> (poolName, id, remoteAddress, metrics) -> {
                      //log.info("lxm metric alc: " + metrics.allocatedSize() + " idle: " + metrics.idleSize() + " pending: " + metrics.pendingAcquireSize() + " acq: " + metrics.acquiredSize());
                      //this.metrics = metrics; })
                    .build();

    HttpClient client = HttpClient.create(provider) // 优雅退出可能并不需要等待httpclient控制的连接
            //.doOnConnect(httpClientConfig -> this.channelGroup = Optional.ofNullable(httpClientConfig.channelGroup())) // 还是null
            //.doOnDisconnected(connection -> log.info("disconnect " +connection)) // 其实只是释放连接，并不是真的断开连接
            .responseTimeout(Duration.ofSeconds(9)) // 默认无限等待，官档说最佳实践是配置
            //.disableRetry(true) // 只针对连接失效的重试
            .doOnResponseError(((httpClientResponse, throwable) -> log.warn("" + httpClientResponse + "" + throwable)))
            .observe((connection, newState) -> { // httpclient 只负责取用连接
              log.info("lxm " + connection + ": " + newState);
              //log.info("lxm metric alc: " + metrics.allocatedSize() + " idle: " + metrics.idleSize() + " pending: " + metrics.pendingAcquireSize() + " acq: " + metrics.acquiredSize());

/*
        if (running) { // do nothing
          if (newState == HttpServerState.REQUEST_RECEIVED) { // 如果能通过其他方式约定，则可以省略
            ((HttpServerResponse)connection).addHeader(KEEP_ALIVE, "timeout=" + idleTimeout.getSeconds());
          }

          return;
        }

        if (newState == HttpServerState.REQUEST_RECEIVED) {
          ((HttpServerResponse)connection).keepAlive(false);
        }
*/
            });

    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client)).build();

    //return WebClient.create();
  }
}
