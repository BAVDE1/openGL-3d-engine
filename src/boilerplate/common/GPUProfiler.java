package boilerplate.common;

import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;

import java.awt.desktop.AboutEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class GPUProfiler {
    public static class Frame {
        public int endQuery;
        public int frameNum;
        public final ArrayList<Log> logs = new ArrayList<>();
    }

    public static class Log {
        public int startQuery;
        public int endQuery;
        public double msTime;
        public String name;

        Log parent = null;
        ArrayList<Log> children = new ArrayList<>();
        int depth = 0;
    }

    public static class LogAverages {
        double[] averages = new double[AVERAGE_HISTORY];
        int depth = 0;
    }

    public static int AVERAGE_HISTORY = 50;

    private static int frameCount = 0;

    // fps tracking
    private static int frameCountStart = 0;
    private static double fpsCountStartTime = -1;
    private static int fpsCount = 0;

    private static Frame currentFrame = null;
    private static Log currentLog = null;

    private static final HashMap<Integer, Frame> frames = new HashMap<>();
    private static final LinkedHashMap<String, LogAverages> logAverages = new LinkedHashMap<>();

    private static String whitespace = "";

    private static double calcLogAverage(Log log) {
        logAverages.computeIfAbsent(log.name, _ -> {
            LogAverages newLa = new LogAverages();
            newLa.depth = log.depth;
            return newLa;
        });

        LogAverages la = GPUProfiler.logAverages.get(log.name);
        la.averages[frameCount % AVERAGE_HISTORY] = log.msTime;
        return Arrays.stream(la.averages).sum() / AVERAGE_HISTORY;
    }

    private static double getTimeFromQueries(int startQuery, int endQuery) {
        long startTime = GL45.glGetQueryObjectui64(startQuery, GL45.GL_QUERY_RESULT);
        long endTime = GL45.glGetQueryObjectui64(endQuery, GL45.GL_QUERY_RESULT);
        double time = endTime - startTime;
        return time < 1000 ? 0 : time / 1e6;  // 1000 nanoseconds is too fast, nothing happened (probably)
    }

    private static void updateFPSCount() {
        if (fpsCountStartTime == -1) fpsCountStartTime = glfwGetTime();
        else if (glfwGetTime() - fpsCountStartTime >= 1) {  // a full second has passed
            fpsCount = frameCount - frameCountStart;
            frameCountStart = frameCount;
            fpsCountStartTime = -1;
        }
    }

    public static void dumpAllLogs() {
        dumpAllLogs(false);
    }

    public static double dumpLog(boolean silent, Log log) {
        log.msTime = getTimeFromQueries(log.startQuery, log.endQuery);
        String space = whitespace.substring(log.name.length());
        double avg = calcLogAverage(log);  // still need to call this even if silent
        if (!silent) Logging.info("%s%s:%s %.2fms (avg: %.2f)", "\t".repeat(log.depth), log.name, space, log.msTime, avg);
        double total = log.msTime;
        for (Log child : log.children) total += dumpLog(silent, child);
        return total;
    }

    public static void dumpAllLogs(boolean silent) {
        ArrayList<Integer> framesFinished = new ArrayList<>();
        for (Frame frame : frames.values()) {
            if (GL45.glGetQueryObjectui(frame.endQuery, GL45.GL_QUERY_RESULT_AVAILABLE) != GL45.GL_TRUE) continue;
            framesFinished.add(frame.frameNum);
            if (!silent) Logging.mystical(">>> Frame %s", frame.frameNum);

            double total = 0;
            for (Log log : frame.logs) total += dumpLog(silent, log);

            if (!silent) {
                Logging.debug("total: %.3f", total);
                Logging.mystical("End logs");
            }
        }
        for (int key : framesFinished) frames.remove(key);
    }

    public static void dumpAllAverages() {
        Logging.mystical(">>> All Averages (from last %s frames)", AVERAGE_HISTORY);
        double total = 0;
        for (String logName : logAverages.keySet()) {
            String space = whitespace.substring(logName.length());
            double avg = Arrays.stream(logAverages.get(logName).averages).sum() / AVERAGE_HISTORY;
            total += avg;
            Logging.info("%s%s:%s %.5fms", "\t".repeat(logAverages.get(logName).depth), logName, space, avg);
        }
        Logging.debug("avg total: %.3f", total);
        Logging.mystical("End averages");
    }

    public static void dumpFps() {
        Logging.mystical(">>> FPS for the last full second of runtime: %s", fpsCount);
    }

    public static int getFps() {
        return fpsCount;
    }

    public static void startFrame() {
        frameCount += 1;
        currentLog = null;
        currentFrame = new Frame();
        currentFrame.frameNum = frameCount;
        updateFPSCount();
    }

    public static void startLog(String logName) {
        Log newLog = new Log();
        newLog.name = logName;
        newLog.parent = currentLog;
        newLog.startQuery = GL45.glGenQueries();
        GL45.glQueryCounter(newLog.startQuery, GL45.GL_TIMESTAMP);

        if (currentLog != null) {
            newLog.depth = currentLog.depth + 1;
            currentLog.children.add(newLog);
        }
        currentLog = newLog;

        if (logName.length() + newLog.depth > whitespace.length()) {
            whitespace = "";
            for (char _ : logName.toCharArray()) whitespace = whitespace.concat(" ");
        }
    }

    public static void endLog() {
        currentLog.endQuery = GL45.glGenQueries();
        GL45.glQueryCounter(currentLog.endQuery, GL45.GL_TIMESTAMP);

        if (currentLog.parent == null) currentFrame.logs.add(currentLog);
        currentLog = currentLog.parent;
    }

    public static void endFrame() {
        currentFrame.endQuery = GL45.glGenQueries();
        GL45.glQueryCounter(currentFrame.endQuery, GL45.GL_TIMESTAMP);

        frames.put(currentFrame.frameNum, currentFrame);
        currentFrame = null;
    }
}
