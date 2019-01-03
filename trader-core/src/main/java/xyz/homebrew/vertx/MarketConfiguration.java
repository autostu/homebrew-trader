package xyz.homebrew.vertx;

import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class MarketConfiguration extends Configurable {

  Type type;

  Set<String> symbols;

  public MarketConfiguration(JsonObject config) {
    super(config);
    type = Type.valueOf(config.getString("type"));
    symbols = config.getJsonArray("symbols").stream().map(o -> (String)o).collect(Collectors.toSet());
  }
}
