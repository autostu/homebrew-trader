package xyz.homebrew;

import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import xyz.homebrew.core.Trader;
import xyz.homebrew.trader.AlgowTrader;
import xyz.homebrew.vertx.Configurations;
import xyz.homebrew.vertx.FCoinAccount;
import xyz.homebrew.vertx.FCoinMarket;

public final class Bootstrap {

  public static void main(String[] args) {
    JsonObject config = Configurations.load();
    int cpu = Runtime.getRuntime().availableProcessors();
    VertxOptions options = new VertxOptions().setWorkerPoolSize(cpu).setEventLoopPoolSize(cpu);
    Vertx vertx = Vertx.vertx(options);
    Context context = vertx.getOrCreateContext();
    if (config.containsKey("proxy")) {
      context.put("proxy", new ProxyOptions(config.getJsonObject("vertx").getJsonObject("proxy")));
    }
    FCoinMarket market = new FCoinMarket();
    vertx.deployVerticle(market, new DeploymentOptions().setConfig(config.getJsonObject("markets").getJsonObject("fcoin")));
    FCoinAccount account = new FCoinAccount(market);
    vertx.deployVerticle(account, new DeploymentOptions().setConfig(config.getJsonObject("accounts").getJsonObject("fcoin")));
    Trader algow = new AlgowTrader(account);
    market.registerTrader(algow);
  }
}
