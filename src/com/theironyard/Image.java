package com.theironyard;

/**
 * This is not a comment
 */
public class Image {
    int id;
    String filename;
    String author;

    public Image(int id, String filename, String author) {
        this.id = id;
        this.filename = filename;
        this.author = author;
    }

    public int getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getAuthor() {
        return author;
    }
}
