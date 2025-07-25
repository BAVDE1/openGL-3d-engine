package boilerplate.common;

import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class GPUProfiler {
    private static class Frame {
        public int endQuery;
        public int frameNum;
        public final ArrayList<Log> logs = new ArrayList<>();
    }

    private static class Log {
        public int startQuery;
        public int endQuery;
        public double msTime;
        public String name;
    }

    private static final int AVERAGE_HISTORY = 50;

    private static int frameCount = 0;
    private static Frame currentFrame;
    private static Log currentLog;

    private static final HashMap<Integer, Frame> frames = new HashMap<>();
    private static final HashMap<String, double[]> logAverages = new HashMap<>();

    private static String whitespace = "";

    public static void startFrame() {
        frameCount += 1;
        currentLog = null;
        currentFrame = new Frame();
        currentFrame.frameNum = frameCount;
    }

    private static double calcLogAverage(Log log) {
        if (!logAverages.containsKey(log.name)) logAverages.put(log.name, new double[AVERAGE_HISTORY]);
        double[] logAverages = GPUProfiler.logAverages.get(log.name);
        logAverages[frameCount % AVERAGE_HISTORY] = log.msTime;
        return Arrays.stream(logAverages).sum() / AVERAGE_HISTORY;
    }

    private static double getTimeFromQueries(int startQuery, int endQuery) {
        long startTime = GL45.glGetQueryObjectui64(startQuery, GL45.GL_QUERY_RESULT);
        long endTime = GL45.glGetQueryObjectui64(endQuery, GL45.GL_QUERY_RESULT);
        double time = endTime - startTime;
        return time < 1000 ? 0 : time / 1e6;  // 1000 nanoseconds is too fast, nothing probably happened
    }

    public static void dumpAllLogs() {
        dumpAllLogs(false);
    }

    public static void dumpAllLogs(boolean silent) {
        ArrayList<Integer> framesFinished = new ArrayList<>();
        for (Frame frame : frames.values()) {
            if (GL45.glGetQueryObjectui(frame.endQuery, GL45.GL_QUERY_RESULT_AVAILABLE) != GL45.GL_TRUE) continue;
            framesFinished.add(frame.frameNum);
            if (silent) continue;

            Logging.mystical(">>> Frame %s", frame.frameNum);

            double total = 0;
            for (Log log : frame.logs) {
                log.msTime = getTimeFromQueries(log.startQuery, log.endQuery);
                String space = whitespace.substring(log.name.length());
                Logging.info("%s:%s %.2fms (avg: %.2f)", log.name, space, log.msTime, calcLogAverage(log));
                total += log.msTime;
            }
            Logging.debug("total: %.3f", total);
            Logging.mystical("End logs");
        }
        for (int key : framesFinished) frames.remove(key);
    }

    public static void startLog(String logName) {
        currentLog = new Log();
        currentLog.name = logName;
        currentLog.startQuery = GL45.glGenQueries();
        GL45.glQueryCounter(currentLog.startQuery, GL45.GL_TIMESTAMP);

        if (logName.length() > whitespace.length()) {
            whitespace = "";
            for (char _ : logName.toCharArray()) whitespace = whitespace.concat(" ");
        }
    }

    public static void endLog() {
        currentLog.endQuery = GL45.glGenQueries();
        GL45.glQueryCounter(currentLog.endQuery, GL45.GL_TIMESTAMP);

        currentFrame.logs.add(currentLog);
        currentLog = null;
    }

    public static void endFrame() {
        currentFrame.endQuery = GL45.glGenQueries();
        GL45.glQueryCounter(currentFrame.endQuery, GL45.GL_TIMESTAMP);

        frames.put(currentFrame.frameNum, currentFrame);
        currentFrame = null;
    }
}
