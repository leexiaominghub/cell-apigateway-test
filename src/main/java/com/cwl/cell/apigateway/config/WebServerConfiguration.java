package com.cwl.cell.apigateway.config;


import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

//当Spring容器内没有TomcatEmbeddedServletContainerFactory这个bean时，会吧此bean加载进spring容器中
//@Component
/*
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
  @Override
  public void customize(ConfigurableWebServerFactory configurableWebServerFactory) {
    //使用对应工厂类提供给我们的接口定制化我们的tomcat connector
    ((NettyReactiveWebServerFactory)configurableWebServerFactory).addServerCustomizers(new NettyServerCustomizer() {
      @Override
      public void customize(Connector connector) {
        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

        //定制化keepAliveTimeout,设置30秒内没有请求则服务端自动断开keepalive链接
        protocol.setKeepAliveTimeout(30000);
        //当客户端发送超过10000个请求则自动断开keepalive链接
        protocol.setMaxKeepAliveRequests(30);
      }
    });
  }
}
*/
public class WebServerConfiguration implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {
  @Override
  public void customize(NettyReactiveWebServerFactory configurableWebServerFactory) {
    //qconfigurableWebServerFactory.setP
  }
}
