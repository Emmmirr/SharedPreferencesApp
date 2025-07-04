package com.example.sharedpreferencesapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionsHelper {
    
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 100;
    
    /**
     * Verifica si los permisos de almacenamiento están concedidos
     */
    public static boolean hasStoragePermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            
            return writePermission == PackageManager.PERMISSION_GRANTED && 
                   readPermission == PackageManager.PERMISSION_GRANTED;
        }
        return true; // En versiones anteriores a Android 6.0 los permisos se conceden al instalar
    }
    
    /**
     * Solicita permisos de almacenamiento
     */
    public static void requestStoragePermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
            
            ActivityCompat.requestPermissions(activity, permissions, STORAGE_PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * Verifica si los permisos fueron concedidos en el callback
     */
    public static boolean arePermissionsGranted(int[] grantResults) {
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}