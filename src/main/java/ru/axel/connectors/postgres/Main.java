package ru.axel.connectors.postgres;

import java.sql.SQLException;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws SQLException, InterruptedException {
        PostgresConnector pgConnector = new PostgresConnector(
            "jdbc:postgresql://localhost:5432/cookbook",
            "admin-postgres",
            "PasSW0rd",
            Executors.newWorkStealingPool(10)
        );

        pgConnector.use(connection -> {
            String stringQueryCreateTable = "SELECT * FROM \"Users\" WHERE \"ID\" = 1;";

            var query = connection.prepareStatement(stringQueryCreateTable);
            var result = query.executeQuery();

            result.next();
            System.out.println(result.getInt("ID"));
        });

        pgConnector.use(connection -> {
            String stringQueryCreateTable = "SELECT * FROM \"Users\" WHERE \"ID\" = 1;";

            var query = connection.prepareStatement(stringQueryCreateTable);
            var result = query.executeQuery();

            result.next();
            System.out.println(result.getInt("ID"));
        });

        for (int i = 0; i < 100000; i++) {
            int finalI = i;

            try {
                pgConnector.use(connection -> {
                    String stringQueryCreateTable = "SELECT * FROM \"Users\" WHERE \"ID\" = 1;";

                    var query = connection.prepareStatement(stringQueryCreateTable);
                    var result = query.executeQuery();

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