package xyz.homebrew.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.reactivex.redis.RedisClient;
import xyz.homebrew.core.TradeHistory;

public class RedisTradeHistory extends AbstractVerticle implements TradeHistory {

  private RedisClient redis;

  @Override
  public void start() {

  }
}
