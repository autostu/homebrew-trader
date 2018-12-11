package xyz.homebrew.core;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@ToString
@Accessors(chain = true)
public final class Order {

  String id;

  BigDecimal price;

  BigDecimal amount;

  String symbol;

  String exchange;

  String executedValue;

  String filledAmount;

  String fee;

  String side;

  Long createdAt;
}
