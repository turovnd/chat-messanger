package main.java.server;

import org.hsqldb.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerDB {
    private Connection connection = null;

    ServerDB(String table, boolean clearDB) {

        Server hsqlServer = new Server();
        hsqlServer.setLogWriter(null);
        hsqlServer.setSilent(true);
        hsqlServer.setDatabaseName(0, table);
        hsqlServer.setDatabasePath(0, "file:" + table);

        hsqlServer.start();

        // making a connection
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/" + table, "sa", ""); // can through sql exception
            if (clearDB) {
                connection.prepareStatement("drop table history if exists;").execute();
                connection.prepareStatement("create table history (message varchar(1024) not null);").execute();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    /**
     * Store message in DB.
     *
     * @param message - message from user.
     */
    public void addMessage(String message) {
        try {
            connection.prepareStatement("insert into history values('" + message + "');").execute();
        } catch (SQLException e) {
            System.out.println("Error on adding element to DB: " + e);
        }
    }

    /**
     * Get messages history from DB.
     *
     * @return List
     */
    public List<String> loadMessageHistory() {
        List<String> messages = Collections.synchronizedList(new ArrayList<>());
        try {
            ResultSet rs = connection.prepareStatement("select * from history;").executeQuery();
            while (rs.next()) {
                messages.add(rs.getString("message"));
            }
        } catch (SQLException e) {
            System.out.println("Error on adding element to DB: " + e);
        }
        return messages;
    }
}
