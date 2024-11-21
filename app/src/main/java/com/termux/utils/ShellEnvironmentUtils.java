package com.termux.utils;

import androidx.annotation.NonNull;

import java.util.HashMap;

public class ShellEnvironmentUtils {
    public static void putToEnvIfInSystemEnv(@NonNull HashMap<String, String> environment,
                                             @NonNull String name) {
        String value = System.getenv(name);
        if (value != null) {
            environment.put(name, value);
        }
    }
}
