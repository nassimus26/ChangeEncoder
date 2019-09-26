package util;

import java.io.File;

public class FileFilter {
    public static boolean match(String[] in_exts, String[] ex_exts, File file, boolean visitDirectory) {
        if (file.isFile() && !file.getName().contains("."))
            return false;
        boolean match = true;
        if (ex_exts!=null) {
            for (String ext : ex_exts) {
                if (file.getAbsolutePath().matches("(?i)"+ext)){
                    return false;
                }
            }
        }
        if (in_exts!=null) {
            if (in_exts.length==1 && in_exts[0].equals(""))
                return match;
            match = false;
            for (String ext : in_exts) {
                if ((visitDirectory && file.isDirectory()) || file.getAbsolutePath().matches("(?i)"+ext)){
                    match = true;
                    break;
                }
            }
        }

        return match;
    }
}
