package org.sRandomRTP.DifferentMethods;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerUtility {
    public static void loggerUtility(String callingClassName, Throwable throwable) {
        try {
            Class<?> clazz = Class.forName(callingClassName);
            String logFileName = clazz.getSimpleName() + ".yml";
            File logFolder = new File(Variables.getInstance().getDataFolder(), "LogsErrors");
            if (!logFolder.exists()) {
                logFolder.mkdirs();
            }
            File logFile = new File(logFolder, logFileName);
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss | dd.MM.yyyy");
                String timestamp = sdf.format(new Date());
                writer.println("[" + timestamp + "] " + throwable.toString());
                throwable.printStackTrace(writer);
                System.err.println("Error in class: " + clazz.getName() + ". Error logged to file: " + logFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
