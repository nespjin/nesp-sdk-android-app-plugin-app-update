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

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import com.nesp.sdk.android.app.AlertLoadingDialog;
import com.nesp.sdk.android.app.ApplicationInfo;
import com.nesp.sdk.android.app.DoubleButtonDialog;
import com.nesp.sdk.android.app.SingleButtonDialog;
import com.nesp.sdk.android.parsedata.json.Json;

import static com.nesp.sdk.android.net.Internet.NetWork.isWifi;

/**
 * @Team: NESP Technology
 * @Author: 靳兆鲁
 * Email: 1756404649@qq.com
 * @Time: Created 2018/7/25 14:17
 * @Project AppUpdateDemo
 **/
public class AppUpdate {

    private static final String TAG = "AppUpdate";

    private String serverUrl;
    private String downloadPath;

    private String localVersionName;
    private int localVersionCode;

    private String serverUpdateLog;
    private String serverDownloadUrl;
    private String serverVersionName;
    private int serverVersionCode;
    private Boolean serverIsForce;

    private Context context;
    private String authority;

    private DownloadFile downloadApk;

    private Boolean isShowCheckDialog;
    private Boolean isOnlyWifi;
    private Boolean isAutoUpdate;

    private final static String SERVER_KEY_DOWNLOAD_URL = "downloadUrl";
    private final static String SERVER_KEY_UPDATE_LOG = "updateLog";
    private final static String SERVER_KEY_VERSION_NAME = "versionName";
    private final static String SERVER_KEY_VERSION_CODE = "versionCode";
    private final static String SERVER_KEY_IS_FORCE = "isForce";

    private DoubleButtonDialog doubleButtonDialog;
    private SingleButtonDialog singleButtonDialog;

    public AppUpdate(String downloadPath, String serverUrl, Context context, String authority, Boolean isShowCheckDialog, Boolean isOnlyWifi, Boolean isAutoUpdate) {
        this.downloadPath = downloadPath;
        this.authority = authority;
        this.serverUrl = serverUrl;
        this.context = context;
        this.isShowCheckDialog = isShowCheckDialog;
        this.isOnlyWifi = isOnlyWifi;
        this.isAutoUpdate = isAutoUpdate;
    }


    private AlertLoadingDialog loadingDialogCheckUpdate;

    public void startUpdate() {
        loadingDialogCheckUpdate = new AlertLoadingDialog.Builder(context).setLoadingMessage(context.getString(R.string.app_update_loading_dialog_checking_message)).create();
        new checkUpdate().execute();

        doubleButtonDialog = new DoubleButtonDialog(context, true);
        doubleButtonDialog.setOnDoubleButtonDialogClickListener(new DoubleButtonDialog.OnDoubleButtonDialogClickListener() {
            @Override
            public void OnDoubleButtonDialogClick(DoubleButtonDialog doubleButtonDialog, View view) {
                int i = view.getId();
                if (i == R.id.double_btn_dg_btn_yes) {
                    updateLaunch();
                } else if (view.getId() == R.id.double_btn_dg_btn_no) {
                    if (onHasNewVersionDialogCancelClickListener != null)
                        onHasNewVersionDialogCancelClickListener.hasNewVersionDialogCancelClick(view);
                }
            }
        });
        singleButtonDialog = new SingleButtonDialog(context, true);
        singleButtonDialog.setOnSingleButtonDialogClickListener(new SingleButtonDialog.OnSingleButtonDialogClickListener() {
            @Override
            public void OnSingleButtonDialogClick(SingleButtonDialog singleButtonDialog, View view) {
                int i = view.getId();
                if (i == R.id.single_btn_dg_btn_yes) {
                    updateLaunch();
                }
            }
        });
    }

    private void updateLaunch() {
        if (isOnlyWifi) {
            if (isWifi(context)) {
                downloadApk.start();
            } else {
                Toast.makeText(context, R.string.app_update_toast_link_wifi_to_download_update, Toast.LENGTH_SHORT).show();
            }
        } else {
            downloadApk.start();
        }
    }

    private String getServerValue(String key) throws Exception {
        return Json.ParseResponseJsonWithGSON.getValue(serverUrl, key);
    }

