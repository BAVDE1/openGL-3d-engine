package boilerplate.common;

import boilerplate.utility.Logging;
import boilerplate.utility.MathUtils;

public class TimeStepper {
    /**
     * Proper time stepper.
     * if game has optimised boolean toggled, thread sleeps for half of dt once stepped.
     */
    public static void startStaticTimeStepper(double staticDeltaTime, GameBase game) {
        startStaticTimeStepper(staticDeltaTime, game, BoilerplateConstants.OPTIMIZE_TIME_STEPPER);
    }

    public static void startStaticTimeStepper(double staticDeltaTime, GameBase game, Boolean tryOptimise) {
        final double staticDeltaTimeN = MathUtils.secondToNano(staticDeltaTime);
        final double halfDtN = staticDeltaTimeN * 0.5;
        final double minFPS = MathUtils.secondToNano(1);  // min of 1 fps
        double accumulator = 0;

        game.createCapabilitiesAndOpen();
        Logging.debug("Starting static time stepper with a dt of %.6f secs (%4fms)", staticDeltaTime, MathUtils.secondToMillis(staticDeltaTime));

        double lastFrame = System.nanoTime();

        while (!game.shouldClose()) {
            double t = System.nanoTime();
            accumulator += t - lastFrame;
            accumulator = Math.min(minFPS, accumulator);
            lastFrame = t;

            while (accumulator >= staticDeltaTimeN) {
                accumulator -= staticDeltaTimeN;

                try {
                    double tStart = System.nanoTime();
                    game.mainLoop(staticDeltaTime);
                    double loopTime = System.nanoTime() - tStart;
                    if (tryOptimise && accumulator + loopTime < halfDtN) {  // only sleep if there is enough time
                        Thread.sleep((long) Math.floor(MathUtils.nanoToMillis(halfDtN)));  // give it a little break *-*
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Program closed while thread was asleep (between frames)");
                }
            }
        }
        game.close();
    }

    public static void startSleepingTimeStepper(double targetDeltaTime, GameBase game) {
        game.createCapabilitiesAndOpen();
        Logging.debug("Starting sleeping time stepper with a target dt of %s", targetDeltaTime);

        while (!game.shouldClose()) {
            try {
                double t = System.nanoTime();
                Thread.sleep((long) Math.floor(MathUtils.secondToMillis(targetDeltaTime)));
                game.mainLoop(MathUtils.nanoToSecond(System.nanoTime() - t));
            } catch (InterruptedException e) {
                throw new RuntimeException("Program closed while thread was asleep (between frames)");
            }
        }
        game.close();
    }
}
