package xyz.homebrew.core;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Account {

  Market getHostingMarket();

  void setHostingMarket(Market market);

  Pair<String, String> getSymbol();

  Balance getBalance();

  List<Order> getExecutingOffers();

  List<Order> getExecutingBids();

  CompletableFuture<Optional<Order>> query(String id);

  CompletableFuture<String> buy(BigDecimal price, BigDecimal amount);

  CompletableFuture<String> sell(BigDecimal price, BigDecimal amount);

  CompletableFuture<Void> cancel(String id);
}
