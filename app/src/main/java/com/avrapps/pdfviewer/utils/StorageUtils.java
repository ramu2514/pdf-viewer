package com.avrapps.pdfviewer.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.constants.AppConstants;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static android.content.Context.USB_SERVICE;
import static android.os.Build.VERSION.SDK_INT;

public class StorageUtils {

    private static final String DEFAULT_FALLBACK_STORAGE_PATH = "/storage/sdcard0";
    private static final String LOG = "StorageUtils";
    private final Pattern DIR_SEPARATOR = Pattern.compile("/");
    private final String PREFIX_OTG = "otg:/";
    Activity activity;

    public StorageUtils(MainActivity activity) {
        this.activity=activity;
    }

    public ArrayList<StorageDirectoryParcelable> getStorageList() {
        ArrayList<StorageDirectoryParcelable> volumes;
        if (SDK_INT >= Build.VERSION_CODES.N) {
            volumes = getStorageDirectoriesNew();
        } else {
            volumes = getStorageDirectoriesLegacy();
        }
        return volumes;
    }

    /**
     * @return All available storage volumes (including internal storage, SD-Cards and USB devices)
     */
    @TargetApi(Build.VERSION_CODES.N)
    public synchronized ArrayList<StorageDirectoryParcelable> getStorageDirectoriesNew() {
        // Final set of paths
        Set<String> removableStorages = new HashSet<>();
        ArrayList<StorageDirectoryParcelable> volumes = new ArrayList<>();
        StorageManager sm = activity.getSystemService(StorageManager.class);
        if (sm == null) return volumes;
        for (StorageVolume volume : sm.getStorageVolumes()) {
            if (!volume.getState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)
                    && !volume.getState().equalsIgnoreCase(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                continue;
            }
            File path = getVolumeDirectory(volume);
            String name = volume.getDescription(activity);
            int icon;
            if (!volume.isRemovable()) {
                icon = R.drawable.ic_phone_android_white_24dp;
            } else {
                // HACK: There is no reliable way to distinguish USB and SD external storage
                // However it is often enough to check for "USB" String
                if (name.toUpperCase().contains("USB") || path.getPath().toUpperCase().contains("USB")) {
                    icon = R.drawable.ic_usb_white_24dp;
                } else {
                    icon = R.drawable.ic_sd_storage_white_24dp;
                }
                removableStorages.add(path.getAbsolutePath());
            }
            volumes.add(new StorageDirectoryParcelable(path.getPath(), name, icon));
        }
        PreferenceUtil.writeStringSet(activity, AppConstants.REMOVABLE_STORAGES, removableStorages);
        return volumes;
    }

