package com.theironyard;

import jodd.json.JsonSerializer;
import spark.Session;
import spark.Spark;

import java.sql.*;

public class Main {

    static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, name VARCHAR)");
        stmt.execute("CREATE TABLE IF NOT EXISTS images (id IDENTITY, filename VARCHAR, user_id INT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS recipients (id IDENTITY, user_id INT, image_id INT)");
    }

    public static void insertUser(Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?)");
        stmt.setString(1, name);
        stmt.execute();
    }

    public static User selectUser(Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
        stmt.setString(1, name);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int id = results.getInt("id");
            return new User(id, name);
        }
        return null;
    }

    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

        Spark.externalStaticFileLocation("public");
        Spark.init();

        Spark.post(
                "/login",
                (request, response) -> {
                    String name = request.queryParams("username");
                    User user = selectUser(conn, name);
                    if (user == null) {
                        insertUser(conn, name);
                    }
                    Session session = request.session();
                    session.attribute("username", name);
                    response.redirect("/");
                    return null;
                }
        );

        Spark.get(
                "/user",
                (request, response) -> {
                    Session session = request.session();
                    String name = session.attribute("username");
                    if (name == null) {
                        return "";
                    }
                    User user = selectUser(conn, name);
                    JsonSerializer serializer = new JsonSerializer();
                    return serializer.serialize(user);
                }
        );

        Spark.post(
                "/logout",
                (request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return null;
                }
        );
    }
}
