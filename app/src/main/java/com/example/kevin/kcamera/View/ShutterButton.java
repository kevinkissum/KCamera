package com.example.kevin.kcamera.View;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class ShutterButton extends ImageView {

    public interface OnShutterButtonListener {
        /**
         * Called when a ShutterButton has been pressed.
         *
         * @param pressed The ShutterButton that was pressed.
         */
        void onShutterButtonFocus(boolean pressed);
        void onShutterButtonClick();

        /**
         * Called when shutter button is held down for a long press.
         */
        void onShutterButtonLongPressed();
    }

    private List<OnShutterButtonListener> mListeners
            = new ArrayList<OnShutterButtonListener>();

    public ShutterButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Add an {@link OnShutterButtonListener} to a set of listeners.
     */
    public void addOnShutterButtonListener(OnShutterButtonListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * Remove an {@link OnShutterButtonListener} from a set of listeners.
     */
    public void removeOnShutterButtonListener(OnShutterButtonListener listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    @Override
    public boolean performClick() {
        Log.d("kk", " Shutter performClick ");
        boolean result = super.performClick();
        if (getVisibility() == View.VISIBLE) {
            for (OnShutterButtonListener listener : mListeners) {
                listener.onShutterButtonClick();
            }
        }
        return result;
    }
}
