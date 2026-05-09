package ltdjms.discord.product.domain;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for the escort option catalog.
 *
 * <p>Provides CRUD operations for the {@code escort_option_catalog} table. This is a guild-agnostic
 * global catalog — all guilds share the same set of escort options. Guild-specific price overrides
 * are handled by {@code EscortOptionPriceRepository}.
 */
public interface EscortOptionCatalogRepository {

  /** Returns all catalog entries ordered by id. */
  List<EscortOptionCatalog> findAll();

  /** Finds a catalog entry by its unique code. */
  Optional<EscortOptionCatalog> findByCode(String code);

  /**
   * Saves a new catalog entry. The returned {@code EscortOptionCatalog} will have the generated id
   * and timestamps populated.
   */
  EscortOptionCatalog save(EscortOptionCatalog catalog);

  /**
   * Updates an existing catalog entry identified by its code. Returns the updated entry with
   * refreshed timestamps.
   */
  EscortOptionCatalog update(EscortOptionCatalog catalog);

  /** Deletes a catalog entry by its code. Returns true if a row was deleted. */
  boolean deleteByCode(String code);

  /** Returns true if a catalog entry with the given code exists. */
  boolean existsByCode(String code);

  /** Returns the total number of catalog entries. */
  long count();
}
