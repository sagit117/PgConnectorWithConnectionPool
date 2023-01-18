package ru.axel.connectors.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws SQLException, ExecutionException, InterruptedException {
        var ex1 = Executors.newWorkStealingPool();

        final Logger logger = Logger.getLogger(Main.class.getName());
        logger.setLevel(Level.ALL);

        final PostgresConnector pgConnector = new PostgresConnector(
            "jdbc:postgresql://localhost:5432/shop-jvm",
            "admin-postgres",
            "PasSW0rd".toCharArray(),
            ex1,
            logger
        );

        var result = pgConnector.use(connection -> {
            final String stringQueryCreateTable = """
                    CREATE TABLE IF NOT EXISTS "Users"
                        (
                            "ID"            SERIAL PRIMARY KEY,
                            "EMAIL"         CHARACTER VARYING(64) UNIQUE NOT NULL,
                            "PASSWORD"      CHARACTER VARYING(64) NOT NULL,
                            "AUTH_CODE"     CHARACTER VARYING(36) NOT NULL,
                            "LAST_LOGIN"    TIMESTAMP,
                            "CREATED_AT"    TIMESTAMP NOT NULL,
                            "UPDATED_AT"    TIMESTAMP
                        );
                    """;

            final PreparedStatement query = connection.prepareStatement(stringQueryCreateTable);
            query.execute();

            return true;
        });

        var result2 = pgConnector.use(connection -> {
            final String queryString = """
                SELECT COUNT(*) FROM "Users"
            """;

            final PreparedStatement query = connection.prepareStatement(queryString);
            final ResultSet res = query.executeQuery();

            res.next();

            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Выполнен запрос: SELECT COUNT(*) FROM \"Users\"");
            }

            return res.getInt(1);
        });

        pgConnector.printConnectionsStatus();
        System.out.println(result);
        pgConnector.printConnectionsStatus();
        System.out.println(result.get());
        pgConnector.printConnectionsStatus();
        System.out.println(result2.get());
        pgConnector.printConnectionsStatus();
    }
}