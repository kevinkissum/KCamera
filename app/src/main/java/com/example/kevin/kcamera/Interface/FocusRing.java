package com.example.kevin.kcamera.Interface;


import android.graphics.RectF;

/**
 * Primary interface for interacting with the focus ring UI.
 */
public interface FocusRing {
    /**
     * Check the state of the passive focus ring animation.
     *
     * @return whether the passive focus animation is running.
     */
    public boolean isPassiveFocusRunning();
    /**
     * Check the state of the active focus ring animation.
     *
     * @return whether the active focus animation is running.
     */
    public boolean isActiveFocusRunning();
    /**
     * Start a passive focus animation.
     */
    public void startPassiveFocus();
    /**
     * Start an active focus animation.
     */
    public void startActiveFocus();
    /**
     * Stop any currently running focus animations.
     */
    public void stopFocusAnimations();
    /**
     * Set the location of the focus ring animation center.
     */
    public void setFocusLocation(float viewX, float viewY);

    /**
     * Set the location of the focus ring animation center.
     */
    public void centerFocusLocation();

    /**
     * Set the target radius as a ratio of min to max visible radius
     * which will internally convert and clamp the value to the
     * correct pixel radius.
     */
    public void setRadiusRatio(float ratio);

    /**
     * The physical size of preview can vary and does not map directly
     * to the size of the view. This allows for conversions between view
     * and preview space for values that are provided in preview space.
     */
    void configurePreviewDimensions(RectF previewArea);
}