package ltdjms.discord.shared.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NoDirectJdaProviderCallsites")
class NoDirectJdaProviderCallsitesTest {

  @Test
  @DisplayName("owned modules should not call JDAProvider.getJda directly")
  void shouldNotCallJdaProviderGetJdaDirectly() throws IOException {
    List<Path> roots =
        List.of(
            Path.of("src/main/java/ltdjms/discord/aiagent"),
            Path.of("src/main/java/ltdjms/discord/aichat"),
            Path.of("src/main/java/ltdjms/discord/shop"),
            Path.of("src/test/java/ltdjms/discord/aiagent"),
            Path.of("src/test/java/ltdjms/discord/aichat"),
            Path.of("src/test/java/ltdjms/discord/shop"));

    List<String> offenders = new ArrayList<>();
    for (Path root : roots) {
      if (!Files.exists(root)) {
        continue;
      }
      try (Stream<Path> stream = Files.walk(root)) {
        stream
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".java"))
            .filter(this::containsForbiddenCallSite)
            .forEach(path -> offenders.add(root.relativize(path).toString()));
      }
    }

    assertThat(offenders).as("forbidden JDAProvider call sites").isEmpty();
  }

  private boolean containsForbiddenCallSite(Path path) {
    try {
      return Files.readString(path).contains("JDAProvider.getJda(");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + path, e);
    }
  }
}
