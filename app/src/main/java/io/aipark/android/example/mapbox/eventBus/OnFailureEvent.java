package io.aipark.android.example.mapbox.eventBus;

public class OnFailureEvent {
    private Throwable throwable;

    public OnFailureEvent() {
    }

    public OnFailureEvent(Throwable throwable) {
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
