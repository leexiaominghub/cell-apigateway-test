package com.cwl.cell.apigateway.config;


import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;
import reactor.netty.ConnectionObserver;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerState;

import java.time.Duration;


@Component
@Slf4j
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
  @Override
  public void customize(ConfigurableWebServerFactory configurableWebServerFactory) {
    ((NettyReactiveWebServerFactory)configurableWebServerFactory).addServerCustomizers(httpServer -> httpServer.childObserve((connection, newState) -> {
      log.info("lxm " + connection + ": " + newState);
      if (newState == HttpServerState.REQUEST_RECEIVED) {
        Integer reqCnt = (Integer)connection.channel().attr(AttributeKey.valueOf("reqCnt")).get();
        connection.channel().attr(AttributeKey.valueOf("reqCnt")).set(++reqCnt);

        if (reqCnt >= 2) {
          if (connection instanceof HttpServerResponse)
            ((HttpServerResponse)connection).keepAlive(false);
          log.info("lxm " +reqCnt + " cut conn");
          //connection.markPersistent(false); // 不需要，上面响应close就会自东关闭
        }

      } else if (newState == ConnectionObserver.State.CONNECTED) {
        connection.channel().attr(AttributeKey.valueOf("reqCnt")).set(0);
      }
    }), httpServer -> httpServer.idleTimeout(Duration.ofSeconds(60)));
  }
}
