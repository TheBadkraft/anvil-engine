package dev.badkraft.anvil.utilities;

import java.nio.file.Path;

public class Utils {

    // Get file extension from Path
    public static String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return ""; // No extension found
        }
        return fileName.substring(lastDotIndex + 1);
    }
}
