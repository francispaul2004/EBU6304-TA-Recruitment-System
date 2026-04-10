package edu.bupt.ta.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public final class IconFactory {

    public enum IconType {
        GRADUATION_CAP,
        DASHBOARD,
        SEARCH,
        FILE,
        USER,
        HELP,
        SETTINGS,
        BRIEFCASE,
        USERS,
        CLIPBOARD,
        BELL,
        LOGOUT,
        UPLOAD,
        CHECK_CIRCLE,
        ALERT_TRIANGLE,
        INFO_CIRCLE,
        CALENDAR,
        PENCIL,
        CHEVRON_RIGHT,
        FILTER,
        EYE,
        LOCK,
        SHIELD,
        TRASH
    }

    private IconFactory() {
    }

    public static StackPane glyph(IconType type, double size, Color color) {
        FontIcon icon = FontIcon.of(iconCode(type), (int) Math.max(10, Math.round(size)));
        icon.setIconColor(color);
        StackPane box = new StackPane(icon);
        box.setPickOnBounds(false);
        return box;
    }

    public static StackPane badge(IconType type, double size, Color background, Color foreground) {
        StackPane badge = new StackPane(glyph(type, size * 0.58, foreground));
        badge.setMinSize(size, size);
        badge.setPrefSize(size, size);
        badge.setMaxSize(size, size);
        badge.setBackground(new Background(new BackgroundFill(background, new CornerRadii(size * 0.24), Insets.EMPTY)));
        return badge;
    }

    public static StackPane notificationBell(double size, Color iconColor, Color dotColor) {
        StackPane icon = glyph(IconType.BELL, size * 0.64, iconColor);
        Circle dot = new Circle(Math.max(2.2, size * 0.09), dotColor);
        StackPane.setMargin(dot, new Insets(size * 0.16, size * 0.14, 0, 0));
        dot.setTranslateX(size * 0.22);
        dot.setTranslateY(-size * 0.22);

        StackPane wrapper = new StackPane(icon, dot);
        wrapper.setMinSize(size, size);
        wrapper.setPrefSize(size, size);
        wrapper.setMaxSize(size, size);
        return wrapper;
    }

    private static FontAwesomeSolid iconCode(IconType type) {
        return switch (type) {
            case GRADUATION_CAP -> FontAwesomeSolid.GRADUATION_CAP;
            case DASHBOARD -> FontAwesomeSolid.TH_LARGE;
            case SEARCH -> FontAwesomeSolid.SEARCH;
            case FILE -> FontAwesomeSolid.FILE_ALT;
            case USER -> FontAwesomeSolid.USER;
            case HELP -> FontAwesomeSolid.QUESTION_CIRCLE;
            case SETTINGS -> FontAwesomeSolid.COG;
            case BRIEFCASE -> FontAwesomeSolid.BRIEFCASE;
            case USERS -> FontAwesomeSolid.USERS;
            case CLIPBOARD -> FontAwesomeSolid.CLIPBOARD_LIST;
            case BELL -> FontAwesomeSolid.BELL;
            case LOGOUT -> FontAwesomeSolid.SIGN_OUT_ALT;
            case UPLOAD -> FontAwesomeSolid.UPLOAD;
            case CHECK_CIRCLE -> FontAwesomeSolid.CHECK_CIRCLE;
            case ALERT_TRIANGLE -> FontAwesomeSolid.EXCLAMATION_TRIANGLE;
            case INFO_CIRCLE -> FontAwesomeSolid.INFO_CIRCLE;
            case CALENDAR -> FontAwesomeSolid.CALENDAR_ALT;
            case PENCIL -> FontAwesomeSolid.PEN;
            case CHEVRON_RIGHT -> FontAwesomeSolid.CHEVRON_RIGHT;
            case FILTER -> FontAwesomeSolid.FILTER;
            case EYE -> FontAwesomeSolid.EYE;
            case LOCK -> FontAwesomeSolid.LOCK;
            case SHIELD -> FontAwesomeSolid.SHIELD_ALT;
            case TRASH -> FontAwesomeSolid.TRASH_ALT;
        };
    }
}
