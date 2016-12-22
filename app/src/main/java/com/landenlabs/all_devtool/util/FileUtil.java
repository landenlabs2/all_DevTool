package com.landenlabs.all_devtool.util;

import android.content.Context;
import android.os.Build;
import android.system.StructStat;
import android.util.Log;
import android.widget.Button;

import java.io.File;
import java.io.IOException;

/**
 * Created by Dennis Lang on 7/13/16.
 */
public class FileUtil {
    public static final int KB = 1 << 10;
    public static final int MB = 1 << 20;
    public static final int GB = 1 << 30;
    public static final double KB_D = 1024.0;

    public static String getSizeStr(long size) {
        if (size <= 0)
            return "";
        else if (size < KB)
            return String.format("%d B", size);
        else if (size < MB)
            return String.format("%.1f KB", size / KB_D);
        else if (size < GB)
            return String.format("%.1f MB", size / KB / KB_D);
        else
            return String.format("%.1f GB", size / MB / KB_D);
    }

    public static class FileInfo extends File {
        public static final int FILESIZE_MAX_DIR_DEPTH = 3;
        public boolean isChecked = false;
        int mFileCount = 0;
        long mDepthSize = 0;


        public FileInfo(String name) {
            super(name);
        }

        public String getLongName() {
            try {
                String path1 = getCanonicalPath();
                String path2 = getAbsolutePath();
                if (!path2.equals(path1)) {
                    return String.format("%s -> %s", getName(), path1);
                }
            } catch (IOException ex) {
                //
            }

            return getName();
        }

        public int getCount() {
            return 0;
        }
        public long getLength() {
            return isDirectory() ? -1 : length();
        }
        public void setFileCnt(int cnt) { mFileCount = cnt;}
        public int getFileCnt() { return mFileCount; }

        public long getDevFreeMB() {
            long free = getUsableSpace();
            return (free != -1) ? free /MB : 0;
        }
        public long getDevSizeMB() {
            long size = getTotalSpace();
            return (size != -1) ? size /MB : 0;
        }

        public long getDeviceId() {
            if (Build.VERSION.SDK_INT >= 21) {
                try {
                    StructStat st = android.system.Os.stat(this.getCanonicalPath());
                    return st.st_dev;
                } catch (Exception ex) {

                }
            }

            return getTotalSpace() % 100;
        }

        /**
         * Returns the time when this file was last accessed, measured in
         * milliseconds since January 1st, 1970, midnight.
         * Returns 0 if the file does not exist.
         *
         * @return the time when this file was last accessed.
         */
        public long getAtime() {
            if (Build.VERSION.SDK_INT >= 21) {
                try {
                    StructStat st = android.system.Os.stat(this.getCanonicalPath());
                    return st.st_atime * 1000L;
                } catch (Exception ex) {

                }
            }

            return 0;
        }


        public long getDepthSize() {
            return mDepthSize;
        }
        public long findDepthSize(int fileCnt, int maxFiles) {
            if (fileCnt > maxFiles)
                return mDepthSize;
            else if (fileCnt == 0 && mDepthSize != 0) {
                return mDepthSize;
            } else if (fileCnt == -1) {
                mDepthSize = 0;
                fileCnt = 0;
            } else if (this.getAbsolutePath().startsWith("/proc")) {
                mDepthSize = 0;
                return mDepthSize;
            }

            try {
                File[] files = this.listFiles();
                if (files != null) {
                    for (File file : files) {
                        try {
                            FileUtil.FileInfo fileInfo = new FileUtil.FileInfo(file.getAbsolutePath());
                            if (fileInfo.isDirectory()) {
                                fileInfo.findDepthSize(fileCnt + 1, fileCnt);
                            } else if (file.isFile()) {
                                mDepthSize += fileInfo.length();
                                if (fileCnt++ > maxFiles) {
                                    return mDepthSize;
                                }
                            }
                        } catch (Exception ex) {
                            Log.e("FileUtil", ex.getLocalizedMessage(), ex);
                        }
                    }
                }
            } catch (Exception ex) {
                Log.e("FileUtil", ex.getLocalizedMessage(), ex);
            }

            return mDepthSize;
        }
    }

    public static class DirInfo extends Button {
        File m_dir;
        public DirInfo(Context context, File dir) {
            super(context);
            setText("/" + dir.getName());
            m_dir = dir;
        }

        public File getDir() {
            return m_dir;
        }
    }
}
