package xyz.homebrew.app.strategy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import xyz.homebrew.core.AbstractTrader;
import xyz.homebrew.core.Account;
import xyz.homebrew.core.Balance;
import xyz.homebrew.core.Market;

import java.math.BigDecimal;
import java.util.ArrayList;

@Slf4j
public class AlgowTrader extends AbstractTrader {

  static final int CAPACITY = 15;

  final ArrayList<Pair<BigDecimal, BigDecimal>> buf = new ArrayList<>(CAPACITY + 1);
  
  Account main;

  @Override
  public void init() {
    main = getAccount("fcoin_main");
  }

  void calculate() {
    double offersPriceDiff = 0;
    double bidsPriceDiff = 0;
    double offersAvg = 0;
    double bidsAvg = 0;
    for (int i = 0; i < CAPACITY; i++) {
      if (i != 0) {
        offersPriceDiff += buf.get(i).getKey().subtract(buf.get(i - 1).getKey()).doubleValue();
        bidsPriceDiff += buf.get(i).getValue().subtract(buf.get(i - 1).getValue()).doubleValue();
      }
      offersAvg += buf.get(i).getKey().doubleValue();
      bidsAvg += buf.get(i).getValue().doubleValue();
    }
    offersAvg /= CAPACITY;
    bidsAvg /= CAPACITY;
    double offersVibrate = 0;
    double bidsVibrate = 0;
    for (int i = 0; i < CAPACITY; i++) {
      offersVibrate += offersAvg - buf.get(i).getKey().doubleValue();
      bidsVibrate += bidsAvg - buf.get(i).getValue().doubleValue();
    }
    offersVibrate = Math.round(Math.abs(offersVibrate));
    bidsVibrate = Math.round(Math.abs(bidsVibrate));
    evaluation[0] = offersPriceDiff;
    evaluation[1] = offersVibrate;
    evaluation[2] = bidsPriceDiff;
    evaluation[3] = bidsVibrate;
  }

  double[] evaluation = new double[4];

  @Override
  public boolean spotted(Market active) {
    buf.add(Pair.of(active.offers().getAvgPrice(), active.bids().getAvgPrice()));
    if (buf.size() > CAPACITY) {
      buf.remove(0);
      Balance balance = main.getBalance();
      if (balance.getTradableBase() == null || balance.getTradableQuote() == null ||
          balance.getTradableBase().compareTo(BigDecimal.ZERO) <= 0 && balance.getTradableQuote().compareTo(BigDecimal.ZERO) <= 0) {
        return false;
      }
      calculate();
      System.out.println("offers difference: " + evaluation[0]);
      System.out.println("offers vibrate: " + evaluation[1]);
      System.out.println("bids difference: " + evaluation[2]);
      System.out.println("bids vibrate: " + evaluation[3]);
      System.out.println("offers volume: " + main.getHostingMarket().offers().getVolume());
      System.out.println("bids volume: " + main.getHostingMarket().bids().getVolume());
      System.out.println("offers amount: " + main.getHostingMarket().offers().getTotalAmount());
      System.out.println("bids amount: " + main.getHostingMarket().bids().getTotalAmount());
      System.out.println("offers avg: " + main.getHostingMarket().offers().getAvgPrice());
      System.out.println("bids avg: " + main.getHostingMarket().bids().getAvgPrice());
      System.out.println("best offer: " + main.getHostingMarket().offers().getBest());
      System.out.println("best bid: " + main.getHostingMarket().bids().getBest());
      System.out.println();
      return true;
    }
    return false;
  }

  @Override
  public void execute() {
  }
}
