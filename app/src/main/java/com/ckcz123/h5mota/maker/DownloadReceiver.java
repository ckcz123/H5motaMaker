package com.ckcz123.h5mota.maker;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.ckcz123.h5mota.maker.lib.CustomToast;
import com.ckcz123.h5mota.maker.lib.Utils;

import java.io.File;

/**
 * Created by oc on 2018/4/25.
 */

public class DownloadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            DownloadManager downloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor c = downloadManager.query(query);
            if (c.moveToFirst()) {
                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                    String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    //TODO : Use this local uri and launch intent to open file

                    Uri uri = Uri.parse(uriString);
                    File file = new File(uri.getPath());
                    String name = file.getName();

                    CustomToast.showSuccessToast(context, "下载成功！\n" + name + " 已经成功下载到 SD卡/H5motaMaker 目录下！", 5000);

                    /*
                    if (name.endsWith(".zip")) {
                        CustomToast.showSuccessToast(context, "下载成功！");
                    }
                    else if (name.endsWith(".apk")) {
                        // auto install

                        try {

                            Intent install = new Intent(Intent.ACTION_VIEW);
                            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.N) {
                                install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID+".fileprovider", file);
                                install.setDataAndType(contentUri, "application/vnd.android.package-archive");
                            }
                            else {
                                install.setDataAndType(uri, "application/vnd.android.package-archive");
                            }

                            context.startActivity(install);
                        }
                        catch (Exception e) {
                            Log.e("Install Error", e.getMessage(), e);
                            CustomToast.showErrorToast(context, "无法自动安装更新包，请手动进行安装。");
                        }

                    }
                    */

                }
            }
        }
    }
}
