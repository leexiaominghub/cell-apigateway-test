package com.cwl.cell.apigateway.bet;

import com.cwl.cell.apigateway.proxies.OrderServiceProxy;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;

@Service
//@AllArgsConstructor
@RequiredArgsConstructor
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


    //InetSocketAddress remoteAddress = serverRequest.exchange().getRequest().getRemoteAddress();
    //String requestID = serverRequest.exchange().getRequest().getId();
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

  //Integer cnt = 0;
  HashMap<Thread, Integer> threadIntegerHashMap = null;
  @PostConstruct
  public void postConstruct() {
    threadIntegerHashMap = new HashMap<>();
  }

  public Mono<ServerResponse> testLocal(ServerRequest serverRequest) {
    Integer now = threadIntegerHashMap.computeIfAbsent(Thread.currentThread(), thread -> 0);
    threadIntegerHashMap.put(Thread.currentThread(), ++now);
    //Thread.currentThread()
    return ServerResponse.ok()
            .bodyValue("lxm " + Thread.currentThread() + " " + now +
                    ", sum: " + threadIntegerHashMap.values().stream().reduce(Integer::sum).orElse(0) +
                    ", pod_name: " + System.getenv("POD_NAME") +
                    "\r\n")
            //.delayElement(Duration.ofMillis(250))
            ;
  }
}
