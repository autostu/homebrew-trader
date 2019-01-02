package xyz.homebrew.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Account {

  Contract getContract();

  void setContract(Contract contract);

  Market getHostingMarket();

  void setHostingMarket(Market market);

  Balance getBalance();

  List<Order> getExecutingOffers();

  List<Order> getExecutingBids();

  CompletableFuture<Optional<Order>> query(String id);

  CompletableFuture<String> buy(BigDecimal price, int units);

  CompletableFuture<String> sell(BigDecimal price, int units);

  CompletableFuture<Void> cancel(String id);
}
