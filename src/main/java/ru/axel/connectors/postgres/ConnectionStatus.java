package ru.axel.connectors.postgres;

import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

final class ConnectionStatus {
    private AtomicBoolean isUse = new AtomicBoolean(false);
    private final Connection connection;

    ConnectionStatus(Connection connection) {
        this.connection = connection;
    }

    void setUse(boolean use) {
        isUse.set(use);
    }

    boolean isUse() {
        return isUse.get();
    }

    @Nullable Connection getConnection() throws SQLException {
        return connection.isClosed() ? null : connection;
    }
}
