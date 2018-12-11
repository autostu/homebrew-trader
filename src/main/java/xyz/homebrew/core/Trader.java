package xyz.homebrew.core;

import java.util.Collection;
import java.util.List;

public interface Trader {

  boolean spotted(Market active);

  void execute();

  List<Account> getAccounts();

  void addAccounts(Collection<? extends Account> accounts);
}
