package ru.axel.connectors.postgres;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PostgresConnector {
    private final String dbUrl;
    private final String dbUserName;
    private final char[] dbPassword;
    private ExecutorService executor;
    private final List<ConnectionStatus> connectionPool = new ArrayList<>();
    private final Logger logger;

    public PostgresConnector(String url, String userName, char[] password, Logger logger) throws SQLException {
        dbUrl = url;
        dbUserName = userName;
        dbPassword = password;
        this.logger = logger;

        firstCreateConnections(10);
    }
    public PostgresConnector(
        String url,
        String userName,
        char[] password,
        ExecutorService executorService,
        Logger logger
    ) throws SQLException {
        dbUrl = url;
        dbUserName = userName;
        dbPassword = password;
        executor = executorService;
        this.logger = logger;

        firstCreateConnections(10);
    }

    /**
     * Создает первые n соединений.
     * @param count количество соединений.
     * @throws SQLException
     */
    private void firstCreateConnections(int count) throws SQLException {
        for (var i = 0; i < count; i++) {
            addNewConnection();
        }
    }

    private Connection createConnection() throws SQLException {
        if (logger.isLoggable(Level.CONFIG)) {
            logger.severe("Создано соединение #" + connectionPool.size());
        }

        return DriverManager.getConnection(dbUrl, dbUserName, String.valueOf(dbPassword));
    }

    /**
     * Метод проверяет существует ли соединение с БД в текущем потоке,
     * если не существует, вернет новое соединение, иначе существующее.
     * @return класс статус соединение с БД
     * @throws SQLException ошибка соединения с БД
     */
    private @NotNull ConnectionStatus getConnection() throws SQLException {
        final var optionalFreeConnection = connectionPool
            .stream()
            .filter(connection -> !connection.isUse())
            .findAny();

        if (optionalFreeConnection.isPresent()) {
            final ConnectionStatus connectionStatus = optionalFreeConnection.get();

            if (logger.isLoggable(Level.CONFIG)) {
                logger.severe("Пытаемся использовать существующие соединение");
            }

            return connectionStatus.getConnection() == null ? addNewConnection() : connectionStatus;
        } else {
            return addNewConnection();
        }
    }

    /**
     * Создает и добавляет в пул новое соединение.
     * @return класс статус соединение.
     * @throws SQLException
     */
    private @NotNull ConnectionStatus addNewConnection() throws SQLException {
        final var conn = new ConnectionStatus(createConnection());
        connectionPool.add(conn);

        return conn;
    }

    public <T> @NotNull CompletableFuture<T> use(@NotNull PostgresConnectorUse<T> useMethod) throws SQLException {
        final ConnectionStatus connectionStatus = getConnection();
        connectionStatus.setUse(true);

        if (logger.isLoggable(Level.CONFIG)) {
            logger.severe("Запрос к БД выполняется");
        }

        if (executor != null) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    var resut = useMethod.use(connectionStatus.getConnection());
                    connectionStatus.setUse(false);

                    return resut;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                if (logger.isLoggable(Level.CONFIG)) {
                    logger.severe("Запрос выполняется в потоке: " + Thread.currentThread().getName());
                }

                try {
                    var resut = useMethod.use(connectionStatus.getConnection());
                    connectionStatus.setUse(false);

                    return resut;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public void printConnectionsStatus() {
        AtomicInteger i = new AtomicInteger();

        connectionPool.forEach(connectionStatus -> {
            System.out.println("Соединение #" + i + ", статус использования: " + connectionStatus.isUse());
            i.getAndIncrement();
        });
    }
}
