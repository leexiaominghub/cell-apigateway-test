package com.cwl.cell.apigateway.config;


import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.util.AsciiString;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerState;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static io.netty.handler.codec.http.HttpUtil.setKeepAlive;


@Component
@Slf4j
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

  private volatile Boolean running = true;

  private Optional<ChannelGroup> channelGroup;

  private static final AsciiString KEEP_ALIVE = AsciiString.cached("Keep-Alive");

  @Setter
  public Duration idleTimeout = Duration.ofSeconds(300);

  //private Integer activeConnections = 0;

  public void setChannelGroup(ChannelGroup channelGroup) {
    this.channelGroup = Optional.ofNullable(channelGroup); // 或许metricRecord更靠谱
    if (!this.channelGroup.isPresent()) {
      log.warn("lxm Fail to get channel group of server");
    }
  }

  public void stop() {
    this.running = false; // 或许需要同步互斥, 如何保证立刻生效？

    this.channelGroup.ifPresent(cs -> {

      while (cs.size() > 0) { // hook线程被带入这里，尚未启动计时。要小心在这里一直不退出(不断有新连接，或某个channel异常不退出)，外层pod直接退出;因为这里没有关闭连接器
        log.info("lxm waiting " + cs.size() + " conn");
        AtomicInteger channelCnt = new AtomicInteger();
        cs.forEach(channel -> { // 看不见客户端的连接
          channel.closeFuture().awaitUninterruptibly(idleTimeout.plusSeconds(5L).toMillis()); // 要和httpclient responseTimeout配合.如果超时，退出大循环.这里只有server的channel
          channelCnt.getAndIncrement();
        }); // 只会等待目前这里的channel
        log.info("lxm {} conn done. checking if new connection", channelCnt);
      }

      log.info("lxm waiting end " + cs.size() + " conn");
    });
  }

  @Override
  public void customize(ConfigurableWebServerFactory configurableWebServerFactory) {
    ((NettyReactiveWebServerFactory)configurableWebServerFactory).addServerCustomizers(httpServer ->
            httpServer
                    .idleTimeout(idleTimeout)
                    .doOnBind(httpServerConfig -> setChannelGroup(httpServerConfig.channelGroup()))
                    .childObserve((connection, newState) -> {

                      //if (HttpServerState.DISCONNECTING == newState)
                        //if (!(connection instanceof HttpServerResponse))
                        //if (HttpServerState.CONNECTED == newState || HttpServerState.DISCONNECTING == newState)
                          //channelGroup.ifPresent(cs -> log.info("lxm " + connection + ": " + newState + ", now conn: " + cs.size()));

                      if (running) { // do nothing
                        if (newState == HttpServerState.REQUEST_RECEIVED) { // 如果能通过其他方式约定timeout，则可以省略.这个if应该放在外面
                          ((HttpServerResponse)connection).addHeader(KEEP_ALIVE, "timeout=" + idleTimeout.getSeconds());
                          //setKeepAlive((HttpMessage) connection, true); // 更好的写法或许在应用handler那里
                          //if (!((HttpServerRequest)connection).version().isKeepAliveDefault())
                            ((HttpServerResponse)connection).addHeader(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE); // 1.0，如果不设置，traffichanlder会关闭连接
                        }

                        return;
                      }

                      if (newState == HttpServerState.REQUEST_RECEIVED) {
                        ((HttpServerResponse)connection).keepAlive(false);
                      }
                    })
    );
  }

/*
    @Override
    public void customize(ConfigurableWebServerFactory configurableWebServerFactory) {
      ((NettyReactiveWebServerFactory)configurableWebServerFactory).addServerCustomizers(httpServer -> httpServer.idleTimeout(Duration.ofSeconds(60))
              .childObserve((connection, newState) -> {
                log.info("lxm " + connection + ": " + newState);

                if (!stop)
                  return;


                //Objects.requireNonNull(this.httpServer.configuration().channelGroup()).forEach(channel -> log.info(channel.toString()));
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
              })
      );
   }
*/
}
