package ltdjms.discord.panel.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.product.domain.Product;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

@DisplayName("AdminProductPanelViewFactory 測試")
class AdminProductPanelViewFactoryTest {

  private static final long TEST_GUILD_ID = 123L;

  @Test
  @DisplayName("空商品列表不應包含選單")
  void emptyProductListShouldNotIncludeMenu() {
    var rows = AdminProductPanelViewFactory.buildProductListComponents(List.of());

    assertThat(rows).hasSize(1); // only button row
    assertThat(rows.get(0).getButtons()).isNotEmpty();
  }

  @Test
  @DisplayName("25 個商品以內應只有一個選單列")
  void upTo25ProductsShouldHaveOneMenuRow() {
    var products = createProducts(20);
    var rows = AdminProductPanelViewFactory.buildProductListComponents(products);

    assertThat(rows).hasSize(2); // 1 select + 1 button
    var select = findSelectMenu(rows);
    assertThat(select.getOptions()).hasSize(20);
  }

  @Test
  @DisplayName("超過 25 個商品時應自動拆分為多個選單列")
  void moreThan25ProductsShouldSplitIntoMultipleMenuRows() {
    var products = createProducts(60);

    var rows = AdminProductPanelViewFactory.buildProductListComponents(products);

    // 3 select rows + 1 button row
    assertThat(rows).hasSize(4);
    assertThat(findSelectMenu(rows.subList(0, 1)).getOptions()).hasSize(25);
    assertThat(findSelectMenu(rows.subList(1, 2)).getOptions()).hasSize(25);
    assertThat(findSelectMenu(rows.subList(2, 3)).getOptions()).hasSize(10);
  }

  @Test
  @DisplayName("拆分後的選單應使用相同的 SELECT_PRODUCT ID")
  void splitMenusShouldShareSameSelectId() {
    var products = createProducts(30);
    var rows = AdminProductPanelViewFactory.buildProductListComponents(products);

    for (ActionRow row : rows.subList(0, rows.size() - 1)) {
      var menu = findSelectMenu(List.of(row));
      assertThat(menu.getId()).isEqualTo(AdminProductPanelHandler.SELECT_PRODUCT);
    }
  }

  private static List<Product> createProducts(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> Product.createWithCurrencyPrice(TEST_GUILD_ID, "商品" + i, null, 100L + i))
        .toList();
  }

  private static StringSelectMenu findSelectMenu(List<ActionRow> rows) {
    return rows.stream()
        .flatMap(row -> row.getComponents().stream())
        .filter(comp -> comp instanceof StringSelectMenu)
        .map(comp -> (StringSelectMenu) comp)
        .findFirst()
        .orElseThrow();
  }
}
