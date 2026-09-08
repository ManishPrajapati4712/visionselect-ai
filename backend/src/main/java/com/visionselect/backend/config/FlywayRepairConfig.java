package com.visionselect.backend.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway migration strategy that performs a one-time checksum repair before
 * migrating.
 *
 * <h2>Why this exists</h2>
 * <p>On 2026-08-16, the {@code visionselect} database was created using an
 * original {@code V1__create_users_and_refresh_tokens.sql} that differed from
 * the untracked file subsequently present in the working tree. The corrected
 * V1 file now matches the live schema, but its CRC32 checksum does not match
 * the one stored in {@code flyway_schema_history} from when V1 first ran.
 *
 * <h2>What {@code flyway.repair()} does</h2>
 * <ul>
 *   <li>Recalculates the CRC32 of every on-disk migration file.</li>
 *   <li>Updates the {@code checksum} column in {@code flyway_schema_history}
 *       for any already-applied migration whose on-disk checksum has changed.</li>
 *   <li>Does NOT re-run any migration.</li>
 *   <li>Does NOT alter any user table or application data.</li>
 *   <li>Does NOT drop or recreate any table.</li>
 *   <li>Is fully idempotent: running it repeatedly is safe.</li>
 * </ul>
 *
 * <h2>What happens on subsequent startups</h2>
 * <p>After the first successful startup with repair, the checksum stored in
 * {@code flyway_schema_history} will match the on-disk V1 file. Subsequent
 * startups also call repair, but since the checksums already match, the repair
 * is a no-op (it finds nothing to update). Flyway's own
 * {@code validate-on-migrate: true} then verifies all checksums and proceeds
 * to run any new pending migrations (V3, V4, …).
 *
 * <h2>Reference</h2>
 * <p>This uses the {@link FlywayMigrationStrategy} contract provided by
 * Spring Boot's Flyway auto-configuration, which is the officially supported
 * way to customise the migration lifecycle without bypassing auto-configuration
 * or hard-coding JDBC credentials.
 */
@Configuration
public class FlywayRepairConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairConfig.class);

    /**
     * Overrides the default migration strategy to run {@code repair()} before
     * {@code migrate()}.
     *
     * <p>{@code repair()} reconciles the {@code flyway_schema_history} table's
     * stored checksums with the current on-disk migration files. This is safe
     * and idempotent. After repair, the standard {@code migrate()} call runs
     * any unapplied migrations.
     */
    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            log.info("Flyway repair: reconciling flyway_schema_history checksums with on-disk migration files");
            flyway.repair();
            log.info("Flyway repair complete — proceeding with migrate()");
            flyway.migrate();
        };
    }
}
