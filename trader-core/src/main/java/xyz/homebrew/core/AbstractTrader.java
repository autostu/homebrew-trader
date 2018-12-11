package xyz.homebrew.core;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractTrader implements Trader {

  private final CopyOnWriteArrayList<Account> accounts = new CopyOnWriteArrayList<>();

  public abstract void init();

  @Override
  public void addAccounts(Collection<? extends Account> accounts) {
    this.accounts.addAll(accounts);
  }

  @Override
  public List<Account> getAccounts() {
    return accounts;
  }
}
