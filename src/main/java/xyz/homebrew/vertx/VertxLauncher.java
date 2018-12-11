package xyz.homebrew.vertx;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;

import java.lang.reflect.InvocationTargetException;

public class VertxLauncher {

  // TODO
  public static void launch() {
    JsonObject config = Configurations.load();
    int cpu = Runtime.getRuntime().availableProcessors();
    VertxOptions options = new VertxOptions().setWorkerPoolSize(cpu).setEventLoopPoolSize(cpu);
    Vertx vertx = Vertx.vertx(options);
    Context context = vertx.getOrCreateContext();
    if (config.containsKey("proxy")) {
      context.put("proxy", new ProxyOptions(config.getJsonObject("vertx").getJsonObject("proxy")));
    }
    Type type = Type.valueOf("fcoin");
    try {
      VertxAccount account = type.account.getConstructor().newInstance();
      vertx.deployVerticle(account);
      VertxMarket market = type.market.getConstructor().newInstance();
      account.setHostingMarket(market);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }
}
