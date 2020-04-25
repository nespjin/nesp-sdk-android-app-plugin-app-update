/*
 *
 *   Copyright (c) 2019  NESP Technology Corporation. All rights reserved.
 *
 *   This program is free software; you can redistribute it and/or modify it
 *   under the terms and conditions of the GNU General Public License,
 *   version 2, as published by the Free Software Foundation.
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License.See the License for the specific language governing permission and
 *   limitations under the License.
 *
 *   If you have any questions or if you find a bug,
 *   please contact the author by email or ask for Issues.
 *
 *   Author:JinZhaolu <1756404649@qq.com>
 */

package com.nesp.sdk.android.appupdate;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.nesp.sdk.android.app.ProgressDialog;
import com.nesp.sdk.android.app.downloadmanager.DownloadListener;
import com.nesp.sdk.android.app.downloadmanager.DownloadTask;

import java.io.File;

import static com.nesp.sdk.android.app.ApplicationManager.installApk;

/**
 * @Team: NESP Technology
 * @Author: 靳兆鲁
 * Email: 1756404649@qq.com
 * @Time: Created 2018/7/25 13:57
 * @Project AppUpdateDemo
 **/
public class DownloadFile {
    private static final String TAG = "DownloadFile";
    private Context context;
    private Activity activity;

    private String downloadUrl;
    private String downloadPath;
    private String authority;

    private String fileName;

    private DownloadTask downloadTask;

    private ProgressDialog progressDialog;

    private DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            showProgressDialog(context, PROGRESS_DIALOG_MESSAGE, progress);
        }

        @Override
        public void onSuccess() {
            Toast.makeText(context, R.string.progress_dialog_toast_download_seccess, Toast.LENGTH_SHORT).show();
            if (fileName.endsWith(".apk")) installApk(context, downloadPath + fileName, authority);
            if (progressDialog.isShowing()) progressDialog.dismiss();

        }

        @Override
        public void onFailed() {
            Toast.makeText(context, R.string.progress_dialog_toast_download_failed, Toast.LENGTH_SHORT).show();
            if (progressDialog.isShowing()) progressDialog.dismiss();
        }

        @Override
        public void onPaused() {
            Toast.makeText(context, R.string.progress_dialog_toast_download_pause, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            Toast.makeText(context, R.string.progress_dialog_toast_download_cancel, Toast.LENGTH_SHORT).show();
            if (progressDialog.isShowing()) progressDialog.dismiss();
        }
    };

    protected Boolean isShowCheckDialog;
    protected Boolean isForce;

    public DownloadFile(Context context, String downloadUrl, String downloadPath, String authority, Boolean isShowCheckDialog, Boolean isForce) {
        this.context = context;
        this.downloadUrl = downloadUrl;
        this.authority = authority;
        this.downloadPath = downloadPath;
        this.isShowCheckDialog = isShowCheckDialog;
        this.isForce = isForce;
    }

    private DownloadFile downloadApk;

    private DownloadFile getInstance() {
        if (downloadApk == null)
            return downloadApk = new DownloadFile(context, downloadUrl, downloadPath, authority, isShowCheckDialog, isForce);
        else return downloadApk;
    }

    private Boolean btnYesPause = false;

    private void showProgressDialog(final Context context, String message, int progress) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(context, true);
        }

        progressDialog.setOnProgressDialogClickListener(new ProgressDialog.OnProgressDialogClickListener() {
            @Override
            public void OnProgressDialogClick(ProgressDialog progressDialog, View view) {
                int i = view.getId();
                if (i == R.id.progress_dg_btn_no) {
                    DownloadFile.this.cancel();
                    if (!isShowCheckDialog && isForce) {
                        activity = (Activity) context;
                        activity.finish();
                    }
                }
            }
        });
        progressDialog.show(message, progress, context.getString(R.string.progress_dialog_btn_no_text));
    }

    private String PROGRESS_DIALOG_MESSAGE = "";

    public void start() {
        PROGRESS_DIALOG_MESSAGE = context.getString(R.string.progress_dialog_message_downloading);
        fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
        if (downloadTask == null) {
            downloadTask = new DownloadTask(downloadPath, downloadUrl);
            downloadTask.setDownloadListener(downloadListener);
            downloadTask.execute();
            showProgressDialog(context, PROGRESS_DIALOG_MESSAGE, 0);
            Toast.makeText(context, R.string.progress_dialog_toast_start_download, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 暂停下载
     */
    public void pause() {
        if (downloadTask != null) {
            downloadTask.pause();
        }
    }

    /**
     * 取消下载
     */
    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel();
            return;
        }

        if (downloadUrl == null) {
            return;
        }

        //删除已下载文件
        deleteFile();
    }

    private void deleteFile() {
        //删除已下载文件
        File file;
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
        if (downloadPath.equals("")) {
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
        } else {
            file = new File(downloadPath);
        }
        if (file.exists()) {
            file.delete();
        }
    }
}
