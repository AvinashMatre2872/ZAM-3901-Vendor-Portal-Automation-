package com.zamplia.utils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class DownloadUtils {

    private DownloadUtils() { }

    public static Set<String> snapshot(File dir) {
        String[] names = dir.list();
        return names == null ? new HashSet<>() : new HashSet<>(Arrays.asList(names));
    }

    /** Waits for a file that was not in {@code before} and has finished downloading. Returns null on timeout. */
    public static File waitForNewFile(File dir, Set<String> before, int timeoutSeconds) {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < end) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String n = f.getName();
                    boolean partial = n.endsWith(".crdownload") || n.endsWith(".part") || n.endsWith(".tmp");
                    if (!before.contains(n) && !partial && f.length() > 0) return f;
                }
            }
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return null;
    }
}
