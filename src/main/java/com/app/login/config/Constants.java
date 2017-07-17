package com.app.login.config;

public final class Constants {

    public static final String LOGIN_REGEX = "^[_'.@A-Za-z0-9-]*$";
    public static final String BEARER = "Bearer ";
    public static final Long TOKEN_VALIDITY_IN_MILLISECONDS = 1800000L;
    public static final Long TOKEN_VALIDITY_IN_MILLISECONDS_FOR_REMEMBER_ME = 604800000L;
    public static final String DEFAULT_LANGUAGE_KEY = "en";
    public static final Integer DOS_MAX_REGISTRATION_ALLOWED = 3;
    private static final String NEWLINE = "\n----------------------------------------------------------\n\t";
    private static final String APP_MSG = "Application '{}' is running! Access URLs:\n\t";
    private static final String EXT = "External: \t{}://{}:{}\n\t";
    public static final String STARTUP_LOG_MSG = NEWLINE + APP_MSG + EXT;

    private Constants() {
    }
}
