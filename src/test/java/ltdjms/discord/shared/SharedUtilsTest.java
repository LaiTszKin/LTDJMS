package ltdjms.discord.shared;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for shared package utility classes. */
@DisplayName("Shared Utils")
class SharedUtilsTest {

  @Nested
  @DisplayName("DotEnvLoader")
  class DotEnvLoaderTests {

    @Test
    @DisplayName("should return empty map when .env file does not exist")
    void shouldReturnEmptyMapWhenFileDoesNotExist(@TempDir Path tempDir) {
      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should load key-value pairs from .env file")
    void shouldLoadKeyValuePairs(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "KEY1=value1\nKEY2=value2");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result).hasSize(2);
      assertThat(result.get("KEY1")).isEqualTo("value1");
      assertThat(result.get("KEY2")).isEqualTo("value2");
    }

    @Test
    @DisplayName("should ignore comments starting with #")
    void shouldIgnoreComments(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "# This is a comment\nKEY=value");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result).hasSize(1);
      assertThat(result.get("KEY")).isEqualTo("value");
    }

    @Test
    @DisplayName("should ignore empty lines")
    void shouldIgnoreEmptyLines(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "\n\nKEY=value\n\n");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result).hasSize(1);
      assertThat(result.get("KEY")).isEqualTo("value");
    }

    @Test
    @DisplayName("should trim whitespace from keys and values")
    void shouldTrimWhitespace(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "  KEY  =  value  ");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result.get("KEY")).isEqualTo("value");
    }

    @Test
    @DisplayName("should strip double quotes from values")
    void shouldStripDoubleQuotes(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "KEY=\"quoted value\"");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result.get("KEY")).isEqualTo("quoted value");
    }

    @Test
    @DisplayName("should strip single quotes from values")
    void shouldStripSingleQuotes(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "KEY='quoted value'");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result.get("KEY")).isEqualTo("quoted value");
    }

    @Test
    @DisplayName("should handle values containing = characters")
    void shouldHandleValuesWithEquals(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "KEY=value=with=equals");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result.get("KEY")).isEqualTo("value=with=equals");
    }

    @Test
    @DisplayName("should skip invalid lines without equals sign")
    void shouldSkipInvalidLines(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "VALID=value\nINVALID LINE\nANOTHER=valid");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result).hasSize(2);
      assertThat(result.containsKey("VALID")).isTrue();
      assertThat(result.containsKey("ANOTHER")).isTrue();
    }

    @Test
    @DisplayName("should skip lines with empty key")
    void shouldSkipEmptyKey(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "=value\nKEY=value");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThat(result).hasSize(1);
      assertThat(result.get("KEY")).isEqualTo("value");
    }

    @Test
    @DisplayName("should return unmodifiable map")
    void shouldReturnUnmodifiableMap(@TempDir Path tempDir) throws IOException {
      Path envFile = tempDir.resolve(".env");
      Files.writeString(envFile, "KEY=value");

      DotEnvLoader loader = new DotEnvLoader(tempDir);
      Map<String, String> result = loader.load();

      assertThatThrownBy(() -> result.put("NEW", "value"))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should use current directory when no directory specified")
    void shouldUseCurrentDirectory() {
      DotEnvLoader loader = new DotEnvLoader();
      Map<String, String> result = loader.load();

      // Should not throw, just return empty if no .env file
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("DatabaseMigrationRunner")
  class DatabaseMigrationRunnerTests {

    @Test
    @DisplayName("should create runner with default location")
    void shouldCreateRunnerWithDefaultLocation() {
      DatabaseMigrationRunner runner = DatabaseMigrationRunner.forDefaultMigrations();

      assertThat(runner).isNotNull();
    }

    @Test
    @DisplayName("should create runner with custom location")
    void shouldCreateRunnerWithCustomLocation() {
      DatabaseMigrationRunner runner = new DatabaseMigrationRunner("classpath:custom/migration");

      assertThat(runner).isNotNull();
    }
  }

  @Nested
  @DisplayName("SchemaMigrationException")
  class SchemaMigrationExceptionTests {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateWithMessage() {
      SchemaMigrationException ex = new SchemaMigrationException("Test message");

      assertThat(ex.getMessage()).isEqualTo("Test message");
      assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateWithMessageAndCause() {
      Throwable cause = new RuntimeException("Root cause");
      SchemaMigrationException ex = new SchemaMigrationException("Test message", cause);

      assertThat(ex.getMessage()).isEqualTo("Test message");
      assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("should be a runtime exception")
    void shouldBeRuntimeException() {
      SchemaMigrationException ex = new SchemaMigrationException("Test");

      assertThat(ex).isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  @DisplayName("Unit")
  class UnitTests {

    @Test
    @DisplayName("should return the singleton instance")
    void shouldReturnSingleton() {
      Unit unit1 = Unit.INSTANCE;
      Unit unit2 = Unit.INSTANCE;

      assertThat(unit1).isSameAs(unit2);
    }

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToString() {
      Unit unit = Unit.INSTANCE;

      assertThat(unit.toString()).isEqualTo("Unit");
    }
  }
}
