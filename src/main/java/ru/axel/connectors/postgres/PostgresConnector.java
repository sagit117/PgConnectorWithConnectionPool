package ru.axel.connectors.postgres;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class PostgresConnector {
    private final String dbUrl;
    private final String dbUserName;
    private final String dbPassword;
    private ExecutorService executor;
    private final ConcurrentHashMap<Thread, Connection> connectionPool = new ConcurrentHashMap<>();

    public PostgresConnector(String url, String userName, String password) {
        dbUrl = url;
        dbUserName = userName;
        dbPassword = password;
    }
    public PostgresConnector(String url, String userName, String password, ExecutorService executorService) {
        dbUrl = url;
        dbUserName = userName;
        dbPassword = password;
        executor = executorService;
    }

    private Connection createConnection() throws SQLException {
        System.out.println("Создано соединение для потока: " + Thread.currentThread().getName());
        return DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
    }

    private Connection getConnection() throws SQLException {
        var currentThread = Thread.currentThread();

        if (connectionPool.containsKey(currentThread)) {
            var conn = connectionPool.get(currentThread);
            return conn.isClosed() ? addNewConnection(currentThread) : conn;
        } else {
            return addNewConnection(currentThread);
        }
    }

    private Connection addNewConnection(Thread thread) throws SQLException {
        var conn = createConnection();
        connectionPool.put(thread, conn);

        return conn;
    }

    public void use(@NotNull PostgresConnectorUse useMethod) throws SQLException {
        if (executor != null) {
            executor.execute(() -> {
                try {
                    useMethod.use(getConnection());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            useMethod.use(getConnection());
        }
    }
}
