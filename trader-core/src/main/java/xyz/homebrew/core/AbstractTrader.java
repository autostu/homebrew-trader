package xyz.homebrew.core;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTrader implements Trader {

  private final Map<String, Account> accounts = new HashMap<>();

  public abstract void init();

  @Override
  public void addAccounts(Map<String, ? extends Account> accounts) {
    this.accounts.putAll(accounts);
  }

  @Override
  public Account getAccount(String id) {
    return accounts.get(id);
  }
}