    /**
     * Returns all available SD-Cards in the system (include emulated)
     *
     * <p>Warning: Hack! Based on Android source code of version 4.3 (API 18) Because there was no
     * standard way to get it before android N
     *
     * @return All available SD-Cards in the system (include emulated)
     */
    public synchronized ArrayList<StorageDirectoryParcelable> getStorageDirectoriesLegacy() {
        List<String> rv = new ArrayList<>();

        // Primary physical SD-CARD (not emulated)
        final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
        // All Secondary SD-CARDs (all exclude primary) separated by ":"
        final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
        // Primary emulated SD-CARD
        final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
        if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
            // Device has physical external storage; use plain paths.
            if (TextUtils.isEmpty(rawExternalStorage)) {
                // EXTERNAL_STORAGE undefined; falling back to default.
                // Check for actual existence of the directory before adding to list
                if (new File(DEFAULT_FALLBACK_STORAGE_PATH).exists()) {
                    rv.add(DEFAULT_FALLBACK_STORAGE_PATH);
                } else {
                    // We know nothing else, use Environment's fallback
                    rv.add(Environment.getExternalStorageDirectory().getAbsolutePath());
                }
            } else {
                rv.add(rawExternalStorage);
            }
        } else {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            final String rawUserId;
            if (SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                rawUserId = "";
            } else {
                final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                final String[] folders = DIR_SEPARATOR.split(path);
                final String lastFolder = folders[folders.length - 1];
                boolean isDigit = false;
                try {
                    Integer.valueOf(lastFolder);
                    isDigit = true;
                } catch (NumberFormatException ignored) {
                }
                rawUserId = isDigit ? lastFolder : "";
            }
            // /storage/emulated/0[1,2,...]
            if (TextUtils.isEmpty(rawUserId)) {
                rv.add(rawEmulatedStorageTarget);
            } else {
                rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
            }
        }
        // Add all secondary storages
        if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
            // All Secondary SD-CARDs splited into array
            final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
            Collections.addAll(rv, rawSecondaryStorages);
        }
        if (SDK_INT >= Build.VERSION_CODES.M && checkStoragePermission()) rv.clear();
        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String[] strings = getExtSdCardPathsForActivity(activity);
            for (String s : strings) {
                File f = new File(s);
                if (!rv.contains(s) && canListFiles(f)) rv.add(s);
            }
        }
        File usb = getUsbDrive();
        if (usb != null && !rv.contains(usb.getPath())) rv.add(usb.getPath());

        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isOTGDeviceConnected()) {
                rv.add(PREFIX_OTG + "/");
            }
        }

        // Assign a label and icon to each directory
        ArrayList<StorageDirectoryParcelable> volumes = new ArrayList<>();
        for (String file : rv) {
            File f = new File(file);
            @DrawableRes int icon;

            if ("/storage/emulated/legacy".equals(file)
                    || "/storage/emulated/0".equals(file)
                    || "/mnt/sdcard".equals(file)) {
                icon = R.drawable.ic_phone_android_white_24dp;
            } else if ("/storage/sdcard1".equals(file)) {
                icon = R.drawable.ic_sd_storage_white_24dp;
            } else if ("/".equals(file)) {
                icon = R.drawable.ic_drawer_root_white;
            } else {
                icon = R.drawable.ic_sd_storage_white_24dp;
            }

            String name = getDeviceDescriptionLegacy(activity, f);
            volumes.add(new StorageDirectoryParcelable(file, name, icon));
        }

        return volumes;
    }

    private boolean checkStoragePermission() {
        String[] permissionArrays = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int res = ActivityCompat.checkSelfPermission(activity, permissionArrays[0]);
            if (res != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, permissionArrays, AppConstants.PERMISSIONS_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    private boolean isOTGDeviceConnected() {
        List<UsbOtgRepresentation> connectedDevices = getMassStorageDevicesConnected(activity);
        if (!connectedDevices.isEmpty()) {

        }
        return false;
    }

    private String getDeviceDescriptionLegacy(Context context, File file) {
        String path = file.getPath();

        switch (path) {
            case "/storage/emulated/legacy":
            case "/storage/emulated/0":
            case "/mnt/sdcard":
                return context.getString(R.string.storage_internal);
            case "/storage/sdcard":
            case "/storage/sdcard1":
                return context.getString(R.string.storage_sd_card);
            case "/":
                return context.getString(R.string.root_directory);
            default:
                return file.getName();
        }
    }

    @NonNull
    private List<UsbOtgRepresentation> getMassStorageDevicesConnected(
            @NonNull final Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(USB_SERVICE);
        if (usbManager == null) return Collections.emptyList();

        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
        ArrayList<UsbOtgRepresentation> usbOtgRepresentations = new ArrayList<>();

        for (String deviceName : devices.keySet()) {
            UsbDevice device = devices.get(deviceName);

            for (int i = 0; i < device.getInterfaceCount(); i++) {
                if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_MASS_STORAGE) {
                    final @Nullable String serial =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                    ? device.getSerialNumber()
                                    : null;

                    UsbOtgRepresentation usb =
                            new UsbOtgRepresentation(device.getProductId(), device.getVendorId(), serial);
                    usbOtgRepresentations.add(usb);
                }
            }
        }

        return usbOtgRepresentations;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String[] getExtSdCardPathsForActivity(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w(LOG, "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1");
        return paths.toArray(new String[0]);
    }

    private boolean canListFiles(File f) {
        return f.canRead() && f.isDirectory();
    }

    public File getUsbDrive() {
        File parent = new File("/storage");

        try {
            for (File f : parent.listFiles())
                if (f.exists() && f.getName().toLowerCase().contains("usb") && f.canExecute()) return f;
        } catch (Exception e) {
        }

        parent = new File("/mnt/sdcard/usbStorage");
        if (parent.exists() && parent.canExecute()) return parent;
        parent = new File("/mnt/sdcard/usb_storage");
        if (parent.exists() && parent.canExecute()) return parent;

        return null;
    }

    @TargetApi(Build.VERSION_CODES.N)
    private File getVolumeDirectory(StorageVolume volume) {
        try {
            Field f = StorageVolume.class.getDeclaredField("mPath");
            f.setAccessible(true);
            return (File) f.get(volume);
        } catch (Exception e) {
            // This shouldn't fail, as mPath has been there in every version
            throw new RuntimeException(e);
        }
    }

    private static class UsbOtgRepresentation {
        public final int productID, vendorID;
        public final @Nullable
        String serialNumber;

        public UsbOtgRepresentation(int productID, int vendorID, @Nullable String serialNumber) {
            this.productID = productID;
            this.vendorID = vendorID;
            this.serialNumber = serialNumber;
        }
    }

    public static class StorageDirectoryParcelable {
        private String path, name;
        private int icon;

        public StorageDirectoryParcelable(String path, String name, int icon) {
            this.path = path;
            this.name = name;
            this.icon = icon;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public int getIcon() {
            return icon;
        }

        @Override
        public String toString() {
            return "StorageDirectoryParcelable{" +
                    "path='" + path + '\'' +
                    ", name='" + name + '\'' +
                    ", icon=" + icon +
                    '}';
        }
    }
}
