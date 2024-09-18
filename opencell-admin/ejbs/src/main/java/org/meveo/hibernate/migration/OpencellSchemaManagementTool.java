package org.meveo.hibernate.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.jboss.logging.Logger;
import org.meveo.jpa.EntityManagerProvider;
import org.meveo.util.Version;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

/**
 * Database schema management tool for Opencell</br>
 * 
 * Implementation based on https://docs.jboss.org/hibernate/orm/5.3/integrationguide/html_single/Hibernate_Integration_Guide.html
 * 
 * @author Andrius Karpavicius
 *
 */
public class OpencellSchemaManagementTool extends HibernateSchemaManagementTool {

    private static final long serialVersionUID = -2534833314913182786L;

    private static final Logger log = Logger.getLogger(OpencellSchemaManagementTool.class);

    private static final String DB_CHANGELLOG_REBUILD = "db_resources/changelog/db.rebuild.xml";

    private static final String DB_CHANGELLOG_CURRENT = "db_resources/changelog/db.current.xml";

    private static final String DB_DATA_SOURCE_NAME = "java:jboss/datasources/MeveoAdminDatasource";

    /**
     * An environment variable indicating if DB migration should be done manually. DB_MIGRATION_MANUAL = true indicates manual migration. If omitted, a default of false is considered.
     */
    private static final String ENV_FLAG_DB_MIGRATION_MANUAL = "DB_MIGRATION_MANUAL";

    /**
     * Database migration status
     */
    private enum DbMigrationStatusEnum {

        /**
         * A fresh new DB, no tables created yet
         */
        NEW_DB,

        /**
         * Migration task is required
         */
        MIGRATION_REQUIRED,

        /**
         * Migration was completed already for a given build number
         */
        MIGRATION_COMPLETED;

    }

