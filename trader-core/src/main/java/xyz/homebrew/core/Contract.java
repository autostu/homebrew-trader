package xyz.homebrew.core;

import io.vertx.core.json.JsonObject;
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

  public Contract(JsonObject config) {
    quote = config.getString("quote");
    base = config.getString("base");
    amount = new BigDecimal(config.getFloat("amount"));
    scale = config.getInteger("scale");
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
