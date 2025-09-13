package it.yuruni.kariview.client.data.actions;

public class MoveElementAction implements Action {
    private String elementId;
    private int x;
    private int y;
    private int duration;
    private String easing;

    public String getElementId() {
        return elementId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDuration() {
        return duration;
    }

    public String getEasing() {
        return easing;
    }
}
