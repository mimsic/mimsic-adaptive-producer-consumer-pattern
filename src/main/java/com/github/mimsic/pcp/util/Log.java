package com.github.mimsic.pcp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Log {

    private static final Map<Class<?>, Logger> LOGGERS = new ConcurrentHashMap<>();

    public static void debug(Class<?> clazz, String format, Object... objects) {
        Optional.of(LOGGERS.computeIfAbsent(clazz, LoggerFactory::getLogger))
                .filter(Logger::isDebugEnabled)
                .ifPresent((Logger logger) -> logger.debug(format, objects));
    }

    public static void error(Class<?> clazz, String msg) {
        Optional.of(LOGGERS.computeIfAbsent(clazz, LoggerFactory::getLogger))
                .filter(Logger::isErrorEnabled)
                .ifPresent((Logger logger) -> logger.error(msg));
    }

    public static void error(Class<?> clazz, String msg, Throwable throwable) {
        Optional.of(LOGGERS.computeIfAbsent(clazz, LoggerFactory::getLogger))
                .filter(Logger::isErrorEnabled)
                .ifPresent((Logger logger) -> logger.error(msg, throwable));
    }

    public static void info(Class<?> clazz, String format, Object... objects) {
        Optional.of(LOGGERS.computeIfAbsent(clazz, LoggerFactory::getLogger))
                .filter(Logger::isInfoEnabled)
                .ifPresent((Logger logger) -> logger.info(format, objects));
    }
}
