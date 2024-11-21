package com.termux.app;

import android.app.Activity;
import android.system.Os;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** @noinspection unchecked*/
public class TermuxInstaller {
    private static final String LOG_TAG = "termux-installer";
    private static String TERMUX_PREFIX_DIR_PATH = null;

    public static Object setupBootstrapIfNeeded(final Activity activity) {
        if (TERMUX_PREFIX_DIR_PATH == null) {
            TERMUX_PREFIX_DIR_PATH = activity.getFilesDir().getAbsolutePath();
        }
        try {
            Log.d(LOG_TAG, "Installing Termux bootstrap packages.");
            Log.d(LOG_TAG, "Extracting bootstrap zip to prefix staging directory \"" + TERMUX_PREFIX_DIR_PATH + "\".");

            final byte[] buffer = new byte[8096];
            final List<Pair<String, String>> symlinks = new ArrayList<>(50);

            final byte[] zipBytes = loadZipBytes();
            try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry zipEntry;
                while ((zipEntry = zipInput.getNextEntry()) != null) {
                    if (zipEntry.getName().equals("SYMLINKS.txt")) {
                        BufferedReader symlinksReader = new BufferedReader(new InputStreamReader(zipInput));
                        String line;
                        while ((line = symlinksReader.readLine()) != null) {
                            String[] parts = line.split("←");
                            if (parts.length != 2)
                                throw new RuntimeException("Malformed symlink line: " + line);
                            String oldPath = parts[0];
                            String newPath = TERMUX_PREFIX_DIR_PATH + "/" + parts[1];
                            symlinks.add(Pair.create(oldPath, newPath));

                            if (ensureDirectoryExists(Objects.requireNonNull(new File(newPath).getParentFile()))) {
                                return new RuntimeException("Parent directory of symlink does not exist: " + newPath);
                            }
                        }
                    } else {
                        String zipEntryName = zipEntry.getName();
                        File targetFile = new File(TERMUX_PREFIX_DIR_PATH, zipEntryName);
                        boolean isDirectory = zipEntry.isDirectory();

                        Log.d(LOG_TAG, "Extracting " + (isDirectory ? "directory" : "file") + " \"" + zipEntryName + "\" to \"" + targetFile + "\".");

                        if (ensureDirectoryExists(Objects.requireNonNull(isDirectory ? targetFile : targetFile.getParentFile()))) {
                            return new RuntimeException("Parent directory of file does not exist: " + targetFile);
                        }

                        if (!isDirectory) {
                            try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                                int readBytes;
                                while ((readBytes = zipInput.read(buffer)) != -1)
                                    outStream.write(buffer, 0, readBytes);
                            }
                            if (zipEntryName.startsWith("bin/") || zipEntryName.startsWith("libexec") ||
                                    zipEntryName.startsWith("lib/apt/apt-helper") || zipEntryName.startsWith("lib/apt/methods")) {
                                //noinspection OctalInteger
                                Os.chmod(targetFile.getAbsolutePath(), 0700);
                            }
                        }
                    }
                }
            }

            if (symlinks.isEmpty())
                throw new RuntimeException("No SYMLINKS.txt encountered");
            for (Pair<String, String> symlink : symlinks) {
                Os.symlink(symlink.first, symlink.second);
            }

            Log.d(LOG_TAG, "Moving termux prefix staging to prefix directory.");

            Log.d(LOG_TAG, "Bootstrap packages installed successfully.");
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Error installing Termux bootstrap packages", e);
            return new RuntimeException(e);
        }
        return true;
    }

    public static boolean ensureDirectoryExists(File directory) {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                if (!directory.delete() || !directory.mkdirs()) {
                    Log.e(LOG_TAG, "Failed to create directory: " + directory);
                    return true;
                }
            }
            return false;
        } else {
            if (!directory.mkdirs()) {
                Log.e(LOG_TAG, "Failed to create directory: " + directory);
                return true;
            }
        }
        return false;
    }

    public static byte[] loadZipBytes() {
        // Only load the shared library when necessary to save memory usage.
        System.loadLibrary("termux-bootstrap");
        return getZip();
    }

    public static native byte[] getZip();
}