    private enum checkResults {
        CHECK_SUCCESS_LAST_VERSION,
        CHECK_SUCCESS_HAVE_NEW_VERSION,
        CHECK_SERVER_ERROR
    }


    private class checkUpdate extends AsyncTask<Integer, Integer, checkResults> {
        @Override
        protected void onPreExecute() {
            if (isShowCheckDialog)
                loadingDialogCheckUpdate.show();
        }

        @Override
        protected checkResults doInBackground(Integer... integers) {
            try {
                localVersionName = ApplicationInfo.getAppVersionName(context);
                localVersionCode = ApplicationInfo.getAppVersionCode(context);

                serverIsForce = Boolean.valueOf(getServerValue(SERVER_KEY_IS_FORCE));
                serverVersionCode = Integer.parseInt(getServerValue(SERVER_KEY_VERSION_CODE));
                serverVersionName = getServerValue(SERVER_KEY_VERSION_NAME);
                serverUpdateLog = getServerValue(SERVER_KEY_UPDATE_LOG);
                serverDownloadUrl = getServerValue(SERVER_KEY_DOWNLOAD_URL);

                if (localVersionCode >= serverVersionCode) {
                    return checkResults.CHECK_SUCCESS_LAST_VERSION;
                } else {
                    return checkResults.CHECK_SUCCESS_HAVE_NEW_VERSION;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return checkResults.CHECK_SERVER_ERROR;
            }
        }

        @Override
        protected void onPostExecute(checkResults checkResults) {
            if (loadingDialogCheckUpdate.isShowing()) loadingDialogCheckUpdate.dismiss();
            switch (checkResults) {
                case CHECK_SUCCESS_HAVE_NEW_VERSION:
                    try {
                        downloadApk = new DownloadFile(context, serverDownloadUrl, downloadPath, authority, isShowCheckDialog, serverIsForce);
                        if (!isShowCheckDialog) {
                            if (serverIsForce) {
                                handleResult(checkResults);
                            } else if (isAutoUpdate) {
                                handleResult(checkResults);
                            }
                        } else {
                            handleResult(checkResults);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case CHECK_SUCCESS_LAST_VERSION:
                    if (isShowCheckDialog) {
                        Toast.makeText(context, R.string.app_update_is_last_version, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case CHECK_SERVER_ERROR:
                    if (isShowCheckDialog) {
                        Toast.makeText(context, R.string.app_update_server_error, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    private OnHasNewVersionDialogCancelClickListener onHasNewVersionDialogCancelClickListener;

    public interface OnHasNewVersionDialogCancelClickListener {
        void hasNewVersionDialogCancelClick(View view);
    }

    public void setOnHasNewVersionDialogCancelClickListener(OnHasNewVersionDialogCancelClickListener onHasNewVersionDialogCancelClickListener) {
        this.onHasNewVersionDialogCancelClickListener = onHasNewVersionDialogCancelClickListener;
    }

    private void handleResult(checkResults checkResults) {
        switch (checkResults) {
            case CHECK_SERVER_ERROR:
                Toast.makeText(context, R.string.app_update_toast_llink_server_failed, Toast.LENGTH_SHORT).show();
                break;
            case CHECK_SUCCESS_LAST_VERSION:
                Toast.makeText(context, R.string.app_update_toast_is_last_version, Toast.LENGTH_SHORT).show();
            case CHECK_SUCCESS_HAVE_NEW_VERSION:
                if (serverIsForce) {
                    if (!singleButtonDialog.isShowing()) {
                        singleButtonDialog.show(context, context.getString(R.string.app_update_dialog_title) + serverVersionName, serverUpdateLog, context.getString(R.string.app_update_dialog_btn_yes), 0);
                        singleButtonDialog.setCanceledOnTouchOutside(false);
                        singleButtonDialog.setCancelable(false);
                    }
                } else {
                    if (!doubleButtonDialog.isShowing())
                        doubleButtonDialog.show(context, context.getString(R.string.app_update_dialog_title) + serverVersionName, serverUpdateLog, context.getString(R.string.app_update_dialog_btn_yes), context.getString(R.string.app_update_dialog_btn_no), 0);
                }
                break;
        }
    }
}
