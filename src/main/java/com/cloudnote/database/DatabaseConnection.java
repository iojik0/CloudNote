package com.cloudnote.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    public Connection getCon() throws SQLException {
        Connection conn;
        conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/cloudnote?characterEncoding=utf8&useUnicode=true",
                "postgres",
                "1234"
        );
        return conn;
    }
}