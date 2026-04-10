package ltdjms.discord.product.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a product that can be redeemed with redemption codes within a specific Discord guild.
 * Products can optionally provide rewards when redeemed or after a paid fiat order is processed.
 */
public record Product(
    Long id,
    long guildId,
    String name,
    String description,
    RewardType rewardType,
    Long rewardAmount,
    Long currencyPrice,
    Long fiatPriceTwd,
    boolean autoCreateEscortOrder,
    String escortOptionCode,
    Instant createdAt,
    Instant updatedAt) {
  /** Type of reward that can be given when a product is redeemed. */
  public enum RewardType {
    CURRENCY,
    TOKEN
  }

  public Product {
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (name.length() > 100) {
      throw new IllegalArgumentException("name must not exceed 100 characters");
    }
    if (description != null && description.length() > 1000) {
      throw new IllegalArgumentException("description must not exceed 1000 characters");
    }
    if (rewardAmount != null && rewardAmount < 0) {
      throw new IllegalArgumentException("rewardAmount must not be negative");
    }
    if (currencyPrice != null && currencyPrice < 0) {
      throw new IllegalArgumentException("currencyPrice must not be negative");
    }
    if (fiatPriceTwd != null && fiatPriceTwd < 0) {
      throw new IllegalArgumentException("fiatPriceTwd must not be negative");
    }
    if (escortOptionCode != null && escortOptionCode.length() > 120) {
      throw new IllegalArgumentException("escortOptionCode must not exceed 120 characters");
    }
    if (autoCreateEscortOrder) {
      if (escortOptionCode == null || escortOptionCode.isBlank()) {
        throw new IllegalArgumentException(
            "escortOptionCode is required when autoCreateEscortOrder is enabled");
      }
    } else if (escortOptionCode != null && !escortOptionCode.isBlank()) {
      throw new IllegalArgumentException(
          "escortOptionCode requires autoCreateEscortOrder to be enabled");
    }
    if ((rewardType == null) != (rewardAmount == null)) {
      throw new IllegalArgumentException(
          "rewardType and rewardAmount must both be specified or both be null");
    }
  }

  public Product(
      Long id,
      long guildId,
      String name,
      String description,
      RewardType rewardType,
      Long rewardAmount,
      Long currencyPrice,
      Long fiatPriceTwd,
      Instant createdAt,
      Instant updatedAt) {
    this(
        id,
        guildId,
        name,
        description,
        rewardType,
        rewardAmount,
        currencyPrice,
        fiatPriceTwd,
        false,
        null,
        createdAt,
        updatedAt);
  }

  public Product(
      Long id,
      long guildId,
      String name,
      String description,
      RewardType rewardType,
      Long rewardAmount,
      Long currencyPrice,
      Instant createdAt,
      Instant updatedAt) {
    this(
        id,
        guildId,
        name,
        description,
        rewardType,
        rewardAmount,
        currencyPrice,
        null,
        false,
        null,
        createdAt,
        updatedAt);
  }

  public static Product create(
      long guildId,
      String name,
      String description,
      RewardType rewardType,
      Long rewardAmount,
      Long currencyPrice,
      Long fiatPriceTwd,
      boolean autoCreateEscortOrder,
      String escortOptionCode) {
    Instant now = Instant.now();
    return new Product(
        null,
        guildId,
        name,
        description,
        rewardType,
        rewardAmount,
        currencyPrice,
        fiatPriceTwd,
        autoCreateEscortOrder,
        escortOptionCode,
        now,
        now);
  }

  public static Product create(
      long guildId,
      String name,
      String description,
      RewardType rewardType,
      Long rewardAmount,
      Long currencyPrice,
      Long fiatPriceTwd) {
    return create(
        guildId,
        name,
        description,
        rewardType,
        rewardAmount,
        currencyPrice,
        fiatPriceTwd,
        false,
        null);
  }

  public static Product create(
      long guildId,
      String name,
      String description,
      RewardType rewardType,
      Long rewardAmount,
      Long currencyPrice) {
    return create(guildId, name, description, rewardType, rewardAmount, currencyPrice, null);
  }

  public static Product createWithoutReward(long guildId, String name, String description) {
    return create(guildId, name, description, null, null, null, null);
  }

  public static Product createWithCurrencyPrice(
      long guildId, String name, String description, long currencyPrice) {
    return create(guildId, name, description, null, null, currencyPrice, null);
  }

  public static Product createWithFiatPriceTwd(
      long guildId, String name, String description, long fiatPriceTwd) {
    return create(guildId, name, description, null, null, null, fiatPriceTwd);
  }

  public Product withUpdatedDetails(
      String name,
      String description,
      RewardType rewardType,
      Long rewardAmount,
      Long currencyPrice,
      Long fiatPriceTwd) {
    return withUpdatedDetails(
        name,
        description,
        rewardType,
        rewardAmount,
        currencyPrice,
        fiatPriceTwd,
        this.autoCreateEscortOrder,
        this.escortOptionCode);
  }

  public Product withUpdatedDetails(
      String name,
      String description,
      RewardType rewardType,
      Long rewardAmount,
      Long currencyPrice,
      Long fiatPriceTwd,
      boolean autoCreateEscortOrder,
      String escortOptionCode) {
    return new Product(
        this.id,
        this.guildId,
        name,
        description,
        rewardType,
        rewardAmount,
        currencyPrice,
        fiatPriceTwd,
        autoCreateEscortOrder,
        escortOptionCode,
        this.createdAt,
        Instant.now());
  }

  public Product withUpdatedDetails(
      String name,
      String description,
      RewardType rewardType,
      Long rewardAmount,
      Long currencyPrice) {
    return withUpdatedDetails(
        name, description, rewardType, rewardAmount, currencyPrice, this.fiatPriceTwd);
  }

  public boolean hasReward() {
    return rewardType != null && rewardAmount != null;
  }

  public String formatReward() {
    if (!hasReward()) {
      return null;
    }
    return switch (rewardType) {
      case CURRENCY -> String.format("%,d 貨幣", rewardAmount);
      case TOKEN -> String.format("%,d 代幣", rewardAmount);
    };
  }

  public boolean hasCurrencyPrice() {
    return currencyPrice != null && currencyPrice > 0;
  }

  public String formatCurrencyPrice() {
    if (!hasCurrencyPrice()) {
      return null;
    }
    return String.format("%,d 貨幣", currencyPrice);
  }

  public boolean hasFiatPriceTwd() {
    return fiatPriceTwd != null && fiatPriceTwd > 0;
  }

  public String formatFiatPriceTwd() {
    if (!hasFiatPriceTwd()) {
      return null;
    }
    return String.format("NT$%,d", fiatPriceTwd);
  }

  public boolean isFiatOnly() {
    return hasFiatPriceTwd() && !hasCurrencyPrice();
  }

  public boolean shouldAutoCreateEscortOrder() {
    return autoCreateEscortOrder && escortOptionCode != null && !escortOptionCode.isBlank();
  }
}
