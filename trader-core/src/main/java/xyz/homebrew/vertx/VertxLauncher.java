package xyz.homebrew.vertx;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import lombok.extern.slf4j.Slf4j;
import xyz.homebrew.core.AbstractTrader;
import xyz.homebrew.core.Market;
import xyz.homebrew.core.Trader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public final class VertxLauncher {

  private VertxLauncher() {
  }

  public static void launch() {
    try {
      int cpu = Runtime.getRuntime().availableProcessors();
      VertxOptions options = new VertxOptions().setWorkerPoolSize(cpu).setEventLoopPoolSize(cpu)
              .setBlockedThreadCheckInterval(3_600_000);
      Vertx vertx = Vertx.vertx(options);
      Context context = vertx.getOrCreateContext();
      JsonObject global = Configurations.load();
      if (global.containsKey("proxy")) {
        context.put("proxy", new ProxyOptions(global.getJsonObject("proxy")));
        log.info("Using proxy config: {}", global.getJsonObject("proxy"));
      }
      JsonArray tradersConfig = global.getJsonArray("traders");

      List<MarketConfiguration> marketConfigurations = global.getJsonArray("markets", new JsonArray())
              .stream().map(o -> new MarketConfiguration((JsonObject) o)).collect(Collectors.toList());
      List<AccountConfiguration> accountConfigurations = global.getJsonArray("accounts", new JsonArray())
              .stream().map(o -> new AccountConfiguration((JsonObject) o)).collect(Collectors.toList());

      Map<String, VertxAccount> accounts = new HashMap<>();
      Map<String, VertxMarket> markets = new HashMap<>();

      for (MarketConfiguration config : marketConfigurations) {
        VertxMarket market = config.getType().getMarket().getConstructor().newInstance();
        config.symbols.forEach(s -> market.subscribe(s));
        markets.put(config.getType().getId(), market);
        log.info("Found market [{}]", config.getType().getId());
      }

      for (AccountConfiguration config : accountConfigurations) {
        VertxAccount account = config.getType().getAccount().getConstructor().newInstance();
        account.config(config);
        Market hostingMarket = markets.get(config.getType().getId());
        if (hostingMarket == null) {
          log.error("Couldn't load market configuration of [{}]");
          throw new IllegalStateException("market config is absent");
        }
        account.setHostingMarket(hostingMarket);
        accounts.put(config.getId(), account);
        log.info("Found account [{}] of [{}]", config.getId(), config.getType().getId());
      }

      accounts.forEach((id, account) -> vertx.deployVerticle(account, ar -> {
        if (ar.succeeded()) {
          CompletableFuture.runAsync(() -> {
            try {
              account.init();
              account.completeDeployment();
              log.info("Account [{}] ready", id);
            } catch (Exception e) {
              account.failDeployment(e);
              log.error("Launch account [{}] failed", id, e);
            }
          });
        } else {
          log.error("Launch account [{}] failed", id);
          account.failDeployment(ar.cause());
        }
      }));
      Map<String, Set<Trader>> watches = new HashMap<>();
      @SuppressWarnings("rawtypes")
      CompletableFuture[] allTradersReady = new CompletableFuture[tradersConfig.size()];
      for (int i = 0; i < tradersConfig.size(); i++) {
        CompletableFuture<Void> traderReady = new CompletableFuture<>();
        JsonObject config = tradersConfig.getJsonObject(i);
        AbstractTrader trader = (AbstractTrader) Class.forName(config.getString("class")).getConstructor().newInstance();
        Map<String, VertxAccount> managedAccounts = config.getJsonArray("accounts").stream().map(a -> (String) a)
                .collect(Collectors.toMap(a -> a, a -> accounts.get(a)));
        trader.addAccounts(managedAccounts);
        @SuppressWarnings("rawtypes")
        CompletableFuture[] futs = new CompletableFuture[managedAccounts.size()];
        CompletableFuture.allOf(managedAccounts.values().stream().map(VertxAccount::getDeployFuture)
                .collect(Collectors.toList())
                .toArray(futs))
                .whenComplete((v, e) -> {
                  if (e == null) {
                    trader.init();
                    config.getJsonArray("watches").stream().map(m -> (String) m)
                            .forEach(m -> watches.computeIfAbsent(m, k -> new HashSet<>()).add(trader));
                    traderReady.complete(null);
                    log.info("Trader {} initialized with {} account(s) under management", config.getString("class"), managedAccounts.size());
                  }
                });
        allTradersReady[i] = traderReady;
      }
      CompletableFuture.allOf(allTradersReady).thenAccept(v -> {
        markets.forEach((type, market) -> vertx.deployVerticle(market, ar -> {
          if (ar.succeeded()) {
            log.info("Market [{}] ready", type);
            watches.get(type).forEach(t -> {
              market.registerTrader(t);
              log.info("Trader {} start watching market [{}]", t, type);
            });
          } else {
            log.error("Launch market [{}] failed", type);
          }
        }));
      });
    } catch (Exception e) {
      log.error("Launch homebrew failed", e);
    }
  }
}
