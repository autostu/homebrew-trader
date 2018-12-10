package xyz.homebrew.core;

public interface Trader {

  boolean spotted(Market active);

  void execute();

  Account getAccount();
}
