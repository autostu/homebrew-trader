package xyz.homebrew.app.strategy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import xyz.homebrew.core.AbstractTrader;
import xyz.homebrew.core.Market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class AlgowTrader extends AbstractTrader {

  static final int CAPACITY = 15;

  final AtomicReference<BigDecimal> asset = new AtomicReference<>();

  final AtomicReference<BigDecimal> cash = new AtomicReference<>();

  final ArrayList<Pair<BigDecimal, BigDecimal>> buf = new ArrayList<>(CAPACITY + 1);

  @Override
  public void init() {
    syncAssets();
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        syncAssets();
      }
    }, 5_000, 5_000);
  }

  private void syncAssets() {
    asset.set(null);
    cash.set(null);
    getAccount("fcoin_main").getBalance().handle((p, e) -> {
      if (e != null) {
        syncAssets();
      } else {
        asset.set(p.getTradableBase());
        cash.set(p.getTradableQuote());
      }
      return null;
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
//      calculate();
//      System.out.println("offers difference: " + evaluation[0]);
//      System.out.println("offers vibrate: " + evaluation[1]);
//      System.out.println("bids difference: " + evaluation[2]);
//      System.out.println("bids vibrate: " + evaluation[3]);
      System.out.println("offers volume: " + getAccount("fcoin_main").getHostingMarket().offers().getVolume());
      System.out.println("bids volume: " + getAccount("fcoin_main").getHostingMarket().bids().getVolume());
      System.out.println("offers amount: " + getAccount("fcoin_main").getHostingMarket().offers().getTotalAmount());
      System.out.println("bids amount: " + getAccount("fcoin_main").getHostingMarket().bids().getTotalAmount());
      System.out.println("offers avg: " + getAccount("fcoin_main").getHostingMarket().offers().getAvgPrice());
      System.out.println("bids avg: " + getAccount("fcoin_main").getHostingMarket().bids().getAvgPrice());
      System.out.println("best offer: " + getAccount("fcoin_main").getHostingMarket().offers().getBest());
      System.out.println("best bid: " + getAccount("fcoin_main").getHostingMarket().bids().getBest());
      System.out.println();
      return true;
    }
    return false;
  }

  @Override
  public void execute() {
    Market market = getAccount("fcoin_main").getHostingMarket();
    double trend = market.offers().getVolume().divide(market.bids().getVolume(), RoundingMode.UP).doubleValue();
    BigDecimal bestOfferPrice = market.offers().getBest().getKey();
    BigDecimal bestBidPrice = market.bids().getBest().getKey();
    if (trend <= 0.1) {
      // terminate sell all
      getAccount("fcoin_main").sell(market.offers().getAvgPrice().multiply(BigDecimal.valueOf(1.05)), asset.get())
          .handle((s, e) -> {
            syncAssets();
            return null;
          });
      asset.set(null);
      cash.set(null);
      log.info("greedy: sell all!");
    } else if (trend <= 0.5) {
      // follow buy to 100%
      // TODO recognize trap !!!
      if (bestOfferPrice.subtract(bestBidPrice).doubleValue() > 50.0 ||
          market.offers().getTotalAmount().doubleValue() <= 1) {
        return;
      }
      getAccount("fcoin_main").buy(bestOfferPrice, cash.get().divide(bestOfferPrice, RoundingMode.DOWN))
          .handle((s, e) -> {
            syncAssets();
            return null;
          });
      log.info("greedy: buy all {} usdt with price {}", cash.get(), bestOfferPrice);
      asset.set(null);
      cash.set(null);
    } else if (trend <= 0.95) {
      // buy 50%
      // TODO recognize trap !!!
      if (bestOfferPrice.subtract(bestBidPrice).doubleValue() > 50.0 ||
          market.offers().getVolume().doubleValue() <= 1000) {
        return;
      }
      if (cash.get().doubleValue() > 30) {
        BigDecimal paid = cash.get().multiply(BigDecimal.valueOf(0.5));
        BigDecimal buy = paid.divide(bestOfferPrice, RoundingMode.DOWN);
        getAccount("fcoin_main").buy(bestOfferPrice, buy)
            .handle((s, e) -> {
              syncAssets();
              return null;
            });
        cash.set(cash.get().subtract(paid));
        log.info("buy 50% = {} usdt with price {}", paid, bestOfferPrice);
      }
    } else if (trend <= 2) {
      // follow sell to 50%
      // TODO recognize trap !!!
      if (bestOfferPrice.subtract(bestBidPrice).doubleValue() > 50.0 ||
          market.bids().getVolume().doubleValue() <= 1000) {
        return;
      }
      BigDecimal amount = asset.get();
      BigDecimal profit = amount.multiply(bestBidPrice).subtract(cash.get());
      if (profit.doubleValue() > 15) {
        BigDecimal sell = profit.divide(bestBidPrice, RoundingMode.DOWN);
        getAccount("fcoin_main").sell(bestBidPrice, sell).handle((s, e) -> {
          syncAssets();
          return null;
        });
        asset.set(amount.subtract(sell));
        log.info("sell to 50% = {} btc with price {}", sell, bestBidPrice);
      }
    } else {
      // follow sell all
      getAccount("fcoin_main").sell(bestOfferPrice.subtract(BigDecimal.valueOf(0.1)), asset.get())
          .handle((s, e) -> {
            syncAssets();
            return null;
          });
      asset.set(null);
      cash.set(null);
      log.info("falls: sell all!");
    }
  }
}
