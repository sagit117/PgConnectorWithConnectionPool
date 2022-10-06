package ru.axel.connectors.postgres;

import java.sql.Connection;
import java.sql.SQLException;

public interface PostgresConnectorUse {
    void use(Connection connection) throws SQLException;
}
