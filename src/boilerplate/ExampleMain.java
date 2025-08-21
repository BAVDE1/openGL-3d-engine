package boilerplate;

import boilerplate.common.BoilerplateConstants;

import boilerplate.example.Example2d;
import boilerplate.example.Example3d;
import boilerplate.example.ExampleIndex;
import boilerplate.utility.Logging;

public class ExampleMain {
    public static void main(String[] args) {
        System.setProperty("joml.format", "false");  // stop it
        Logging.setupFileLogging(BoilerplateConstants.LOGGING_FILE_NAME);
        Logging.mystical("Starting example program");
        new Example3d().start();
    }
}
