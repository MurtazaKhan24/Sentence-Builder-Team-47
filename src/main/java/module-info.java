module sentencebuilder {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.slf4j;

    opens sentencebuilder.db.model to javafx.base;
    opens sentencebuilder.ui to javafx.graphics;

    exports sentencebuilder.ui;
    exports sentencebuilder.db;
    exports sentencebuilder.db.model;
    exports sentencebuilder.parser;
    exports sentencebuilder.algorithms;
}
