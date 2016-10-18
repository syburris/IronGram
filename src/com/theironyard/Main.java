package com.theironyard;

import jodd.json.JsonSerializer;
import spark.Session;
import spark.Spark;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;

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

    static void insertImage(Connection conn, String filename, int userId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO images VALUES (NULL, ?, ?)");
        stmt.setString(1, filename);
        stmt.setInt(2, userId);
        stmt.execute();
    }

    static ArrayList<Image> selectImages(Connection conn) throws SQLException {
        ArrayList<Image> images = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM images INNER JOIN users ON images.user_id = users.id");
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            int id = results.getInt("images.id");
            String filename = results.getString("images.filename");
            String author = results.getString("users.name");
            Image img = new Image(id, filename, author);
            images.add(img);
        }
        return images;
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

        Spark.post(
                "/upload",
                (request, response) -> {
                    request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
                    try (InputStream is = request.raw().getPart("image").getInputStream()) {
                        File dir = new File("public/images");
                        dir.mkdirs();
                        File f = File.createTempFile("image", request.raw().getPart("image").getSubmittedFileName(), dir);
                        FileOutputStream fos = new FileOutputStream(f);
                        if (is.available() > 1024 * 1024 * 20) {
                            return null;
                        }
                        byte[] buffer = new byte[is.available()];
                        is.read(buffer);
                        fos.write(buffer);

                        Session session = request.session();
                        User user = selectUser(conn, session.attribute("username"));
                        insertImage(conn, f.getName(), user.id);
                    }
                    response.redirect("/");
                    return null;
                }
        );

        Spark.get(
                "/images",
                (request, response) -> {
                    JsonSerializer serializer = new JsonSerializer();
                    return serializer.serialize(selectImages(conn));
                }
        );
    }
}
