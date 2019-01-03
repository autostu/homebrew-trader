package xyz.homebrew.vertx;

import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(chain = true)
public abstract class Configurable {

  JsonObject config;

  protected Configurable(JsonObject config) {
    this.config = config;
  }
}
