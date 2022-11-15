package ru.axel.connectors.postgres;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public final class PostgresConnector {
    private final String dbUrl;
    private final String dbUserName;
    private final char[] dbPassword;
    private ExecutorService executor;
    private final ConcurrentHashMap<Thread, Connection> connectionPool = new ConcurrentHashMap<>();
    private final Logger logger;

    public PostgresConnector(String url, String userName, char[] password, Logger logger) {
        dbUrl = url;
        dbUserName = userName;
        dbPassword = password;
        this.logger = logger;
    }
    public PostgresConnector(
        String url,
        String userName,
        char[] password,
        ExecutorService executorService,
        Logger logger
    ) {
        dbUrl = url;
        dbUserName = userName;
        dbPassword = password;
        executor = executorService;
        this.logger = logger;
    }

    private Connection createConnection() throws SQLException {
        logger.config("Создано соединение для потока: " + Thread.currentThread().getName());
        final StringBuilder password = new StringBuilder();

        for (char str : dbPassword) {
            password.append(str);
        }

        return DriverManager.getConnection(dbUrl, dbUserName, password.toString());
    }

    /**
     * Метод проверяет существует ли соединение с БД в текущем потоке,
     * если не существует, вернет новое соединение, иначе существующее.
     * @return соединение с БД
     * @throws SQLException ошибка соединения с БД
     */
    private Connection getConnection() throws SQLException {
        final var currentThread = Thread.currentThread();

        if (connectionPool.containsKey(currentThread)) {
            final var conn = connectionPool.get(currentThread);
            return conn.isClosed() ? addNewConnection(currentThread) : conn;
        } else {
            return addNewConnection(currentThread);
        }
    }

    private Connection addNewConnection(Thread thread) throws SQLException {
        final var conn = createConnection();
        connectionPool.put(thread, conn);

        return conn;
    }

    public <T> @NotNull CompletableFuture<T> use(@NotNull PostgresConnectorUse<T> useMethod) {
        if (executor != null) {
             return CompletableFuture.supplyAsync(() -> {
                try {
                    return useMethod.use(getConnection());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return useMethod.use(getConnection());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }
}
