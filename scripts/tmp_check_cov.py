import csv
import fnmatch
from pathlib import Path


def main() -> None:
    csv_path = Path("target/site/jacoco/jacoco.csv")
    with csv_path.open() as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    # Exclude patterns copied from jacoco-maven-plugin check configuration
    patterns = [
        "**/DiscordCurrencyBot.class",
        "**/BotErrorHandler.class",
        "**/SlashCommandListener.class",
        "**/SlashCommandListener$*.class",
        "**/GatewayIntentsConfiguration.class",
        "**/*CommandHandler.class",
        "**/UserPanelButtonHandler.class",
        "**/AdminPanelButtonHandler.class",
        "**/EmojiValidator.class",
        "**/DiscordEmojiValidator.class",
        "**/JdaEmojiValidator.class",
        "**/DatabaseConfig.class",
        "**/RepositoryException.class",
        "**/NegativeBalanceException.class",
        "**/Jdbc*Repository.class",
        "**/Jooq*Repository.class",
        "**/AppComponent.class",
        "**/AppComponentFactory.class",
        "**/DaggerAppComponent*.class",
        "**/*Module.class",
    ]

    def is_excluded(pkg: str, cls: str) -> bool:
        path = f"{pkg.replace('.', '/')}/{cls}.class"
        return any(fnmatch.fnmatch(path, p) for p in patterns)

    inc = {"missed": 0, "covered": 0}
    inc_all = {"missed": 0, "covered": 0}
    included_rows = []

    for r in rows:
        missed = int(r["LINE_MISSED"])
        covered = int(r["LINE_COVERED"])
        inc_all["missed"] += missed
        inc_all["covered"] += covered

        if not is_excluded(r["PACKAGE"], r["CLASS"]):
            inc["missed"] += missed
            inc["covered"] += covered
            included_rows.append((r["PACKAGE"], r["CLASS"], missed, covered))

    ratio_all = inc_all["covered"] / float(inc_all["missed"] + inc_all["covered"])
    ratio = inc["covered"] / float(inc["missed"] + inc["covered"])

    print("All lines coverage:", ratio_all)
    print("After excludes coverage:", ratio)
    print("Totals (included): missed=", inc["missed"], "covered=", inc["covered"])
    print("Totals (all): missed=", inc_all["missed"], "covered=", inc_all["covered"])
    print("\nIncluded classes sorted by missed lines:")
    for pkg, cls, missed, covered in sorted(
        included_rows, key=lambda x: x[2], reverse=True
    ):
        total = missed + covered
        ratio_cls = covered / total if total else 1.0
        print(
            f"{pkg}.{cls}: missed={missed}, covered={covered}, "
            f"coverage={ratio_cls:.1%}, total_lines={total}"
        )


if __name__ == "__main__":
    main()
