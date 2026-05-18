package com.example.calculator;

import java.math.BigDecimal;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

/**
 * Mathservice 任务1-2要求的Service组件。
 * 接收Activity通过Intent传入的操作数a、b和操作类型op，
 * 内部分别启动 4 个子线程（AddThread/SubThread/MulThread/DivThread），
 * 每个子线程负责一种四则运算，运算结果通过 Handler（Messenger）机制
 * 返回给主线程（UI 线程）。同时使用 Log 打印当前 PID 与 TID。
 */
public class Mathservice extends Service {

    public static final String TAG = "Mathservice";

    public static final int OP_ADD = 1;
    public static final int OP_SUB = 2;
    public static final int OP_MUL = 3;
    public static final int OP_DIV = 4;

    public static final String EXTRA_A = "extra_a";
    public static final String EXTRA_B = "extra_b";
    public static final String EXTRA_OP = "extra_op";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_MESSENGER = "extra_messenger";

    public static final String DATA_RESULT = "data_result";
    public static final String DATA_ERROR = "data_error";
    public static final String DATA_OP = "data_op";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Mathservice onCreate"
                + " PID=" + Process.myPid()
                + " TID=" + Process.myTid()
                + " ThreadName=" + Thread.currentThread().getName());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        double a = intent.getDoubleExtra(EXTRA_A, 0);
        double b = intent.getDoubleExtra(EXTRA_B, 0);
        int requestedOp = intent.getIntExtra(EXTRA_OP, OP_ADD);
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, 0);
        Messenger replyTo = intent.getParcelableExtra(EXTRA_MESSENGER);

        Log.i(TAG, "Mathservice onStartCommand a=" + a + " b=" + b
                + " requestedOp=" + requestedOp + " taskId=" + taskId
                + " PID=" + Process.myPid()
                + " TID=" + Process.myTid()
                + " ThreadName=" + Thread.currentThread().getName());

        if (replyTo == null) {
            Log.w(TAG, "Mathservice: no replyTo Messenger, abort");
            return START_NOT_STICKY;
        }

        // 分别启动 4 个子线程，每个线程对应一种四则运算
        new Thread(new ComputeRunnable("AddThread", OP_ADD, a, b, taskId, replyTo)).start();
        new Thread(new ComputeRunnable("SubThread", OP_SUB, a, b, taskId, replyTo)).start();
        new Thread(new ComputeRunnable("MulThread", OP_MUL, a, b, taskId, replyTo)).start();
        new Thread(new ComputeRunnable("DivThread", OP_DIV, a, b, taskId, replyTo)).start();

        return START_NOT_STICKY;
    }

    private static class ComputeRunnable implements Runnable {
        private final String name;
        private final int op;
        private final double a;
        private final double b;
        private final int taskId;
        private final Messenger replyTo;

        ComputeRunnable(String name, int op, double a, double b, int taskId, Messenger replyTo) {
            this.name = name;
            this.op = op;
            this.a = a;
            this.b = b;
            this.taskId = taskId;
            this.replyTo = replyTo;
        }

        @Override
        public void run() {
            Thread.currentThread().setName(name);
            Log.i(TAG, name + " start"
                    + " PID=" + Process.myPid()
                    + " TID=" + Process.myTid()
                    + " ThreadName=" + Thread.currentThread().getName());

            double result = 0;
            boolean error = false;
            try {
                BigDecimal ba = new BigDecimal(Double.toString(a));
                BigDecimal bb = new BigDecimal(Double.toString(b));
                switch (op) {
                    case OP_ADD:
                        result = ba.add(bb).doubleValue();
                        break;
                    case OP_SUB:
                        result = ba.subtract(bb).doubleValue();
                        break;
                    case OP_MUL:
                        result = ba.multiply(bb).doubleValue();
                        break;
                    case OP_DIV:
                        if (b == 0) {
                            error = true;
                        } else {
                            result = ba.divide(bb, 10, BigDecimal.ROUND_HALF_UP).doubleValue();
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, name + " exception", e);
                error = true;
            }

            Log.i(TAG, name + " finish result=" + result + " error=" + error
                    + " PID=" + Process.myPid()
                    + " TID=" + Process.myTid());

            Message msg = Message.obtain();
            msg.what = op;
            msg.arg1 = taskId;
            Bundle data = new Bundle();
            data.putDouble(DATA_RESULT, result);
            data.putBoolean(DATA_ERROR, error);
            data.putInt(DATA_OP, op);
            msg.setData(data);
            try {
                replyTo.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, name + " send failed", e);
            }
        }
    }
}
