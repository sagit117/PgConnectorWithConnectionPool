package ru.axel.connectors.postgres;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final Logger logger = Logger.getLogger(Main.class.getName());
        logger.setLevel(Level.ALL);

        final PostgresConnector pgConnector = new PostgresConnector(
            "jdbc:postgresql://localhost:5432/river",
            "admin-postgres",
            "PasSW0rd".toCharArray(),
            Executors.newWorkStealingPool(10),
            logger
        );

        CompletableFuture<Integer> id = pgConnector.use((connection) -> {
            final String stringQueryCreateTable = "SELECT * FROM \"Users\" WHERE \"ID\" = 2;";

            final var query = connection.prepareStatement(stringQueryCreateTable);
            final var result = query.executeQuery();

            result.next();
            return result.getInt("ID");
        });

        id.thenAccept(System.out::println).exceptionally((ex) -> {
            System.out.println(ex.getLocalizedMessage());
            return null;
        }).get();
    }
}