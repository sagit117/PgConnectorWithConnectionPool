package ru.axel.connectors.postgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public interface PostgresConnectorUse<T> {
    T use(Connection connection) throws SQLException;
}
