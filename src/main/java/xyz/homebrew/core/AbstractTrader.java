package xyz.homebrew.core;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractTrader implements Trader {

  private final CopyOnWriteArrayList<Account> accounts = new CopyOnWriteArrayList<>();

  @Override
  public void addAccount(Account... account) {
    accounts.addAll(Arrays.asList(account));
  }

  @Override
  public List<Account> getAccounts() {
    return accounts;
  }
}
