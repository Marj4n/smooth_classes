package org.marj4n.smooth_classes.client.gui;

public class TextureState {
    public float x;
    public float y;
    public float speed;
    public float scale;
    public long animationSpeed;
    public float brightness;

    public TextureState(float x, float y, float speed, float scale, long animationSpeed, float brightness) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.scale = scale;
        this.animationSpeed = animationSpeed;
        this.brightness = brightness;
    }
}
