package org.sRandomRTP.DifferentMethods;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerUtility {
    public static void loggerUtility(String callingClassName, Throwable throwable) {
        if (callingClassName == null) {
            loggerUtility(LoggerUtility.class, throwable);
            return;
        }
        try {
            Class<?> clazz = Class.forName(callingClassName);
            loggerUtility(clazz, throwable);
        } catch (ClassNotFoundException e) {
            Logger.getLogger(LoggerUtility.class.getName())
                    .log(Level.WARNING, "ClassNotFoundException while resolving logger class: " + callingClassName, e);
        }
    }

    public static void loggerUtility(Class<?> sourceClass, Throwable throwable) {
        if (sourceClass == null) {
            sourceClass = LoggerUtility.class;
        }
        if (throwable == null) {
            return;
        }
        Logger fallbackLogger = Logger.getLogger(sourceClass.getName());
        if (Variables.getInstance() == null) {
            fallbackLogger.log(Level.SEVERE, "Plugin instance is not available for structured error logging", throwable);
            return;
        }
        String logFileName = sourceClass.getSimpleName() + ".yml";
        File logFolder = new File(Variables.getInstance().getDataFolder(), "LogsErrors");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
        File logFile = new File(logFolder, logFileName);
        File latestLogFile = new File(logFolder, "latest-error.log");
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
            appendThrowable(writer, sourceClass, throwable);
        } catch (IOException e) {
            fallbackLogger.log(Level.SEVERE, "Failed to write class-specific error log", e);
        }
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(latestLogFile, false)))) {
            appendThrowable(writer, sourceClass, throwable);
            Variables.getInstance().getLogger().severe("Error in class: " + sourceClass.getName()
                    + ". Error logged to file: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            fallbackLogger.log(Level.SEVERE, "Failed to write latest error log", e);
        }
    }

    private static void appendThrowable(PrintWriter writer, Class<?> sourceClass, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss | dd.MM.yyyy"));
        writer.println("[" + timestamp + "] " + throwable.toString());
        writer.println("Source: " + sourceClass.getName());
        writer.println("Thread: " + Thread.currentThread().getName());
        Throwable cause = throwable.getCause();
        while (cause != null) {
            writer.println("Caused by: " + cause.toString());
            cause = cause.getCause();
        }
        throwable.printStackTrace(writer);
    }
}
