package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryScanner {
    public static List<String> scanDirectory(File directory, String[] extensions) {
        List<String> files = new ArrayList<>();
        if (directory.exists() && directory.isDirectory()) {
            scanDirectoryRecursive(directory, files, extensions);
        }
        return files;
    }

    private static void scanDirectoryRecursive(File directory, List<String> files, String[] extensions) {
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    scanDirectoryRecursive(file, files, extensions);
                } else {
                    String fileName = file.getName().toLowerCase();
                    for (String extension : extensions) {
                        if (fileName.endsWith(extension.toLowerCase())) {
                            files.add(file.getAbsolutePath());
                            break;
                        }
                    }
                }
            }
        }
    }
}