package xyz.homebrew.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import xyz.homebrew.core.Market;
import xyz.homebrew.core.Orderbook;
import xyz.homebrew.core.TradeHistory;
import xyz.homebrew.core.Trader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Slf4j
public class FCoinMarket extends AbstractVerticle implements Market {

  @Override
  public void start() {
    init();
  }

  private void init() {
    Context context = vertx.getOrCreateContext();
    HttpClientOptions options = new HttpClientOptions()
        .setSsl(true)
        .setDefaultPort(443)
        .setDefaultHost("api.fcoin.com")
        .setTrustAll(true);
    ProxyOptions proxyOptions = context.get("proxy");
    if (proxyOptions != null) {
      options = options.setProxyOptions(proxyOptions);
    }
    HttpClient client = vertx.createHttpClient(options);
    int baseCurrencyScale = context.config().getInteger("baseCurrencyScale", 10);
    int quoteCurrencyScale = context.config().getInteger("quoteCurrencyScale", 10);
    client.websocket("/v2/ws", ws -> {
      long pingTimer = vertx.setPeriodic(15_000, l -> {
        Directive ping = new Directive()
            .setCmd("ping")
            .setArgs(new JsonArray().add(System.currentTimeMillis()))
            .setId("ignore-response");
        try {
          ws.writeTextMessage(ping.toString());
        } catch (IllegalStateException ignore) {
        }
      });
      AtomicLong lastMessage = new AtomicLong(System.currentTimeMillis());
      vertx.setPeriodic(3_000, countdown -> {
        if (System.currentTimeMillis() - lastMessage.get() > 10_000) {
          try {
            log.warn("no data received since {}, prepare to reconnect", lastMessage.get());
            vertx.cancelTimer(countdown);
            ws.close();
          } catch (IllegalStateException ignore) {
          }
        }
      });
      ws.exceptionHandler(err -> {
        log.warn("Something went wrong during websocket handler", err);
        ws.close();
      });
      ws.closeHandler(v -> {
        log.info("websocket closed, stop pinging server");
        vertx.cancelTimer(pingTimer);
        init();
      });
      SubscribeEventDispatcher dispatcher = new SubscribeEventDispatcher(lastMessage);
      Function<JsonArray, List<Pair<BigDecimal, BigDecimal>>> mapper = array -> {
        List<Pair<BigDecimal, BigDecimal>> r = new LinkedList<>();
        Optional.ofNullable(array).ifPresent(x -> {
          for (int i = 0; i < x.size(); i += 2) {
            r.add(Pair.of(new BigDecimal(x.getDouble(i)).setScale(quoteCurrencyScale, RoundingMode.UP),
                new BigDecimal(x.getDouble(i + 1)).setScale(baseCurrencyScale, RoundingMode.UP)));
          }
        });
        return r;
      };
      dispatcher.register("depth.L150.btcusdt",
          json -> Pair.of(mapper.apply(json.getJsonArray("asks")), mapper.apply(json.getJsonArray("bids"))),
          offersAndBids -> {
            offers.update(offersAndBids.getLeft());
            bids.update(offersAndBids.getRight());
            traders.stream().filter(t -> t.spotted(this)).forEach(t -> t.execute());
          });
      // TODO register trade listener
      ws.handler(dispatcher);
      Directive depth = new Directive()
          .setCmd("sub")
          .setArgs(new JsonArray().add("depth.L150.btcusdt"))
          .setId("sub-depth-with-no-shit");
      ws.writeTextMessage(depth.toString());
      Directive trade = new Directive()
          .setCmd("sub")
          .setArgs(new JsonArray().add("trade.btcusdt"))
          .setId("sub-trade-with-no-shit");
      ws.writeTextMessage(trade.toString());
    }, err -> {
      log.warn("Unable to connect to fcoin");
      vertx.setTimer(10_000, v -> init());
    });
  }

  private static class SubscribeEventDispatcher implements Handler<Buffer> {

    Map<String, Handler<Object>> handlers = new ConcurrentHashMap<>();

    Map<String, Function<JsonObject, Object>> codecs = new ConcurrentHashMap<>();

    AtomicLong lastMessage;

    SubscribeEventDispatcher(AtomicLong lastMessage) {
      this.lastMessage = lastMessage;
    }

    @Override
    public void handle(Buffer event) {
      try {
        lastMessage.set(System.currentTimeMillis());
        JsonObject data = new JsonObject(event);
        if (data.containsKey("type")) {
          Function<JsonObject, Object> codec = codecs.get(data.getString("type"));
          Handler<Object> handler = handlers.get(data.getString("type"));
          if (handler != null && codec != null) {
            handler.handle(codec.apply(data));
          }
        }
      } catch (Throwable t) {
        log.warn("Fail to handle websocket message", t);
      }
    }

    @SuppressWarnings("unchecked")
    public <T> void register(String type, Function<JsonObject, T> codec, Handler<T> handler) {
      handlers.put(type, (Handler<Object>) handler);
      codecs.put(type, (Function<JsonObject, Object>) codec);
    }
  }

  private final Orderbook offers = new Orderbook(Comparator.naturalOrder());

  private final Orderbook bids = new Orderbook(Comparator.reverseOrder());

  private final List<Trader> traders = new CopyOnWriteArrayList<>();

  @Override
  public Orderbook offers() {
    return offers;
  }

  @Override
  public Orderbook bids() {
    return bids;
  }

  @Override
  public TradeHistory history() {
    return null;
  }

  @Override
  public void registerTrader(Trader trader) {
    traders.add(trader);
  }

  @Data
  @Accessors(chain = true)
  private static class Directive {

    String cmd;

    JsonArray args;

    String id;

    @Override
    public String toString() {
      return JsonObject.mapFrom(this).toString();
    }
  }
}
