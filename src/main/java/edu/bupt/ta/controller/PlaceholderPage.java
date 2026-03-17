package edu.bupt.ta.controller;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class PlaceholderPage {

    private PlaceholderPage() {
    }

    public static Parent simple(String title, String text) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(24));
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label desc = new Label(text);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
        box.getChildren().addAll(heading, desc);
        return box;
    }
}
