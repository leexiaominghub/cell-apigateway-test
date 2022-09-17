package com.cwl.cell.apigateway.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@ToString
public enum OrderState {
  APPROVAL_PENDING(0, "处理中"),
  APPROVED(1, "已完成"),
  REJECTED(2, "已拒绝"),
  REPEATED(3, "重复订单"),
  NONSEXIST(4, "订单不存在"),
  CANCEL_PENDING(5, ""),
  CANCELLED(6, ""),
  REVISION_PENDING(7, "");

  @Getter
  //@JsonValue
  private final int code;
  @Getter
  private final String description;
}

