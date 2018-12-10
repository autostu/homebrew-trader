package xyz.homebrew.core;

import lombok.Data;

import java.math.BigDecimal;

@Data
public final class Trade {

  String platform;

  String taker;

  BigDecimal price;

  BigDecimal amount;

  Long timestamp;
}
