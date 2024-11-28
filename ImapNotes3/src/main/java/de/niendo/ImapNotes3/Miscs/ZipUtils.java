/*
 * Copyright (C)      2024 - Peter Korf <peter@niendo.de>
 * Copyright (C)      2024 - woheller69
 *
 * This file is part of ImapNotes3.
 *
 * ImapNotes3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.niendo.ImapNotes3.Miscs;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.Manifest;
import android.util.Log;

import de.niendo.ImapNotes3.R;

public class ZipUtils {

    public static final int PERMISSION_REQUEST_CODE = 123;
    public static final String TAG = "IN_ZipUtils";

    public static boolean checkPermissionStorage(Context context) {
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true;
        } else {
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static void requestPermission(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        //builder.setIcon(R.drawable.ic_warning_amber_black_24dp);
        builder.setTitle(activity.getResources().getString(R.string.permission_required));
        builder.setMessage(activity.getResources().getString(R.string.permission_message));
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            dialog.cancel();
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, whichButton) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void zipDirectory(String sourceDirPath, String zipFilePath, String basePath) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);
        zipDirectoryContents(new File(sourceDirPath), zos, basePath);
        zos.close();
        fos.close();
    }

    private static void zipDirectoryContents(File dir, ZipOutputStream zos, String basePath) throws IOException {
        File[] files = dir.listFiles();
        byte[] buffer = new byte[1024];
        int bytesRead;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectoryContents(file, zos, basePath + file.getName() + "/");
                continue;
            }

            FileInputStream fis = new FileInputStream(file);
            String entryName = basePath + file.getName();
            ZipEntry zipEntry = new ZipEntry(entryName);
            zos.putNextEntry(zipEntry);

            while ((bytesRead = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }

            fis.close();
            zos.closeEntry();
        }
    }

    // Function to list all directories in a zip file
    public static List<String> listDirectories(Context context, Uri zipFile) throws IOException {
        List<String> directories = new ArrayList<>();
        InputStream src = context.getContentResolver().openInputStream(zipFile);
        try (ZipInputStream zis = new ZipInputStream(src)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String fname = entry.getName();

                if (entry.isDirectory()) {
                    directories.add(fname);
                } else if (fname.contains("/")) {
                    String dir = fname.split("/")[0];
                    if (!directories.contains(dir)) {
                        directories.add(dir);
                    }
                }
            }
        }
        return directories;
    }

    // Function to list all files in a directory in a zip file
    public static List<String> listFilesInDirectory(Context context, Uri zipFile, String directory) throws IOException {
        List<String> files = new ArrayList<>();
        InputStream src = context.getContentResolver().openInputStream(zipFile);

        try (ZipInputStream zis = new ZipInputStream(src)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ((!entry.isDirectory() && (entry.getName().startsWith(directory + "/"))) || directory.isEmpty()) {
                    files.add(entry.getName());
                }
            }
        }

        return files;
    }

    // Function to extract a given file from a zip file
    public static String extractFile(Context context, Uri zipFile, String fileName, String destDirectory) throws IOException {
        byte[] buffer = new byte[1024];
        InputStream src = context.getContentResolver().openInputStream(zipFile);
        try (ZipInputStream zis = new ZipInputStream(src)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals(fileName)) {
                    File outFile = new File(destDirectory + File.separator + entry.getName());
                    new File(outFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    Log.d(TAG, "File extracted successfully: " + outFile.getAbsolutePath());
                    return outFile.getAbsolutePath();
                }
            }
        }
        Log.d(TAG, "File not found in the zip file.");
        return "";
    }

}
