package com.cloudnote.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    public Connection getCon() throws SQLException {
        Connection conn;
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/dbcloudnote?useSSL=false&serverTimezone=UTC", "root", "1234");
        return conn;
    }
}
