package xyz.homebrew.vertx;

import io.vertx.core.AbstractVerticle;
import xyz.homebrew.core.Market;

import java.util.HashSet;
import java.util.Set;

public abstract class VertxMarket extends AbstractVerticle implements Market {

  protected final Set<String> symbols = new HashSet<>();

  @Override
  public void subscribe(String symbol) {
    symbols.add(symbol);
  }
}
