package xyz.homebrew.core;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public final class Balance {

  BigDecimal quote;

  BigDecimal base;

  BigDecimal frozenBase;

  BigDecimal frozenQuote;

  BigDecimal tradableBase;

  BigDecimal tradableQuote;
}
