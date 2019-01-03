package xyz.homebrew.core;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Accessors(chain = true)
public final class Contract {

  private final String quote;

  private final String base;

  private final BigDecimal amount;

  private final int scale;

  public Contract(String quote, String base, BigDecimal amount, int scale) {
    this.quote = quote;
    this.base = base;
    this.amount = amount;
    this.scale = scale;
  }

  public BigDecimal volume(BigDecimal price) {
    return amount.multiply(price).setScale(scale, RoundingMode.DOWN);
  }

  public int units(BigDecimal base) {
    return base.divide(amount, RoundingMode.DOWN).intValue();
  }

  public String symbol() {
    return base + quote;
  }
}
