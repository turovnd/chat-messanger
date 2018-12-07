package demo;

import org.hsqldb.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class DataStore {
    private List<String> messages = Collections.synchronizedList(new ArrayList<>());
    private Connection connection = null;

    DataStore(boolean clearDB) {


        Server hsqlServer = new Server();
        hsqlServer.setLogWriter(null);
        hsqlServer.setSilent(true);
        hsqlServer.setDatabaseName(0, "chatdb");
        hsqlServer.setDatabasePath(0, "file:chatdb");

        hsqlServer.start();

        // making a connection
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/chatdb", "sa", ""); // can through sql exception
            if (clearDB) {
                connection.prepareStatement("drop table history if exists;").execute();
            }
            connection.prepareStatement("create table history (message varchar(20) not null, creator varchar(50) not null);").execute();

        } catch (SQLException | ClassNotFoundException e2) {
            e2.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public void add(String message, String creator) throws SQLException {
        connection.prepareStatement("insert into history values('" + message + "', '" + creator +"');").execute();
        messages.add(message);
    }

    public List<String> get() throws SQLException {
        ResultSet rs = connection.prepareStatement("select * from history;").executeQuery();
        List<String> messages = Collections.synchronizedList(new ArrayList<>());
        while (rs.next()) {
            messages.add(rs.getString("message"));
        }
        return messages;
    }
}