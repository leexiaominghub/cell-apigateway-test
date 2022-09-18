package com.cwl.cell.apigateway.bet;

import com.cwl.cell.apigateway.proxies.OrderServiceProxy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Service
@AllArgsConstructor
@Slf4j
public class OrderHandlers {

  private final OrderServiceProxy orderService;

  public Mono<ServerResponse> getOrderDetails(ServerRequest serverRequest) {

    String stationID = serverRequest.pathVariable("stationID");
    String num = serverRequest.pathVariable("num");
    String money = serverRequest.pathVariable("money");
    String orderID = serverRequest.pathVariable("orderID");

    log.info("lxm: {}, {}, {}, {}", stationID, num, money, orderID);

    return orderService.findOrderById(stationID, num, money, orderID)
            .flatMap(o -> ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(o)
                    .onErrorResume(RuntimeException.class, e -> ServerResponse.notFound().build())
            );
  }

  public Mono<ServerResponse> test(ServerRequest serverRequest) {

    String id = serverRequest.pathVariable("id");


    InetSocketAddress remoteAddress = serverRequest.exchange().getRequest().getRemoteAddress();
    String requestID = serverRequest.exchange().getRequest().getId();

    //log.info("remoteAddr {}, {}", remoteAddress, requestID);
    log.info("lxm: " + id);

    return orderService.test2(id)
            .flatMap(o -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(o)
                            .onErrorResume(RuntimeException.class, e -> ServerResponse.notFound().build())

                    // 或许在这里做订阅kafka的消息更合适？
            );
  }


}
