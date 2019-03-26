package com.example.kevin.kcamera;

import android.view.View;
import android.widget.ImageButton;

import com.example.kevin.kcamera.View.MultiToggleImageButton;

public class ButtonManager implements SettingsManager.OnSettingChangedListener {

    private final CameraActivity mActivity;

    public ButtonManager(CameraActivity activity) {
        mActivity = activity;
    }

    public interface ButtonCallback {
        public void onStateChanged(int state);
    }

    public static final int BUTTON_FLASH = 0;
    public static final int BUTTON_TORCH = 1;
    public static final int BUTTON_HDR_PLUS_FLASH = 2;
    public static final int BUTTON_CAMERA = 3;
    public static final int BUTTON_HDR_PLUS = 4;
    public static final int BUTTON_HDR = 5;
    public static final int BUTTON_CANCEL = 6;
    public static final int BUTTON_DONE = 7;
    public static final int BUTTON_RETAKE = 8;
    public static final int BUTTON_REVIEW = 9;
    public static final int BUTTON_GRID_LINES = 10;
    public static final int BUTTON_EXPOSURE_COMPENSATION = 11;
    public static final int BUTTON_COUNTDOWN = 12;

    private MultiToggleImageButton mButtonCamera;
    private MultiToggleImageButton mButtonFlash;
    private MultiToggleImageButton mButtonHdr;
    private MultiToggleImageButton mButtonGridlines;
    private MultiToggleImageButton mButtonCountdown;

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {

    }
    public void disableButton(int conflictingButton) {
//        View button;
//        if (buttonId == BUTTON_EXPOSURE_COMPENSATION) {
//            button = getImageButtonOrError(buttonId);
//        } else {
//            button = getButtonOrError(buttonId);
//        }
//        // HDR and HDR+ buttons share the same button object,
//        // but change actual image icons at runtime.
//        // This extra check is to ensure the correct icons are used
//        // in the case of the HDR[+] button being disabled at startup,
//        // e.g. app startup with front-facing camera.
//        // b/18104680
//        if (buttonId == BUTTON_HDR_PLUS) {
//            initializeHdrPlusButtonIcons((MultiToggleImageButton) button, R.array.pref_camera_hdr_plus_icons);
//        } else if (buttonId == BUTTON_HDR) {
//            initializeHdrButtonIcons((MultiToggleImageButton) button, R.array.pref_camera_hdr_icons);
//        }
//
//        if (button.isEnabled()) {
//            button.setEnabled(false);
//            if (mListener != null) {
//                mListener.onButtonEnabledChanged(this, buttonId);
//            }
//        }
//        button.setTag(R.string.tag_enabled_id, null);
    }

    public void initializeButton(int buttonId, ButtonCallback cb, ButtonCallback preCb) {
        MultiToggleImageButton button = getButtonOrError(buttonId);
        switch (buttonId) {
            case BUTTON_FLASH:
//                initializeFlashButton(button, cb, preCb, R.array.camera_flashmode_icons);
                break;
            case BUTTON_TORCH:
//                initializeTorchButton(button, cb, preCb, R.array.video_flashmode_icons);
                break;
            case BUTTON_HDR_PLUS_FLASH:
//                initializeHdrPlusFlashButton(button, cb, preCb, R.array.camera_flashmode_icons);
                break;
            case BUTTON_CAMERA:
                initializeCameraButton(button, cb, preCb, R.array.camera_id_icons);
                break;
            case BUTTON_HDR_PLUS:
//                initializeHdrPlusButton(button, cb, preCb, R.array.pref_camera_hdr_plus_icons);
                break;
            case BUTTON_HDR:
//                initializeHdrButton(button, cb, preCb, R.array.pref_camera_hdr_icons);
                break;
            case BUTTON_GRID_LINES:
//                initializeGridLinesButton(button, cb, preCb, R.array.grid_lines_icons);
                break;
            case BUTTON_COUNTDOWN:
//                initializeCountdownButton(button, cb, preCb, R.array.countdown_duration_icons);
                break;
            default:
                throw new IllegalArgumentException("button not known by id=" + buttonId);
        }
    }

    public void getButtonsReferences(View root) {
//        mButtonCamera
//                = (MultiToggleImageButton) root.findViewById(R.id.camera_switch);
//        mButtonFlash
//                = (MultiToggleImageButton) root.findViewById(R.id.flash_toggle_button);
//        mButtonHdr
//                = (MultiToggleImageButton) root.findViewById(R.id.hdr_plus_toggle_button);
//        mButtonGridlines
//                = (MultiToggleImageButton) root.findViewById(R.id.grid_lines_toggle_button);

    }


    private MultiToggleImageButton getButtonOrError(int buttonId) {
        switch (buttonId) {
            case BUTTON_FLASH:
                if (mButtonFlash == null) {
                    throw new IllegalStateException("Flash button could not be found.");
                }
                return mButtonFlash;
            case BUTTON_TORCH:
                if (mButtonFlash == null) {
                    throw new IllegalStateException("Torch button could not be found.");
                }
                return mButtonFlash;
            case BUTTON_HDR_PLUS_FLASH:
                if (mButtonFlash == null) {
                    throw new IllegalStateException("Hdr plus torch button could not be found.");
                }
                return mButtonFlash;
            case BUTTON_CAMERA:
                if (mButtonCamera == null) {
                    throw new IllegalStateException("Camera button could not be found.");
                }
                return mButtonCamera;
            case BUTTON_HDR_PLUS:
                if (mButtonHdr == null) {
                    throw new IllegalStateException("Hdr plus button could not be found.");
                }
                return mButtonHdr;
            case BUTTON_HDR:
                if (mButtonHdr == null) {
                    throw new IllegalStateException("Hdr button could not be found.");
                }
                return mButtonHdr;
            case BUTTON_GRID_LINES:
                if (mButtonGridlines == null) {
                    throw new IllegalStateException("Grid lines button could not be found.");
                }
                return mButtonGridlines;
            case BUTTON_COUNTDOWN:
                if (mButtonCountdown == null) {
                    throw new IllegalStateException("Countdown button could not be found.");
                }
                return mButtonCountdown;
            default:
                throw new IllegalArgumentException("button not known by id=" + buttonId);
        }    }

    private void initializeCameraButton(final MultiToggleImageButton button,
                                        final ButtonCallback cb, final ButtonCallback preCb, int resIdImages) {

//        if (resIdImages > 0) {
//            button.overrideImageIds(resIdImages);
//        }
//
//        int index = mSettingsManager.getIndexOfCurrentValue(mActivity.getModuleScope(),
//                Keys.KEY_CAMERA_ID);
//        button.setState(index >= 0 ? index : 0, false);

        setPreChangeCallback(button, preCb);

        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
//                mSettingsManager.setValueByIndex(mAppController.getModuleScope(),
//                        Keys.KEY_CAMERA_ID, state);
//                int cameraId = mSettingsManager.getInteger(mActivity.getModuleScope(),
//                        Keys.KEY_CAMERA_ID);
//                // This is a quick fix for ISE in Gcam module which can be
//                // found by rapid pressing camera switch button. The assumption
//                // here is that each time this button is clicked, the listener
//                // will do something and then enable this button again.
//                button.setEnabled(false);
//                if (cb != null) {
//                    cb.onStateChanged(cameraId);
//                }
//                mActivity.getCameraAppUI().onChangeCamera();
            }
        });
    }

    private void setPreChangeCallback(MultiToggleImageButton button, final ButtonCallback preCb) {
        button.setOnPreChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                if(preCb != null) {
                    preCb.onStateChanged(state);
                }
            }
        });
    }

}