    @SuppressWarnings("rawtypes")
    @Override
    public SchemaCreator getSchemaCreator(Map options) {

        return new SchemaCreator() {

            @Override
            public void doCreation(Metadata metadata, ExecutionOptions executionOptions, ContributableMatcher contributableInclusionFilter, SourceDescriptor sourceDescriptor, TargetDescriptor targetDescriptor) {
                log.info("Will proceed to create DB schema");

                try {
                    runLiquibaseUpdateAndValidation(metadata, executionOptions, contributableInclusionFilter, options, false);

                } catch (Exception e) {
                    log.error("Failed to setup DB with latest changes", e);
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SchemaDropper getSchemaDropper(Map options) {

        log.info("Will proceede with a default Hibernate schema dropper");

        return super.getSchemaDropper(options);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SchemaMigrator getSchemaMigrator(Map options) {
        return new SchemaMigrator() {

            @Override
            public void doMigration(Metadata metadata, ExecutionOptions executionOptions, ContributableMatcher contributableInclusionFilter, TargetDescriptor targetDescriptor) {
                log.info("Will proceed to migrate DB schema");

                try {

                    String dbMigrationManual = System.getenv(ENV_FLAG_DB_MIGRATION_MANUAL);
                    boolean isDBMigrationManual = dbMigrationManual != null && Boolean.parseBoolean(dbMigrationManual);

                    runLiquibaseUpdateAndValidation(metadata, executionOptions, contributableInclusionFilter, options, isDBMigrationManual);

                } catch (Exception e) {
                    log.error("Failed to update DB schema with latest changes", e);
                    throw new RuntimeException(e);
                }
            }

        };
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SchemaValidator getSchemaValidator(Map options) {

        return new SchemaValidator() {

            @Override
            public void doValidation(Metadata metadata, ExecutionOptions executionOptions, ContributableMatcher contributableInclusionFilter) {

                log.info("Will proceed to validate DB schema");

                try {
                    runLiquibaseUpdateAndValidation(metadata, executionOptions, contributableInclusionFilter, options, true);

                } catch (Exception e) {
                    log.error("Failed to validate DB schema", e);
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Run Liquibase update operation and validate DB schema
     * 
     * @param metadata Metadata
     * @param executionOptions Execution options
     * @param contributableInclusionFilter Filter for Contributable instances to use
     * @param options Options
     * @throws LiquibaseException Failed to execute Liquibase update
     */
    @SuppressWarnings("deprecation")
    private void runLiquibaseUpdateAndValidation(Metadata metadata, ExecutionOptions executionOptions, ContributableMatcher contributableInclusionFilter, @SuppressWarnings("rawtypes") Map options, boolean validateOnly)
            throws LiquibaseException {
        try {
            InitialContext initialContext = new InitialContext();
            DataSource dataSource = (DataSource) initialContext.lookup(DB_DATA_SOURCE_NAME);

            Connection connection = dataSource.getConnection();
            DbMigrationStatusEnum dbMigrationStatus = getDBMigrationStatus(connection, Version.buildNumber);

            if (dbMigrationStatus == DbMigrationStatusEnum.MIGRATION_COMPLETED) {
                log.info("Database is already up to date for build " + Version.buildNumber + ". Will skip DB migration and schema validation");
                return;
            }

            // Run Liquibase update from a corresponding file
            if (!validateOnly) {
                runLiquibase(connection, dbMigrationStatus == DbMigrationStatusEnum.NEW_DB ? DB_CHANGELLOG_REBUILD : DB_CHANGELLOG_CURRENT);
            }

            // Run a default schema validation
            log.info("Will proceede with a default Hibernate schema validator");
            super.getSchemaValidator(options).doValidation(metadata, executionOptions, contributableInclusionFilter);

            updateMigrationStatus(dataSource.getConnection(), Version.buildNumber, Version.appVersion);

        } catch (SQLException | NamingException e) {
            log.error("Failed to obtain a DB data source");
            throw new RuntimeException("Failed to obtain a DB data source " + DB_DATA_SOURCE_NAME, e);
        }

    }

    /**
     * Run a Liquibase update command from a given Liquibase changeset file
     * 
     * @param connection Database connection
     * @param changelogFilename Change log filename
     * @throws LiquibaseException Unrecognized DB type from a DB connection
     */
    private void runLiquibase(Connection connection, String changelogFilename) throws LiquibaseException {

        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

        ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(getClass().getClassLoader());

        Liquibase liquibase = new Liquibase(changelogFilename, resourceAccessor, database);

        boolean isPostgressDB = !EntityManagerProvider.isDBOracle();

        if (isPostgressDB) {
            // database.setDefaultSchemaName("public");
            liquibase.getChangeLogParameters().set("db.schema", "public");
        }

        liquibase.getChangeLogParameters().set("liquibase_file_prefix", "");

        log.info("Will execute liquibase update from file " + changelogFilename);
        liquibase.update(new Contexts(), new LabelExpression());
        liquibase.close();
    }

    /**
     * Check DB migration status for a given build number
     * 
     * @param connection Database connection
     * @param buildNr Opencell application build number
     * @return Database migration status
     */
    private DbMigrationStatusEnum getDBMigrationStatus(Connection connection, String buildNr) {

        try (ResultSet resultset = connection.createStatement().executeQuery("select 1 from db_migration_status where build_nr='" + buildNr + "'")) {

            // Migration is required if no entry found for a given build number
            if (resultset.next()) {
                return DbMigrationStatusEnum.MIGRATION_COMPLETED;
            } else {
                return DbMigrationStatusEnum.MIGRATION_REQUIRED;
            }
            // DB was not initialized yet - table does not exist yet
        } catch (SQLException e) {
            return DbMigrationStatusEnum.NEW_DB;
        }
    }

    /**
     * Update DB migration status as completed for a given build number
     * 
     * @param connection Database connection
     * @param buildNr Opencell application build number
     * @param appVersion Opencell application version
     */
    private void updateMigrationStatus(Connection connection, String buildNr, String appVersion) {

        try (Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.executeUpdate("insert into db_migration_status (build_nr, app_version) values ('" + buildNr + "','" + appVersion + "')");

            connection.commit();

            log.info("Successfully migrated DB for build nr " + buildNr + ", application version " + appVersion);
        } catch (SQLException e) {
            log.error("Failed to update DB migration status", e);
        }
    }
}