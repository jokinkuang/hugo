package hugo.weaving.io;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.PrintStream;

import hugo.weaving.internal.Constants;

/*
 */
public class IOHelper {
    public static void init(String filePath) {
        try {
            System.setOut(new PrintStream(new MultiOutputStream(System.out, filePath)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(Constants.TAG,  "trace file path:"+filePath);
    }
}
