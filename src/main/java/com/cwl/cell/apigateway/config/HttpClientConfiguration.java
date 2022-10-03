package com.cwl.cell.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
public class HttpClientConfiguration implements HttpClientCustomizer {
  @Override
  public HttpClient customize(HttpClient httpClient) {
    return httpClient.responseTimeout(Duration.ofSeconds(30))
            //.observe(((connection, newState) -> log.info("lxm " + connection + ": " + newState) ) )
            ;
  }
}
