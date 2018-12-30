package io.aipark.android.example.mapbox.eventBus;

public class MapThemeChangeEvent {
    public enum Theme {
        LIGHT, DARK
    }

    private Theme theme;

    public MapThemeChangeEvent(Theme theme) {
        this.theme = theme;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }
}
