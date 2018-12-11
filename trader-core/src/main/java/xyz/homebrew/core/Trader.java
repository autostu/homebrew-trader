package xyz.homebrew.core;

import java.util.Map;

public interface Trader {

  boolean spotted(Market active);

  void execute();

  Account getAccount(String id);

  void addAccounts(Map<String, Account> accounts);
}
