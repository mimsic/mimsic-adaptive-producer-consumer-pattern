package com.github.mimsic.pcp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoggerUtil {

    private static final Map<Class<?>, Logger> LOGGERS = new ConcurrentHashMap<>();

    public static void debug(Class<?> clazz, String format, Object... objects) {
        LOGGERS.computeIfAbsent(clazz, LoggerFactory::getLogger).debug(format, objects);
    }

    public static void error(Class<?> clazz, String msg) {
        LOGGERS.computeIfAbsent(clazz, LoggerFactory::getLogger).error(msg);
    }

    public static void error(Class<?> clazz, String msg, Throwable throwable) {
        LOGGERS.computeIfAbsent(clazz, LoggerFactory::getLogger).error(msg, throwable);
    }

    public static void info(Class<?> clazz, String format, Object... objects) {
        LOGGERS.computeIfAbsent(clazz, LoggerFactory::getLogger).info(format, objects);
    }
}
