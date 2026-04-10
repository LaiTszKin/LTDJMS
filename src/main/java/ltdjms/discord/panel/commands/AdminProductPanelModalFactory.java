package ltdjms.discord.panel.commands;

import ltdjms.discord.product.domain.Product;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

final class AdminProductPanelModalFactory {

  private AdminProductPanelModalFactory() {}

  static Modal createCreateProductModal() {
    TextInput nameInput =
        TextInput.create("name", "商品名稱", TextInputStyle.SHORT)
            .setPlaceholder("輸入商品名稱")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(100)
            .build();

    TextInput rewardTypeInput =
        TextInput.create("reward_type", "獎勵類型", TextInputStyle.SHORT)
            .setPlaceholder("CURRENCY 或 TOKEN（留空表示無自動獎勵）")
            .setRequired(false)
            .setMaxLength(20)
            .build();

    TextInput rewardAmountInput =
        TextInput.create("reward_amount", "獎勵數量", TextInputStyle.SHORT)
            .setPlaceholder("輸入獎勵數量（留空表示無自動獎勵）")
            .setRequired(false)
            .setMaxLength(15)
            .build();

    TextInput currencyPriceInput =
        TextInput.create("currency_price", "貨幣價格", TextInputStyle.SHORT)
            .setPlaceholder("輸入貨幣購買價格（留空表示不可用貨幣購買）")
            .setRequired(false)
            .setMaxLength(15)
            .build();

    TextInput fiatPriceInput =
        TextInput.create("fiat_price_twd", "實際價值（TWD）", TextInputStyle.SHORT)
            .setPlaceholder("輸入新台幣金額（留空表示非法幣商品）")
            .setRequired(false)
            .setMaxLength(15)
            .build();

    return Modal.create(AdminProductPanelHandler.MODAL_CREATE_PRODUCT, "建立商品")
        .addComponents(
            ActionRow.of(nameInput),
            ActionRow.of(rewardTypeInput),
            ActionRow.of(rewardAmountInput),
            ActionRow.of(currencyPriceInput),
            ActionRow.of(fiatPriceInput))
        .build();
  }

  static Modal createEditProductModal(long productId, Product product) {
    TextInput nameInput =
        TextInput.create("name", "商品名稱", TextInputStyle.SHORT)
            .setValue(product.name())
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(100)
            .build();

    String description = product.description();
    TextInput descInput =
        TextInput.create("description", "商品描述", TextInputStyle.PARAGRAPH)
            .setPlaceholder("輸入商品描述（選填）")
            .setRequired(false)
            .setMaxLength(500)
            .build();
    if (description != null && !description.isBlank()) {
      descInput =
          TextInput.create("description", "商品描述", TextInputStyle.PARAGRAPH)
              .setValue(description)
              .setRequired(false)
              .setMaxLength(500)
              .build();
    }

    String rewardTypeValue = product.rewardType() != null ? product.rewardType().name() : "";
    TextInput rewardTypeInput =
        TextInput.create("reward_type", "獎勵類型", TextInputStyle.SHORT)
            .setPlaceholder("CURRENCY 或 TOKEN（留空表示無自動獎勵）")
            .setRequired(false)
            .setMaxLength(20)
            .build();
    if (!rewardTypeValue.isBlank()) {
      rewardTypeInput =
          TextInput.create("reward_type", "獎勵類型", TextInputStyle.SHORT)
              .setPlaceholder("CURRENCY 或 TOKEN（留空表示無自動獎勵）")
              .setValue(rewardTypeValue)
              .setRequired(false)
              .setMaxLength(20)
              .build();
    }

    String rewardAmountValue =
        product.rewardAmount() != null ? String.valueOf(product.rewardAmount()) : "";
    TextInput rewardAmountInput =
        TextInput.create("reward_amount", "獎勵數量", TextInputStyle.SHORT)
            .setPlaceholder("輸入獎勵數量")
            .setRequired(false)
            .setMaxLength(15)
            .build();
    if (!rewardAmountValue.isBlank()) {
      rewardAmountInput =
          TextInput.create("reward_amount", "獎勵數量", TextInputStyle.SHORT)
              .setPlaceholder("輸入獎勵數量")
              .setValue(rewardAmountValue)
              .setRequired(false)
              .setMaxLength(15)
              .build();
    }

    String currencyPriceValue =
        product.currencyPrice() != null ? String.valueOf(product.currencyPrice()) : "";
    TextInput currencyPriceInput =
        TextInput.create("currency_price", "貨幣價格", TextInputStyle.SHORT)
            .setPlaceholder("輸入貨幣購買價格（留空表示不可用貨幣購買）")
            .setRequired(false)
            .setMaxLength(15)
            .build();
    if (!currencyPriceValue.isBlank()) {
      currencyPriceInput =
          TextInput.create("currency_price", "貨幣價格", TextInputStyle.SHORT)
              .setPlaceholder("輸入貨幣購買價格（留空表示不可用貨幣購買）")
              .setValue(currencyPriceValue)
              .setRequired(false)
              .setMaxLength(15)
              .build();
    }

    return Modal.create(AdminProductPanelHandler.MODAL_EDIT_PRODUCT + productId, "編輯商品")
        .addComponents(
            ActionRow.of(nameInput),
            ActionRow.of(descInput),
            ActionRow.of(rewardTypeInput),
            ActionRow.of(rewardAmountInput),
            ActionRow.of(currencyPriceInput))
        .build();
  }

  static Modal createSetFiatValueModal(long productId, Product product) {
    String currentValue =
        product.fiatPriceTwd() != null ? String.valueOf(product.fiatPriceTwd()) : "";
    TextInput.Builder builder =
        TextInput.create("fiat_price_twd", "實際價值（TWD）", TextInputStyle.SHORT)
            .setPlaceholder("輸入新台幣金額（留空可清除）")
            .setRequired(false)
            .setMaxLength(15);
    if (!currentValue.isBlank()) {
      builder.setValue(currentValue);
    }
    return Modal.create(AdminProductPanelHandler.MODAL_SET_FIAT_VALUE + productId, "設定實際價值（TWD）")
        .addComponents(ActionRow.of(builder.build()))
        .build();
  }

  static Modal createGenerateCodesModal(long productId) {
    TextInput countInput =
        TextInput.create("count", "生成數量", TextInputStyle.SHORT)
            .setPlaceholder("輸入要生成的兌換碼數量（1-100）")
            .setValue("10")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(3)
            .build();

    TextInput quantityInput =
        TextInput.create("quantity", "每個碼可兌換數量", TextInputStyle.SHORT)
            .setPlaceholder("每個兌換碼可兌換的商品數量（1-1000，預設為 1）")
            .setValue("1")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(4)
            .build();

    TextInput expiresInput =
        TextInput.create("expires", "到期日期", TextInputStyle.SHORT)
            .setPlaceholder("格式：YYYY-MM-DD（留空表示永不過期）")
            .setRequired(false)
            .setMaxLength(10)
            .build();

    return Modal.create(AdminProductPanelHandler.MODAL_GENERATE_CODES + productId, "生成兌換碼")
        .addComponents(
            ActionRow.of(countInput), ActionRow.of(quantityInput), ActionRow.of(expiresInput))
        .build();
  }
}
