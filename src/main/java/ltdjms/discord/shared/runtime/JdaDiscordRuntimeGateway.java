package ltdjms.discord.shared.runtime;

/**
 * Transitional alias for the canonical JDA-backed Discord runtime gateway.
 *
 * <p>New code should depend on {@link ltdjms.discord.discord.services.JdaDiscordRuntimeGateway}
 * directly.
 */
public final class JdaDiscordRuntimeGateway
    extends ltdjms.discord.discord.services.JdaDiscordRuntimeGateway
    implements DiscordRuntimeGateway {}
