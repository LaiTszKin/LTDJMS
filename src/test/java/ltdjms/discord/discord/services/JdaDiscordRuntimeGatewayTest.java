package ltdjms.discord.discord.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.discord.domain.DiscordRuntimeNotReadyException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

@DisplayName("JdaDiscordRuntimeGateway")
class JdaDiscordRuntimeGatewayTest {

  @Test
  @DisplayName("未發布時應回傳明確 not-ready 例外")
  void shouldThrowWhenRuntimeIsNotReady() {
    JdaDiscordRuntimeGateway gateway = new JdaDiscordRuntimeGateway();

    assertThat(gateway.isReady()).isFalse();
    assertThatThrownBy(gateway::requireReadyJda)
        .isInstanceOf(DiscordRuntimeNotReadyException.class)
        .hasMessageContaining("not ready");
    assertThatThrownBy(() -> gateway.findGuild(1L))
        .isInstanceOf(DiscordRuntimeNotReadyException.class);
  }

  @Test
  @DisplayName("發布後應可解析 guild / channel / bot identity")
  void shouldExposePublishedRuntimeCapabilities() {
    JDA jda = mock(JDA.class);
    Guild guild = mock(Guild.class);
    GuildChannel guildChannel = mock(GuildChannel.class);
    ThreadChannel threadChannel = mock(ThreadChannel.class);
    SelfUser selfUser = mock(SelfUser.class);

    when(jda.getGuildById(123L)).thenReturn(guild);
    when(guild.getGuildChannelById(456L)).thenReturn(guildChannel);
    when(guild.getThreadChannelById(789L)).thenReturn(threadChannel);
    when(jda.getSelfUser()).thenReturn(selfUser);
    when(selfUser.getIdLong()).thenReturn(999L);

    JdaDiscordRuntimeGateway gateway = new JdaDiscordRuntimeGateway();
    gateway.publishReady(jda);

    assertThat(gateway.isReady()).isTrue();
    assertThat(gateway.requireReadyJda()).isSameAs(jda);
    assertThat(gateway.findGuild(123L)).contains(guild);
    assertThat(gateway.findGuildChannel(123L, 456L)).contains(guildChannel);
    assertThat(gateway.findThreadChannel(123L, 789L)).contains(threadChannel);
    assertThat(gateway.selfUserId()).isEqualTo(999L);
  }

  @Test
  @DisplayName("重複發布應被拒絕")
  void shouldRejectDuplicatePublish() {
    JDA first = mock(JDA.class);
    JDA second = mock(JDA.class);
    JdaDiscordRuntimeGateway gateway = new JdaDiscordRuntimeGateway();

    gateway.publishReady(first);

    assertThatThrownBy(() -> gateway.publishReady(second))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already been published");
  }
}
