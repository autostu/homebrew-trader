package xyz.homebrew.core;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Orderbook {

  private final Map<BigDecimal, BigDecimal> orders;

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  public Orderbook(Comparator<BigDecimal> cmp) {
    orders = new TreeMap<>(cmp);
  }

  public Pair<BigDecimal, BigDecimal> getBest() {
    try {
      lock.readLock().lock();
      Iterator<Entry<BigDecimal, BigDecimal>> iterator = orders.entrySet().iterator();
      if (iterator.hasNext()) {
        Entry<BigDecimal, BigDecimal> best = iterator.next();
        return Pair.of(best.getKey(), best.getValue());
      }
      return null;
    } finally {
      lock.readLock().unlock();
    }
  }

  public BigDecimal getAvgPrice() {
    try {
      lock.readLock().lock();
      return getVolume().divide(getTotalAmount(), RoundingMode.UP);
    } finally {
      lock.readLock().unlock();
    }
  }

  public BigDecimal getTotalAmount() {
    try {
      lock.readLock().lock();
      return orders.values().stream()
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    } finally {
      lock.readLock().unlock();
    }
  }

  public BigDecimal getVolume() {
    try {
      lock.readLock().lock();
      return orders.entrySet().stream()
          .map(kv -> kv.getKey().multiply(kv.getValue()))
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    } finally {
      lock.readLock().unlock();
    }
  }

  public void update(List<Pair<BigDecimal, BigDecimal>> depth) {
    try {
      lock.writeLock().lock();
      if (!depth.isEmpty()) {
        orders.clear();
        for (Pair<BigDecimal, BigDecimal> level : depth) {
          orders.put(level.getKey(), level.getValue());
        }
      }
    } finally {
      lock.writeLock().unlock();
    }
  }
}
