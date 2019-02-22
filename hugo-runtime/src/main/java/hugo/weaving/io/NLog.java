package hugo.weaving.io;

import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import hugo.weaving.internal.Constants;

public class NLog {
    private static final BlockingQueue<String> mMessageQueue = new LinkedBlockingQueue<>();
    static {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String msg = mMessageQueue.take();
                        Log.e(Constants.TAG, ">> take" + mMessageQueue.size() + " | " + msg);
                        log(msg);
                    } catch (InterruptedException e) {
                        Log.e(Constants.TAG, "=====================", e);
                    }
                }
            }
        }).start();
    }
    public static void e(String tag, String msg) {
        try {
            Log.e(Constants.TAG, ">> add" + mMessageQueue.size() + "|" + msg);
            mMessageQueue.put(tag + msg);
        } catch (InterruptedException e) {
            Log.e(Constants.TAG, "=====================", e);
        }
    }

    private static int i = 0;
    private static void log(String msg) {
        // System.out.println(msg);
        writeLog(msg);
        writeLog("========"+i+"========");
        i++;
        // Log.e(Constants.TAG, msg);
    }

    // 这里只解决了多线程，没有解决多进程访问了同一个文件！！！各个进程，各有一个主线程！！
    private static File sLogFile = new File("/sdcard/hugo"+ Process.myPid()+Thread.currentThread().getId()+".txt");
    private static void writeLog(String msg) {
        FileWriter fileWriter = null;
        File logFile = sLogFile;


        try {
            fileWriter = new FileWriter(logFile, true);

            Log.e(Constants.TAG, ">> wirte: " + msg);
            fileWriter.append(msg);
            fileWriter.append("\n");

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Log.e(Constants.TAG, "", e);
            if (fileWriter != null) {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e1) {
                    Log.e(Constants.TAG, "", e);
                }
            }
        }
    }
}
