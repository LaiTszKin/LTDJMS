package ltdjms.discord.dispatch.services;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.dispatch.domain.DispatchAfterSalesStaffRepository;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shared.Unit;

/** 售後人員設定服務。 */
public class DispatchAfterSalesStaffService {

  private static final Logger LOG = LoggerFactory.getLogger(DispatchAfterSalesStaffService.class);

  private final DispatchAfterSalesStaffRepository repository;

  public DispatchAfterSalesStaffService(DispatchAfterSalesStaffRepository repository) {
    this.repository = repository;
  }

  public Result<Set<Long>, DomainError> getStaffUserIds(long guildId) {
    try {
      return Result.ok(repository.findStaffUserIds(guildId));
    } catch (Exception e) {
      LOG.error("Failed to query dispatch after-sales staff: guildId={}", guildId, e);
      return Result.err(DomainError.persistenceFailure("查詢售後人員失敗", e));
    }
  }

  public Result<Unit, DomainError> addStaff(long guildId, long userId) {
    try {
      boolean inserted = repository.addStaff(guildId, userId);
      if (!inserted) {
        return Result.err(DomainError.invalidInput("該成員已在售後名單中"));
      }
      return Result.okVoid();
    } catch (Exception e) {
      LOG.error(
          "Failed to add dispatch after-sales staff: guildId={}, userId={}", guildId, userId, e);
      return Result.err(DomainError.persistenceFailure("新增售後人員失敗", e));
    }
  }

  public Result<Unit, DomainError> removeStaff(long guildId, long userId) {
    try {
      boolean removed = repository.removeStaff(guildId, userId);
      if (!removed) {
        return Result.err(DomainError.invalidInput("該成員不在售後名單中"));
      }
      return Result.okVoid();
    } catch (Exception e) {
      LOG.error(
          "Failed to remove dispatch after-sales staff: guildId={}, userId={}", guildId, userId, e);
      return Result.err(DomainError.persistenceFailure("移除售後人員失敗", e));
    }
  }

  public boolean isAfterSalesStaff(long guildId, long userId) {
    try {
      return repository.findStaffUserIds(guildId).contains(userId);
    } catch (Exception e) {
      LOG.warn(
          "Failed to verify dispatch after-sales staff: guildId={}, userId={}", guildId, userId, e);
      return false;
    }
  }
}
