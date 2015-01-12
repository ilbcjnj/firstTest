package com.hesine.nmsg.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public final class MLog {
    public static final int LOG_DEBUG = 0;
    public static final int LOG_INFO = 1;
    public static final int LOG_ERROR = 2;
    public static final int LOG_NONE_LOG = 3;
    private static int logLevel = LOG_DEBUG;
    public static File file = null;
    public static RandomAccessFile fWriter = null;
    public static final String LOG_PATH = File.separator + EnumConstants.ROOT_DIR + "/Log/";

    private static final int MAX_LOG_SIZE = 10 * 1024 * 1024;
    private static final String TAG = "Mlog";

    public static synchronized void init() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                String newLog = FileEx.getSDCardPath() + LOG_PATH + "nmsg.log";
                String bakLog = FileEx.getSDCardPath() + LOG_PATH + "nmsg-bak.log";
                file = new File(newLog);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                boolean isFileExist = file.exists();
                if (isFileExist && file.length() >= MAX_LOG_SIZE) {
                    File fileBak = new File(bakLog);
                    if (fileBak.exists()) {
                        fileBak.delete();
                    }
                    file.renameTo(fileBak);
                    file = null;
                    file = new File(newLog);
                    isFileExist = false;
                }

                if (fWriter != null) {
                    fWriter.close();
                }

                fWriter = new RandomAccessFile(file, "rws");
                if (isFileExist) {
                    fWriter.seek(file.length());
                }

                error("NmsLog", "java file logger is inited");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void destroy() {
        if (fWriter != null) {
            try {
                fWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fWriter = null;
            }
        }
        file = null;
    }

    public static String getStactTrace(Exception e) {
        if (null == e) {
            return null;
        }
        StringBuffer ret = new StringBuffer(e.toString());
        StackTraceElement[] stack = e.getStackTrace();
        for (int i = 0; stack != null && i < stack.length; ++i) {
            ret = ret.append("\n" + stack[i].toString());
        }
        return ret.toString();
    }

    public static void printStackTrace(Exception e) {
        if (null == e && (logLevel > LOG_ERROR)) {
            return;
        }

        error(EnumConstants.NMSG_TAG_GLOBAL, "Exception: " + e.toString());
        StackTraceElement[] stack = e.getStackTrace();
        for (int i = 0; stack != null && i < stack.length; ++i) {
            error(EnumConstants.NMSG_TAG_GLOBAL, stack[i].toString());
        }
    }

    public static void error(String tag, String msg) {
        if (logLevel <= LOG_ERROR) {
            Log.e(tag," ERROR " + "Line: " + Thread.currentThread().getStackTrace()[3].getLineNumber() + "  " + msg);
            appendLog(file," ERROR "+  tag + "\t" + "Line: " 
                    +Thread.currentThread().getStackTrace()[3].getLineNumber() + "  "
                    + msg, 0);
        }
    }

    public static void info(String tag, String msg) {
        if (logLevel <= LOG_INFO) {
            Log.i(tag, " INFO " +"Line " + Thread.currentThread().getStackTrace()[3].getLineNumber() + "  " + msg);
            appendLog(file," INFO " + tag + "\t" + "Line: " 
                     + Thread.currentThread().getStackTrace()[3].getLineNumber() + "  "
                    + msg, 1);
        }
    }

    public static void debug(String tag, String msg) {
        if (logLevel <= LOG_DEBUG) {
            Log.w(tag, " DEBUG "+"Line " + Thread.currentThread().getStackTrace()[3].getLineNumber() + "  " + msg);
            appendLog(file, " DEBUG " +tag + "\t" + "Line " 
                    + Thread.currentThread().getStackTrace()[3].getLineNumber() + "  "
                    + msg, 2);
        }
    }

    public static void appendLog(File file, String content, int level) {
        try {
            if (file == null || !file.exists()) {
                init();
                return;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(EnumConstants.SDF2.format(new Date()));
            sb.append("\t ");
            sb.append(level == 1 ? "i" : level == 2 ? "w" : "e");
            sb.append("\t");
            sb.append(content);
            sb.append("\r\n");
            fWriter.write(sb.toString().getBytes());
        } catch (IOException  e) {
            Log.e(EnumConstants.NMSG_TAG_GLOBAL,
                    "log output exception,maybe the log file is not exists," + getStactTrace(e));
        } finally {

            if (file != null && file.length() >= MAX_LOG_SIZE) {
                init();
                return;
            }
        }
    }

    static void setLogPriority(int priority) {
        logLevel = priority;
    }

    public static int getLogPriority() {
        return logLevel;
    }

    public static void readIniFile() {
        String filePath = FileEx.getSDCardPath() + File.separator + "nmsg.ini";
        File file = new File(filePath);
        String logLevelString = null;
        if (!file.exists()) {
            return;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                if (tempString.indexOf("LOG_LEVEL") != -1) {
                    String[] str = tempString.split("=");
                    logLevelString = str[1];
                    logLevelString = logLevelString.replaceAll("\\s", "");
                    logLevelString = logLevelString.replaceAll(";", "");
                    int logLevel = Integer.parseInt(logLevelString);
                    MLog.error(TAG, "log was changed before loglevel is:" + getLogPriority());
                    setLogPriority(logLevel);
                    MLog.error(TAG, "log was changed current loglevel is:" + logLevel);
                    break;
                }
            }
            if (null != reader) {
                reader.close();
            }
        } catch (NumberFormatException  e) {
            e.printStackTrace();
        } catch (IOException e) {          
            e.printStackTrace();
        }
    }
}
