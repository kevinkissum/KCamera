package com.example.kevin.kcamera.Ex;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;


import java.util.LinkedList;
import java.util.Queue;

public class DispatchThread extends Thread {
    private static final String TAG = "CAM_DispatchThread";
    private static final long MAX_MESSAGE_QUEUE_LENGTH = 256;

    private final Queue<Runnable> mJobQueue;
    private Boolean mIsEnded;
    private Handler mCameraHandler;
    private HandlerThread mCameraHandlerThread;

    //SPRD Bug:508634,add wait time 2500ms
    private static final long VAL_WAIT_DONE_RELEASE_TIMEOUT = 2500L;

    public DispatchThread(Handler cameraHandler, HandlerThread cameraHandlerThread) {
        super("Camera Job Dispatch Thread");
        mJobQueue = new LinkedList<Runnable>();
        mIsEnded = new Boolean(false);
        mCameraHandler = cameraHandler;
        mCameraHandlerThread = cameraHandlerThread;
    }

    public void setHandler(Handler handler) {
        mCameraHandler = handler;
    }

    /**
     * Queues up the job.
     *
     * @param job The job to run.
     */
    public void runJob(Runnable job) {
        if (isEnded()) {
            throw new IllegalStateException(
                    "Trying to run job on interrupted dispatcher thread");
        }
        synchronized (mJobQueue) {
            if (mJobQueue.size() == MAX_MESSAGE_QUEUE_LENGTH) {
                throw new RuntimeException("Camera master thread job queue full");
            }

            mJobQueue.add(job);
            mJobQueue.notifyAll();
        }
    }

    /**
     * Queues up the job and wait for it to be done.
     *
     * @param job The job to run.
     * @param timeoutMs Timeout limit in milliseconds.
     * @param jobMsg The message to log when the job runs timeout.
     * @return Whether the job finishes before timeout.
     */
    public void runJobSync(final Runnable job, Object waitLock, long timeoutMs, String jobMsg) {
        String timeoutMsg = "Timeout waiting " + timeoutMs + "ms for " + jobMsg;
        synchronized (waitLock) {
            long timeoutBound = SystemClock.uptimeMillis() + timeoutMs;
            try {
                runJob(job);
                waitLock.wait(timeoutMs);
                if (SystemClock.uptimeMillis() > timeoutBound) {
                    throw new IllegalStateException(timeoutMsg);
                }
            } catch (InterruptedException ex) {
                if (SystemClock.uptimeMillis() > timeoutBound) {
                    throw new IllegalStateException(timeoutMsg);
                }
            }
        }
    }

    /**
     * Gracefully ends this thread. Will stop after all jobs are processed.
     */
    public void end() {
        synchronized (mIsEnded) {
            mIsEnded = true;
        }
        synchronized(mJobQueue) {
            mJobQueue.notifyAll();
        }
    }

    private boolean isEnded() {
        synchronized (mIsEnded) {
            return mIsEnded;
        }
    }

    @Override
    public void run() {
        while(true) {
            Runnable job = null;
            synchronized (mJobQueue) {
                while (mJobQueue.size() == 0 && !isEnded()) {
                    try {
                        mJobQueue.wait();
                    } catch (InterruptedException ex) {
                        Log.w(TAG, "Dispatcher thread wait() interrupted, exiting");
                        break;
                    }
                }

                job = mJobQueue.poll();
            }

            if (job == null) {
                // mJobQueue.poll() returning null means wait() is
                // interrupted and the queue is empty.
                if (isEnded()) {
                    break;
                }
                continue;
            }

            job.run();

            // SPRD Bug:508634,add wait time 2500ms
            Log.i(TAG, "Runnable job.run() end!");

            synchronized (DispatchThread.this) {
                mCameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (DispatchThread.this) {
                            DispatchThread.this.notifyAll();

                            // SPRD Bug:508634,add wait time 2500ms
                            Log.i(TAG, "DispatchThread.this.notifyAll()!");
                        }
                    }
                });
                try {
                    /*
                     * SPRD Bug:508634,add wait time 2500ms @{
                     * Original Android code:
                    DispatchThread.this.wait();
                     */
                    Log.i(TAG, "DispatchThread.this.wait will!");
                    DispatchThread.this.wait(VAL_WAIT_DONE_RELEASE_TIMEOUT);// SPRD:Fix bug 508634
                    Log.i(TAG, "DispatchThread.this.wait end!");
                    /* @} */
                } catch (InterruptedException ex) {
                    // TODO: do something here.
                }
            }
        }
        mCameraHandlerThread.quitSafely();
    }
}
