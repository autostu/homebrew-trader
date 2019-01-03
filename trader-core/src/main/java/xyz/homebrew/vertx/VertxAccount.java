package xyz.homebrew.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import xyz.homebrew.core.Account;
import xyz.homebrew.core.Balance;
import xyz.homebrew.core.Contract;
import xyz.homebrew.core.Order;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class VertxAccount extends AbstractVerticle implements Account {

  private Balance balance = new Balance();

  protected Contract contract;

  protected final CopyOnWriteArrayList<Order> offers = new CopyOnWriteArrayList<>();

  protected final CopyOnWriteArrayList<Order> bids = new CopyOnWriteArrayList<>();

  private final ReentrantReadWriteLock balanceLock = new ReentrantReadWriteLock();

  private final CompletableFuture<Void> deployFuture = new CompletableFuture<>();

  public abstract void config(AccountConfiguration config);

  public abstract void init() throws Exception;

  public CompletableFuture<Void> getDeployFuture() {
    return deployFuture;
  }

  public void completeDeployment() {
    deployFuture.complete(null);
  }

  public void failDeployment(Throwable t) {
    deployFuture.completeExceptionally(t);
  }

  @Override
  public Contract getContract() {
    return contract;
  }

  @Override
  public void setContract(Contract contract) {
    this.contract = contract;
  }

  @Override
  public Balance getBalance() {
    try {
      balanceLock.readLock().lock();
      return balance;
    } finally {
      balanceLock.readLock().unlock();
    }
  }

  @Override
  public CopyOnWriteArrayList<Order> getExecutingOffers() {
    return offers;
  }

  @Override
  public CopyOnWriteArrayList<Order> getExecutingBids() {
    return bids;
  }

  public void updateBalance(Balance balance) {
    try {
      balanceLock.writeLock().lock();
      this.balance = balance;
    } finally {
      balanceLock.writeLock().unlock();
    }
  }
}
