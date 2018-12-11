package xyz.homebrew.trader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import xyz.homebrew.core.AbstractTrader;
import xyz.homebrew.core.Market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class AlgowTrader extends AbstractTrader {

  static final int CAPACITY = 15;

  final AtomicReference<BigDecimal> asset = new AtomicReference<>();

  final AtomicReference<BigDecimal> cash = new AtomicReference<>();

  final ArrayList<Pair<BigDecimal, BigDecimal>> buf = new ArrayList<>(CAPACITY + 1);

  @Override
  public void init() {
    syncAssets();
  }

  private void syncAssets() {
    getAccounts().get(0).getBalance().thenAccept(p -> {
      asset.set(p.getTradableBase());
      cash.set(p.getTradableQuote());
    });
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
      if (asset.get() == null || cash.get() == null ||
          asset.get().compareTo(BigDecimal.ZERO) <= 0 && cash.get().compareTo(BigDecimal.ZERO) <= 0) {
        return false;
      }
      calculate();
      System.out.println("offers difference: " + evaluation[0]);
      System.out.println("offers vibrate: " + evaluation[1]);
      System.out.println("bids difference: " + evaluation[2]);
      System.out.println("bids vibrate: " + evaluation[3]);
      System.out.println("offers volume: " + getAccounts().get(0).getHostingMarket().offers().getVolume());
      System.out.println("bids volume: " + getAccounts().get(0).getHostingMarket().bids().getVolume());
      System.out.println("offers amount: " + getAccounts().get(0).getHostingMarket().offers().getTotalAmount());
      System.out.println("bids amount: " + getAccounts().get(0).getHostingMarket().bids().getTotalAmount());
      System.out.println("offers avg: " + getAccounts().get(0).getHostingMarket().offers().getAvgPrice());
      System.out.println("bids avg: " + getAccounts().get(0).getHostingMarket().bids().getAvgPrice());
      System.out.println("best offer: " + getAccounts().get(0).getHostingMarket().offers().getBest());
      System.out.println("best bid: " + getAccounts().get(0).getHostingMarket().bids().getBest());
      System.out.println();
      return true;
    }
    return false;
  }

  @Override
  public void execute() {
    Market market = getAccounts().get(0).getHostingMarket();
    double trend = market.offers().getVolume().divide(market.bids().getVolume(), RoundingMode.UP).doubleValue();
    if (trend < 1) {
      // UP
      // <= 0.1 ->
      // <= 0.2 ->
      // <= 0.25 ->
      // <= 0.33 ->
      // <= 0.5 ->
      // > 0.5 abort

    } else {
      // DOWN
      // >= 10
      // >= 5
      // >= 3
      // >= 2
      // >= 1 abort
    }
    BigDecimal a = asset.get();
    BigDecimal c = cash.get();
//    getAccount().getHostingMarket().offers().getBest();
    Pair<BigDecimal, BigDecimal> bid = getAccounts().get(0).getHostingMarket().bids().getBest();
    BigDecimal estimate = bid.getKey().multiply(a).add(c);
  }
}
