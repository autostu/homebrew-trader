package xyz.homebrew.sdk.fcoin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import io.vertx.core.Context;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import xyz.homebrew.core.Balance;
import xyz.homebrew.core.Market;
import xyz.homebrew.core.Order;
import xyz.homebrew.core.Sdk;
import xyz.homebrew.vertx.AccountConfiguration;
import xyz.homebrew.vertx.VertxAccount;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Sdk("fcoin")
public class FCoinAccount extends VertxAccount {

  private static final String HOST = "https://api.fcoin.com";

  private String key;

  private String secret;

  private WebClient client;

  private Market hostingMarket;

  @Override
  public void config(AccountConfiguration config) {
    key = config.getConfig().getString("key");
    secret = config.getConfig().getString("secret");
    contract = config.getContract();
  }

  @Override
  public void init() throws Exception {
    sync().get(1, TimeUnit.MINUTES);
    vertx.setPeriodic(10_000, l -> {
      sync();
    });
  }

  @Override
  public void start() {
    Context context = vertx.getOrCreateContext();
    WebClientOptions options = new WebClientOptions()
        .setSsl(true)
        .setDefaultPort(443)
        .setDefaultHost("api.fcoin.com")
        .setTrustAll(true);
    ProxyOptions proxyOptions = context.get("proxy");
    if (proxyOptions != null) {
      options = options.setProxyOptions(proxyOptions);
    }
    client = WebClient.create(vertx, options);
  }

  private HttpRequest<Buffer> prepare(HttpMethod method, String relative, JsonObject params) {
    Pair<Long, String> signature = sign(method, HOST + relative, params, secret);
    return client.request(method, relative)
        .putHeader("FC-ACCESS-KEY", key)
        .putHeader("FC-ACCESS-SIGNATURE", signature.getValue())
        .putHeader("FC-ACCESS-TIMESTAMP", String.valueOf(signature.getKey()));
  }

  private CompletableFuture<Void> sync() {
    CompletableFuture<Void> balanceFuture = new CompletableFuture<>();
    prepare(HttpMethod.GET, "/v2/accounts/balance", null)
        .as(map(new TypeReference<JsonBody<List<FCoinBalance>>>() {
        }, balanceFuture))
        .send(r -> {
          if (r.succeeded()) {
            List<FCoinBalance> balances = r.result().body();
            Balance balance = new Balance();
            for (FCoinBalance fcoin : balances) {
              if (contract.getBase().equalsIgnoreCase(fcoin.currency)) {
                balance.setHoldingContracts(contract.units(new BigDecimal(fcoin.available)));
              }
              if (contract.getQuote().equalsIgnoreCase(fcoin.currency)) {
                balance.setFrozenCash(new BigDecimal(fcoin.frozen));
                balance.setTradableCash(new BigDecimal(fcoin.available));
              }
            }
            updateBalance(balance);
            balanceFuture.complete(null);
          } else {
            balanceFuture.completeExceptionally(r.cause());
          }
        });
    return CompletableFuture.allOf(balanceFuture);
  }

  @Override
  public Market getHostingMarket() {
    return hostingMarket;
  }

  @Override
  public void setHostingMarket(Market market) {
    hostingMarket = market;
  }

  @Override
  public CompletableFuture<Optional<Order>> query(String id) {
    CompletableFuture<Optional<Order>> future = new CompletableFuture<>();
    prepare(HttpMethod.GET, String.format("/v2/orders/%s", id), null)
        .as(map(new TypeReference<JsonBody<FCoinOrder>>() {
        }, future))
        .send(r -> {
          if (r.succeeded()) {
            FCoinOrder fcoin = r.result().body();
            if (fcoin == null || Strings.isNullOrEmpty(id)) {
              future.complete(Optional.empty());
            } else {
              Order order = new Order().setExchange("fcoin")
                  .setAmount(new BigDecimal(fcoin.amount))
                  .setPrice(new BigDecimal(fcoin.price))
                  .setCreatedAt(fcoin.createdAt)
                  .setSide(fcoin.side)
                  .setExecutedValue(fcoin.executedValue)
                  .setFee(fcoin.fillFees)
                  .setSymbol(fcoin.symbol)
                  .setId(fcoin.id)
                  .setFilledAmount(fcoin.filledAmount);
              future.complete(Optional.of(order));
            }
          } else {
            future.completeExceptionally(new IllegalStateException("Can't get order from fcoin"));
          }
        });
    return future;
  }

