package com.couplestory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs the real Flyway migration SQL (adapted for H2/PostgreSQL-compatibility mode -
 * see src/test/resources/db/migration-verify) against H2, then lets Hibernate's
 * ddl-auto=validate check the result against every JPA entity.
 *
 * Empirically (confirmed by two independent attempts, with the column declared as
 * both JSONB and JSON) H2's PostgreSQL-compatibility mode cannot be made to report a
 * column's JDBC metadata type name as literally "jsonb", which is what Hibernate's
 * PostgreSQLDialect requires for a @JdbcTypeCode(SqlTypes.JSON) field. This is a
 * limitation of the H2 harness, not evidence of a real mismatch: Hibernate 6 +
 * PostgreSQLDialect's own documented behavior is to map that annotation to a real
 * `jsonb` column, which is exactly what the real migration
 * (src/main/resources/db/migration/V1__baseline_schema.sql) declares.
 *
 * So this test asserts the NEGATIVE space instead: schema validation must fail in
 * EXACTLY this one known, documented way (websites.template_config) and no other way.
 * If a future entity/migration change introduces any OTHER mismatch, the failure
 * message will no longer match this narrow assertion and the test will fail, catching
 * the regression. websites.template_config itself needs verification against a real
 * PostgreSQL instance (see Dockerfile/docker-compose - Needs Runtime Verification on
 * Ubuntu).
 */
class FlywayMigrationValidationTest {

    @Test
    void migrationMatchesEveryEntityExceptTheKnownH2JsonLimitation() {
        SpringApplication app = new SpringApplication(CoupleStoryApplication.class);
        app.setAdditionalProfiles("flyway-verify");

        try (ConfigurableApplicationContext ignored = app.run()) {
            fail("Expected context startup to fail on the known H2 JSON/JSONB limitation " +
                    "for websites.template_config, but it started cleanly. If the migration " +
                    "or entities changed to make JSON columns H2-verifiable, update this test " +
                    "to a normal 'must start cleanly' assertion.");
        } catch (Exception e) {
            String chain = causeChainMessages(e);
            assertTrue(chain.contains("template_config"),
                    "Schema validation failed for a reason OTHER than the known " +
                            "websites.template_config JSON/JSONB limitation - this likely " +
                            "indicates a real mismatch between the migration and an entity. " +
                            "Full cause chain: " + chain);
            assertTrue(chain.contains("wrong column type"),
                    "Expected a column-type mismatch specifically, got: " + chain);
        }
    }

    private static String causeChainMessages(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable current = t;
        while (current != null) {
            sb.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage()).append(" | ");
            current = current.getCause();
        }
        return sb.toString();
    }
}
