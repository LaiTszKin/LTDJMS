package ltdjms.discord.aiagent.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ltdjms.discord.aiagent.domain.PermissionSetting;
import ltdjms.discord.aiagent.domain.PermissionSetting.PermissionEnum;

@DisplayName("T0xx: PermissionSetting JSON 反序列化測試")
class PermissionSettingJsonTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("應接受舊版 permissionSet 並映射為 allowSet")
  void shouldDeserializeLegacyPermissionSet() throws Exception {
    String json =
        """
        [
          {"roleId": 123, "permissionSet": "admin_only"}
        ]
        """;

    List<PermissionSetting> result =
        mapper.readValue(json, new TypeReference<List<PermissionSetting>>() {});

    assertThat(result).hasSize(1);
    PermissionSetting setting = result.get(0);

    assertThat(setting.roleId()).isEqualTo(123);
    assertThat(setting.allowSet())
        .containsExactlyInAnyOrder(
            PermissionEnum.ADMINISTRATOR, PermissionEnum.VIEW_CHANNEL, PermissionEnum.MESSAGE_SEND);
    assertThat(setting.denySet()).isEmpty();
  }

  @Test
  @DisplayName("若 allowSet 已提供則不被舊欄位覆蓋")
  void shouldPreferAllowSetWhenProvided() throws Exception {
    String json =
        """
        [
          {
            "roleId": 456,
            "allowSet": ["VIEW_CHANNEL"],
            "permissionSet": "private"
          }
        ]
        """;

    List<PermissionSetting> result =
        mapper.readValue(json, new TypeReference<List<PermissionSetting>>() {});

    PermissionSetting setting = result.get(0);

    assertThat(setting.roleId()).isEqualTo(456);
    assertThat(setting.allowSet()).containsExactly(PermissionEnum.VIEW_CHANNEL);
    assertThat(setting.denySet()).isEmpty();
  }
}
