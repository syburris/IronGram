package com.theironyard;

import spark.Spark;

public class Main {

    public static void main(String[] args) {
        Spark.externalStaticFileLocation("public");
        Spark.init();
    }
}