  @Override
  public CompletableFuture<String> buy(BigDecimal price, int units) {
    BigDecimal _price = price.setScale(contract.getScale(), RoundingMode.DOWN);
    BigDecimal _amount = contract.getAmount().multiply(BigDecimal.valueOf(units));
    Balance balance = new Balance();
    BigDecimal volume = _price.multiply(_amount);
    balance.setFrozenCash(getBalance().getFrozenCash().add(volume));
    balance.setTradableCash(getBalance().getTradableCash().subtract(volume));
    balance.setHoldingContracts(getBalance().getHoldingContracts() + units);
    updateBalance(balance);
    return order(_price, _amount, Side.buy);
  }

  @Override
  public CompletableFuture<String> sell(BigDecimal price, int units) {
    BigDecimal _price = price.setScale(contract.getScale(), RoundingMode.DOWN);
    BigDecimal _amount = contract.getAmount().multiply(BigDecimal.valueOf(units));
    return order(_price, _amount, Side.sell);
  }

  @Override
  public CompletableFuture<Void> cancel(String id) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    prepare(HttpMethod.POST, String.format("/v2/orders/%s/submit-cancel", id), null)
        .as(map(new TypeReference<JsonBody<Boolean>>() {
        }, future))
        .send(r -> {
          if (r.succeeded()) {
            future.complete(null);
          } else {
            future.completeExceptionally(new IllegalStateException("Can't cancel order from fcoin"));
          }
          sync();
        });
    return future;
  }

  private CompletableFuture<String> order(BigDecimal price, BigDecimal amount, Side side) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      CompletableFuture<String> future = new CompletableFuture<>();
      future.completeExceptionally(new IllegalArgumentException());
      return future;
    }
    CompletableFuture<String> future = new CompletableFuture<>();
    JsonObject req = new OrderReq().setAmount(amount.toPlainString())
        .setPrice(price.toPlainString())
        .setSymbol(contract.symbol())
        .setSide(side.name())
        .toJson();
    prepare(HttpMethod.POST, "/v2/orders", req)
        .as(map(new TypeReference<JsonBody<String>>() {
        }, future))
        .sendJsonObject(req, r -> {
          if (r.succeeded()) {
            String id = r.result().body();
            future.complete(id);
          } else {
            future.completeExceptionally(new IllegalStateException("Can't ordering into fcoin"));
          }
          sync();
        });
    return future;
  }

  enum Side {
    buy,
    sell,
  }

  @Data
  @Accessors(chain = true)
  private static class OrderReq {
    String symbol;
    String side;
    String type = "limit";
    String price;
    String amount;
    String exchange = "main";
    String account_type = "spot";

    public JsonObject toJson() {
      return JsonObject.mapFrom(this);
    }

    @Override
    public String toString() {
      return JsonObject.mapFrom(this).toString();
    }
  }

  private static Pair<Long, String> sign(HttpMethod method, String uri, JsonObject params, String secret) {
    Long timestamp = System.currentTimeMillis();
    StringBuilder sb = new StringBuilder();
    if (params != null && !params.isEmpty()) {
      params.fieldNames().stream()
          .sorted()
          .forEach(f -> sb.append(f).append('=').append(params.getString(f)).append('&'));
      sb.deleteCharAt(sb.length() - 1);
    }
    String concat = String.format("%s%s%s%s", method.name(), uri, timestamp, sb);
    byte[] base64 = Base64.getEncoder().encode(concat.getBytes());
    byte[] hash = Hashing.hmacSha1(secret.getBytes()).hashBytes(base64).asBytes();
    return Pair.of(timestamp, new String(Base64.getEncoder().encode(hash)));
  }

  @Data
  private static class JsonBody<T> {
    int status;
    T data;
  }

  @Data
  private static class FCoinBalance {
    String currency;
    String available;
    String frozen;
    String balance;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class FCoinOrder {
    String id;
    String symbol;
    String type;
    String side;
    String price;
    String amount;
    String state;
    String executedValue;
    String fillFees;
    String filledAmount;
    Long createdAt;
    String source;
  }

  static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  private static <U> BodyCodec<U> map(TypeReference<JsonBody<U>> typeReference, CompletableFuture<?> future) {
    return BodyCodec.create(buf -> {
      byte[] raw = buf.getBytes();
      try {
        JsonBody<U> body = mapper.readValue(raw, typeReference);
        if (body.status != 0) {
          log.error("Unexpected FCoin response, {}", new String(raw));
          if (future != null) {
            future.completeExceptionally(new IllegalStateException("Illegal response: \n" + new String(raw)));
          }
          return null;
        }
        return body.data;
      } catch (IOException e) {
        log.error("IOException thrown when map resopnse to POJO", e);
        if (future != null) {
          future.completeExceptionally(new IllegalStateException("Illegal response: \n" + new String(raw)));
        }
      }
      return null;
    });
  }
}
