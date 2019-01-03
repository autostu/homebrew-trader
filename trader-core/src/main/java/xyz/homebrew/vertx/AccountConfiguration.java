package xyz.homebrew.vertx;

import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import xyz.homebrew.core.Contract;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class AccountConfiguration extends Configurable {

  Contract contract;

  Type type;

  String id;

  public AccountConfiguration(JsonObject config) {
    super(config);
    type = Type.valueOf(config.getString("type"));
    id = config.getString("id");
    JsonObject contract = config.getJsonObject("contract");
    this.contract = new Contract(contract.getString("quote"), contract.getString("base"),
            new BigDecimal(contract.getFloat("amount")), contract.getInteger("scale"));
  }
}
