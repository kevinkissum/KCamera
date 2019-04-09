package com.example.kevin.kcamera.Ex;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.LinkedList;

class HistoryHandler extends Handler {
    private static final int MAX_HISTORY_SIZE = 400;

    final LinkedList<Integer> mMsgHistory;

    HistoryHandler(Looper looper) {
        super(looper);
        mMsgHistory = new LinkedList<Integer>();
        // We add a -1 at the beginning to mark the very beginning of the
        // history.
        mMsgHistory.offerLast(-1);
    }

    Integer getCurrentMessage() {
        return mMsgHistory.peekLast();
    }

    String generateHistoryString(int cameraId) {
        String info = new String("HIST");
        info += "_ID" + cameraId;
        for (Integer msg : mMsgHistory) {
            info = info + '_' + msg.toString();
        }
        info += "_HEND";
        return info;
    }

    /**
     * Subclasses' implementations should call this one before doing their work.
     */
    @Override
    public void handleMessage(Message msg) {
        mMsgHistory.offerLast(msg.what);
        while (mMsgHistory.size() > MAX_HISTORY_SIZE) {
            mMsgHistory.pollFirst();
        }
    }
}
