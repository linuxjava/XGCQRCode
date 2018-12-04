package com.tencent.qbar;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by williamjin on 18/5/14.
 */

public class Util {

    public static boolean isNullOrNil(String string) {
        if (string == null || string.length() == 0) {
            return true;
        }
        return false;
    }

    public static void copyFile(String srcPath, String destPath, boolean replace) {
        File destFile  = new File(destPath);
        File srcFile = new File(srcPath);
        if (!srcFile.exists()) {
            return;
        }
        if (!destFile.exists() || (destFile.exists() && replace)) {
            try {
                destFile.delete();
                FileUtils.copyFile(srcFile, destFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void copyFile(InputStream srcInput, String destPath, boolean replace) {
        File destFile  = new File(destPath);

        if (!destFile.exists() || (destFile.exists() && replace)) {
            try {
                destFile.delete();
                FileUtils.copyInputStreamToFile(srcInput, destFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
