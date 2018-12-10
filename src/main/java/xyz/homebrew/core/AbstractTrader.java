package xyz.homebrew.core;

public abstract class AbstractTrader implements Trader {

  private final Account account;

  protected AbstractTrader(Account account) {
    this.account = account;
  }

  @Override
  public Account getAccount() {
    return account;
  }
}
