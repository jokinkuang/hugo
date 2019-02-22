package hugo.weaving.internal;

import android.os.Build;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hugo.weaving.io.IOHelper;
import hugo.weaving.io.MultiOutputStream;
import hugo.weaving.io.NLog;

@Aspect
public class Hugo {
  private static volatile boolean enabled = true;
  private static final String IFLOW = "within(com.example.hugo.*)";

  private static AtomicInteger i = new AtomicInteger(0);

  static {
    System.setOut(new PrintStream(new MultiOutputStream(System.out, "/sdcard/hugo.txt")));
  }

  @Pointcut("within(@hugo.weaving.DebugLog *)")
  public void withinAnnotatedClass() {}

  @Pointcut("execution(!synthetic * *(..)) && withinAnnotatedClass()")
  public void methodInsideAnnotatedType() {}

  @Pointcut("execution(!synthetic *.new(..)) && withinAnnotatedClass()")
  public void constructorInsideAnnotatedType() {}

  @Pointcut(IFLOW)
  public void method() {}

  @Pointcut("execution(@hugo.weaving.DebugLog *.new(..)) || constructorInsideAnnotatedType()")
  public void constructor() {}

  public static void setEnabled(boolean enabled) {
    Hugo.enabled = enabled;
  }

  public static void init(String logFilePath) {
    IOHelper.init(logFilePath);
  }

  public static void dumpStatistics() {
    Statistics.dump();
  }

  @Around("method() || constructor()")
  public Object logAndExecute(ProceedingJoinPoint joinPoint) throws Throwable {
    // NLog.e(Constants.TAG, "JoinPoint:"+joinPoint);
    i.addAndGet(1);
    // NLog.e(Constants.TAG, new String(new char[i.get()]).replace("\0", "-")
    //         +getLogPrefix(joinPoint)+"[start]");
    printN(joinPoint);
    enterMethod(joinPoint);

    long startNanos = System.nanoTime();
    Object result = joinPoint.proceed();
    long stopNanos = System.nanoTime();
    long lengthMillis = TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos);

    if (lengthMillis != 0) {
      NLog.e(Constants.TAG, new String(new char[i.get()]).replace("\0", "-")
              + getLogPrefix(joinPoint) + "[end]" + " " + lengthMillis + " ms");
    } else {
      // NLog.e(Constants.TAG, new String(new char[i.get()]).replace("\0", "-")
      //         + getLogPrefix(joinPoint) + "[end]");
    }
    i.addAndGet(-1);
    printN(joinPoint);
    exitMethod(joinPoint, result, lengthMillis);


    Statistics.add(new Statistics.MethodProfile(getLogPrefix(joinPoint)+"[end]", lengthMillis));

    return result;
  }

  private static String getLogPrefix(ProceedingJoinPoint joinPoint) {
    String kind = joinPoint.getStaticPart().getKind();
    Class<?> cls = joinPoint.getSignature().getDeclaringType();
    String methodName = joinPoint.getSignature().getName();
    String thread = Thread.currentThread().getName();
    return "->"+kind+"->"+thread +"->"+getClassName(cls) + "("+cls.hashCode()+")"+"->" + methodName;
  }

  private static void printN(ProceedingJoinPoint joinPoint) {
    Signature signature = joinPoint.getSignature();
    Class<?> cls = signature.getDeclaringType();
    String methodName = signature.getName();


    // Log.e(getClassName(cls), new String(new char[i]).replace("\0", "-") + methodName);
  }

  private static void enterMethod(JoinPoint joinPoint) {
    if (!enabled) return;

    Signature codeSignature = joinPoint.getSignature();

    Class<?> cls = codeSignature.getDeclaringType();
    String methodName = codeSignature.getName();
    String[] parameterNames = new String[0];
    Object[] parameterValues = new Object[0];

    if (joinPoint.getSignature() instanceof  MethodSignature) {
      parameterNames = ((MethodSignature) codeSignature).getParameterNames();
      parameterValues = joinPoint.getArgs();
    }

    StringBuilder builder = new StringBuilder("\u21E2 ");
    builder.append(methodName).append('(');
    for (int i = 0; i < parameterValues.length; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(parameterNames[i]).append('=');
      builder.append(Strings.toString(parameterValues[i]));
    }
    builder.append(')');

    if (Looper.myLooper() != Looper.getMainLooper()) {
      builder.append(" [Thread:\"").append(Thread.currentThread().getName()).append("\"]");
    }

    // Log.e(getClassName(cls), builder.toString());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      final String section = builder.toString().substring(2);
      Trace.beginSection(section);
    }
  }

  private static void exitMethod(JoinPoint joinPoint, Object result, long lengthMillis) {
    if (!enabled) return;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      Trace.endSection();
    }

    Signature signature = joinPoint.getSignature();

    Class<?> cls = signature.getDeclaringType();
    String methodName = signature.getName();
    boolean hasReturnType = signature instanceof MethodSignature
        && ((MethodSignature) signature).getReturnType() != void.class;

    StringBuilder builder = new StringBuilder("\u21E0 ")
        .append(getClassName(cls))
        .append(methodName)
        .append(" [")
        .append(lengthMillis)
        .append("ms]");

    if (hasReturnType) {
      builder.append(" = ");
      builder.append(Strings.toString(result));
    }

    // Log.e(Constants.TAG, builder.toString());
  }

  private static String getClassName(Class<?> cls) {
    if (cls.isAnonymousClass()) {
      return getClassName(cls.getEnclosingClass());
    }
    return cls.getSimpleName();
  }
}
