package com.cwl.cell.apigateway.proxies;

import com.cwl.cell.apigateway.bet.OrderDestinations;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class OrderServiceProxy {

  private final OrderDestinations orderDestinations;

  private final WebClient client;

  public Mono<String> findOrderById(String stationID, String num, String money, String orderID) {

    return client.get()
            .uri(orderDestinations.getOrderServiceUrl() + "/3DBet/bet/{stationID}/{num}/{money}/{orderID}", stationID, num, money, orderID)
            .exchangeToMono(res -> {

              log.info("lxm get " + res.statusCode().name());
              switch (res.statusCode()) {
                case OK:
                  return res.bodyToMono(String.class);
                default:
                  return Mono.error(new RuntimeException("unknown" + res.statusCode()));
              }
            });
  }

  public Mono<String> test2(String id) {
    return client.get()
            .uri(orderDestinations.getOrderServiceUrl() + "/test/bet/{id}", id)
            .exchangeToMono(res -> {

              log.info("lxm get " + res.statusCode().name());
              switch (res.statusCode()) {
                case OK:
                  return res.bodyToMono(String.class); // 在这里可以做循环查询; 或者能否接收推送？
                default:
                  return Mono.error(new RuntimeException("unknown" + res.statusCode()));
              }
            });
  }

}
