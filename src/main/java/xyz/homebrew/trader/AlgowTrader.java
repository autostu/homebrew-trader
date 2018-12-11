package xyz.homebrew.trader;

import lombok.extern.slf4j.Slf4j;
import xyz.homebrew.core.AbstractTrader;
import xyz.homebrew.core.Market;

@Slf4j
public final class AlgowTrader extends AbstractTrader {

  @Override
  public boolean spotted(Market active) {
    return true;
  }

  @Override
  public void execute() {
//    System.out.println("offers volume: " + getAccount().getHostingMarket().offers().getVolume());
//    System.out.println("bids volume: " + getAccount().getHostingMarket().bids().getVolume());
//    System.out.println("offers amount: " + getAccount().getHostingMarket().offers().getTotalAmount());
//    System.out.println("bids amount: " + getAccount().getHostingMarket().bids().getTotalAmount());
//    System.out.println("offers avg: " +  getAccount().getHostingMarket().offers().getAvgPrice());
//    System.out.println("bids avg: " +  getAccount().getHostingMarket().bids().getAvgPrice());
//    System.out.println("best offer: " + getAccount().getHostingMarket().offers().getBest());
//    System.out.println("best bid: " + getAccount().getHostingMarket().bids().getBest());
    System.out.println();
  }
}
