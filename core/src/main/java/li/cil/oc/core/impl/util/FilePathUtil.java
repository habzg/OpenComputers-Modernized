package li.cil.oc.core.impl.util;

import li.cil.oc.core.impl.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;
import java.util.UUID;

public final class FilePathUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilePathUtil.class);
    private static final Set<Character> invalidChars = new java.util.HashSet<>();
    private static Boolean isCaseInsensitive_ = null;

    static {
        for (char c : "\\:*?\"<>|".toCharArray()) invalidChars.add(c);
    }

    private FilePathUtil() {
    }

    public static boolean isCaseInsensitive() {
        if (isCaseInsensitive_ == null) {
            isCaseInsensitive_ = Settings.get().forceCaseInsensitive || checkCaseInsensitive();
        }
        return isCaseInsensitive_;
    }

    private static boolean checkCaseInsensitive() {
        try {
            String uuid = UUID.randomUUID().toString();
            File lowerCase = File.createTempFile(uuid + "oc_rox", null);
            File upperCase = new File(lowerCase.getParentFile(), uuid + "OC_ROX");
            if (lowerCase.exists() && !lowerCase.delete()) {
                LOGGER.warn("Failed to delete temp file: {}", lowerCase);
            }
            if (upperCase.exists() && !upperCase.delete()) {
                LOGGER.warn("Failed to delete temp file: {}", upperCase);
            }
            if (!lowerCase.createNewFile()) {
                LOGGER.warn("Failed to create temp file: {}", lowerCase);
            }
            boolean insensitive = upperCase.exists();
            if (!lowerCase.delete()) {
                LOGGER.warn("Failed to delete temp file: {}", lowerCase);
            }
            return insensitive;
        } catch (Throwable t) {
            LOGGER.warn("Couldn't determine if file system is case sensitive, falling back to insensitive.", t);
            return true;
        }
    }

    public static boolean isValidFilename(String name) {
        for (char c : name.toCharArray()) {
            if (invalidChars.contains(c)) return false;
        }
        return true;
    }

    public static String validatePath(String path) {
        if (!isValidFilename(path)) {
            throw new RuntimeException(new java.io.IOException("path contains invalid characters"));
        }
        return path;
    }
}
