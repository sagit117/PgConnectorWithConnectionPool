package ru.axel.connectors.postgres;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws SQLException, InterruptedException {
        final Logger logger = Logger.getLogger(Main.class.getName());
        logger.setLevel(Level.ALL);

        final PostgresConnector pgConnector = new PostgresConnector(
            "jdbc:postgresql://localhost:5432/cookbook",
            "admin-postgres",
            "PasSW0rd",
            Executors.newWorkStealingPool(10),
            logger
        );

        pgConnector.use(connection -> {
            String stringQueryCreateTable = "SELECT * FROM \"Users\" WHERE \"ID\" = 1;";

            final var query = connection.prepareStatement(stringQueryCreateTable);
            final var result = query.executeQuery();

            result.next();
            System.out.println(result.getInt("ID"));
        });

        pgConnector.use(connection -> {
            final String stringQueryCreateTable = "SELECT * FROM \"Users\" WHERE \"ID\" = 1;";

            final var query = connection.prepareStatement(stringQueryCreateTable);
            final var result = query.executeQuery();

            result.next();
            System.out.println(result.getInt("ID"));
        });

        for (int i = 0; i < 100000; i++) {
            int finalI = i;

            try {
                pgConnector.use(connection -> {
                    String stringQueryCreateTable = "SELECT * FROM \"Users\" WHERE \"ID\" = 1;";

                    final var query = connection.prepareStatement(stringQueryCreateTable);
                    final var result = query.executeQuery();

                    result.next();
                    System.out.println("result" + finalI + " = "  + result.getInt("ID"));
                });
            } catch (SQLException e) {
                System.out.println("connections err");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        Thread.sleep(10000);
    }
}