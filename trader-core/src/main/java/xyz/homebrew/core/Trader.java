package xyz.homebrew.core;

import java.util.Map;

public interface Trader {

  void onMarketDepthUpdate(String symbol, Market active);

  Account getAccount(String id);

  void addAccounts(Map<String, ? extends Account> accounts);
}
