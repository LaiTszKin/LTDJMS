package ltdjms.discord.panel.commands;

import ltdjms.discord.gametoken.domain.DiceGame1Config;
import ltdjms.discord.gametoken.domain.DiceGame2Config;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

final class AdminPanelModalFactory {

  private AdminPanelModalFactory() {}

  static Modal createBalanceAdjustModal(
      long userId, String mode, String userMention, String modeLabel) {
    String modalTitle = String.format("%s - %s", modeLabel, userMention);
    if (modalTitle.length() > 45) {
      modalTitle = modeLabel;
    }

    TextInput amountInput =
        TextInput.create("amount", "金額", TextInputStyle.SHORT)
            .setPlaceholder(mode.equals("adjust") ? "輸入目標餘額" : "輸入調整金額")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(15)
            .build();

    return Modal.create(
            AdminPanelButtonHandler.MODAL_BALANCE_ADJUST + ":" + userId + ":" + mode, modalTitle)
        .addComponents(ActionRow.of(amountInput))
        .build();
  }

  static Modal createTokenAdjustModal(
      long userId, String mode, String userMention, String modeLabel) {
    String modalTitle = String.format("%s - %s", modeLabel, userMention);
    if (modalTitle.length() > 45) {
      modalTitle = modeLabel;
    }

    TextInput amountInput =
        TextInput.create("amount", "數量", TextInputStyle.SHORT)
            .setPlaceholder(mode.equals("adjust") ? "輸入目標代幣數量" : "輸入調整數量")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(15)
            .build();

    return Modal.create(
            AdminPanelButtonHandler.MODAL_TOKEN_ADJUST + ":" + userId + ":" + mode, modalTitle)
        .addComponents(ActionRow.of(amountInput))
        .build();
  }

  static Modal createGame1TokensModal(DiceGame1Config config) {
    TextInput minInput =
        TextInput.create("min", "最小代幣數", TextInputStyle.SHORT)
            .setPlaceholder("最小可投入代幣數量")
            .setValue(String.valueOf(config.minTokensPerPlay()))
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(10)
            .build();

    TextInput maxInput =
        TextInput.create("max", "最大代幣數", TextInputStyle.SHORT)
            .setPlaceholder("最大可投入代幣數量")
            .setValue(String.valueOf(config.maxTokensPerPlay()))
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(10)
            .build();

    return Modal.create(AdminPanelButtonHandler.MODAL_GAME_1_TOKENS, "摘星手 - 代幣範圍")
        .addComponents(ActionRow.of(minInput), ActionRow.of(maxInput))
        .build();
  }

  static Modal createGame1RewardModal(DiceGame1Config config) {
    TextInput rewardInput =
        TextInput.create("reward", "單骰獎勵倍率", TextInputStyle.SHORT)
            .setPlaceholder("每點數的獎勵金額")
            .setValue(String.valueOf(config.rewardPerDiceValue()))
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(15)
            .build();

    return Modal.create(AdminPanelButtonHandler.MODAL_GAME_1_REWARD, "摘星手 - 獎勵設定")
        .addComponents(ActionRow.of(rewardInput))
        .build();
  }

  static Modal createGame2TokensModal(DiceGame2Config config) {
    TextInput minInput =
        TextInput.create("min", "最小代幣數", TextInputStyle.SHORT)
            .setPlaceholder("最小可投入代幣數量")
            .setValue(String.valueOf(config.minTokensPerPlay()))
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(10)
            .build();

    TextInput maxInput =
        TextInput.create("max", "最大代幣數", TextInputStyle.SHORT)
            .setPlaceholder("最大可投入代幣數量")
            .setValue(String.valueOf(config.maxTokensPerPlay()))
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(10)
            .build();

    return Modal.create(AdminPanelButtonHandler.MODAL_GAME_2_TOKENS, "神龍擺尾 - 代幣範圍")
        .addComponents(ActionRow.of(minInput), ActionRow.of(maxInput))
        .build();
  }

  static Modal createGame2MultipliersModal(DiceGame2Config config) {
    TextInput straightInput =
        TextInput.create("straight", "順子倍率", TextInputStyle.SHORT)
            .setPlaceholder("順子獎勵的基礎倍率")
            .setValue(String.valueOf(config.straightMultiplier()))
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(15)
            .build();

    TextInput baseInput =
        TextInput.create("base", "基礎倍率", TextInputStyle.SHORT)
            .setPlaceholder("非順子非豹子的基礎倍率")
            .setValue(String.valueOf(config.baseMultiplier()))
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(15)
            .build();

    return Modal.create(AdminPanelButtonHandler.MODAL_GAME_2_MULTIPLIERS, "神龍擺尾 - 獎勵倍率")
        .addComponents(ActionRow.of(straightInput), ActionRow.of(baseInput))
        .build();
  }

  static Modal createGame2BonusesModal(DiceGame2Config config) {
    TextInput lowInput =
        TextInput.create("low", "小豹子獎勵", TextInputStyle.SHORT)
            .setPlaceholder("小豹子（總和<10）的獎勵金額")
            .setValue(String.valueOf(config.tripleLowBonus()))
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(15)
            .build();

    TextInput highInput =
        TextInput.create("high", "大豹子獎勵", TextInputStyle.SHORT)
            .setPlaceholder("大豹子（總和≥10）的獎勵金額")
            .setValue(String.valueOf(config.tripleHighBonus()))
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(15)
            .build();

    return Modal.create(AdminPanelButtonHandler.MODAL_GAME_2_BONUSES, "神龍擺尾 - 豹子獎勵")
        .addComponents(ActionRow.of(lowInput), ActionRow.of(highInput))
        .build();
  }
}
