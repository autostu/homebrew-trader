package xyz.homebrew.core;

public interface Market {

  Orderbook offers();

  Orderbook bids();

  TradeHistory history();

  void registerTrader(Trader trader);
}
