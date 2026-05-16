package ltdjms.discord.discord.services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

/** 當 StringSelectMenu 選項可能超過 25 個時，自動拆分為多個選單。 */
public final class SelectMenuUtil {

  private static final int MAX_OPTIONS_PER_MENU = 25;

  private SelectMenuUtil() {}

  /**
   * 將選項列表拆分為多個 {@link StringSelectMenu}，每個選單不超過 25 個選項。所有選單共用同一個 selectId。
   *
   * @param selectId 選單的 custom ID（所有拆分選單共用）
   * @param placeholder 選單的 placeholder 文字
   * @param items 選項列表
   * @param optionBuilder 將每個 item 加入 StringSelectMenu.Builder 的回呼
   * @return 拆分後的選單列表（若 items 為空則回傳空列表）
   */
  public static <T> List<StringSelectMenu> splitSelectMenu(
      String selectId,
      String placeholder,
      List<T> items,
      BiConsumer<StringSelectMenu.Builder, T> optionBuilder) {
    return splitSelectMenu(selectId, placeholder, items, optionBuilder, MAX_OPTIONS_PER_MENU);
  }

  /**
   * 將選項列表拆分為多個 {@link StringSelectMenu}，每個選單不超過指定數量的選項。
   *
   * @param selectId 選單的 custom ID（所有拆分選單共用）
   * @param placeholder 選單的 placeholder 文字
   * @param items 選項列表
   * @param optionBuilder 將每個 item 加入 StringSelectMenu.Builder 的回呼
   * @param maxOptionsPerMenu 每個選單的最大選項數
   * @return 拆分後的選單列表（若 items 為空則回傳空列表）
   */
  public static <T> List<StringSelectMenu> splitSelectMenu(
      String selectId,
      String placeholder,
      List<T> items,
      BiConsumer<StringSelectMenu.Builder, T> optionBuilder,
      int maxOptionsPerMenu) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }

    List<StringSelectMenu> menus = new ArrayList<>();
    for (int i = 0; i < items.size(); i += maxOptionsPerMenu) {
      int end = Math.min(i + maxOptionsPerMenu, items.size());
      List<T> chunk = items.subList(i, end);

      StringSelectMenu.Builder builder =
          StringSelectMenu.create(selectId).setPlaceholder(placeholder);
      for (T item : chunk) {
        optionBuilder.accept(builder, item);
      }
      menus.add(builder.build());
    }
    return menus;
  }

  /**
   * 將選項列表拆分為多個 {@link ActionRow}，每列一個 {@link StringSelectMenu}。
   *
   * @see #splitSelectMenu(String, String, List, BiConsumer)
   */
  public static <T> List<ActionRow> buildSelectRows(
      String selectId,
      String placeholder,
      List<T> items,
      BiConsumer<StringSelectMenu.Builder, T> optionBuilder) {
    return splitSelectMenu(selectId, placeholder, items, optionBuilder).stream()
        .map(ActionRow::of)
        .toList();
  }
}
