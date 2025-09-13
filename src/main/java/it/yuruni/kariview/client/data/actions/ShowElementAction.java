package it.yuruni.kariview.client.data.actions;

public class ShowElementAction implements Action {
    private String elementId;
    private int x;
    private int y;
    private int width;
    private int height;

    public String getElementId() {
        return elementId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
