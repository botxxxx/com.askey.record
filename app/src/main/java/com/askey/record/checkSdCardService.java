package com.askey.record;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.StatFs;
import android.widget.TextView;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import static com.askey.record.Utils.EXTRA_VIDEO_COPY;
import static com.askey.record.Utils.EXTRA_VIDEO_PASTE;
import static com.askey.record.Utils.EXTRA_VIDEO_REFORMAT;
import static com.askey.record.Utils.EXTRA_VIDEO_REMOVE;
import static com.askey.record.Utils.EXTRA_VIDEO_VERSION;
import static com.askey.record.Utils.errorMessage;
import static com.askey.record.Utils.getPath;
import static com.askey.record.Utils.getSDPath;
import static com.askey.record.Utils.getSdCard;
import static com.askey.record.Utils.isError;
import static com.askey.record.Utils.logName;
import static com.askey.record.Utils.sdData;
import static com.askey.record.Utils.videoLogList;
import static com.askey.record.restartActivity.EXTRA_MAIN_PID;

public class checkSdCardService extends IntentService {

    public checkSdCardService() {
        // ActivityのstartService(intent);で呼び出されるコンストラクタはこちら
        super("checkSdCardService");
    }

    private void checkSdCardFromFileList() {
        getSdCard = !getSDPath().equals("");
        if (getSdCard) {
            try {
                StatFs stat = new StatFs(getSDPath());
                long sdAvailSize = stat.getAvailableBlocksLong()
                        * stat.getBlockSizeLong();
                double gigaAvailable = (sdAvailSize >> 30);
                if (gigaAvailable < sdData) {
                    videoLogList.add(new LogMsg("SD Card is Full."));
                    ArrayList<String> tmp = new ArrayList();
                    File[] fileList = new File(getSDPath()).listFiles();
                    for (int i = 0; i < fileList.length; i++) {
                        // Recursive call if it's a directory
                        File file = fileList[i];
                        if (!fileList[i].isDirectory()) {
                            if (Utils.getFileExtension(file.toString()).equals("mp4"))
                                tmp.add(file.toString());
                        }
                    }
                    if (tmp.size() >= 2) {
                        Object[] list = tmp.toArray();
                        Arrays.sort(list);
                        delete((String) list[0], false, true);
                        delete((String) list[1], false, true);
                        checkSdCardFromFileList();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                isError = true;
                getSdCard = !getSDPath().equals("");
                isError = true;
                videoLogList.add(new LogMsg("#error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.", mLog.e));
//                new Handler().post(() -> saveLog(this, false, false));
                errorMessage = "error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.";
//                ((TextView) findViewById(R.id.record_status)).setText("Error");
            }
        } else {
            isError = true;
            getSdCard = !getSDPath().equals("");
            videoLogList.add(new LogMsg("#error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.", mLog.e));
//            new Handler().post(() -> saveLog(this, false, false));
            errorMessage = "error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.";
//            ((TextView) findViewById(R.id.record_status)).setText("Error");
        }
    }

    private void delete(String path, boolean check, boolean fromSDcard) {
        try {
            if (path != "") {
                File video = new File(path);
                if (video.exists()) {
//                    if (check) {
//                        fileCheck(path);
//                    }
                    if (fromSDcard)
                        videoLogList.add(new LogMsg("Delete: " + path.split("/")[3], mLog.w));
                    else
                        videoLogList.add(new LogMsg("Delete: " + path.split("/")[5], mLog.w));
                    video.delete();
                } else {
                    videoLogList.add(new LogMsg("Video not find.", mLog.e));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            getSdCard = !getSDPath().equals("");
            isError = true;
            videoLogList.add(new LogMsg("#delete " + path + " error. <============ Crash here", mLog.e));
//            new Handler().post(() -> saveLog(getApplicationContext(), false, false));
            errorMessage = "Delete file error. <============ Crash here";
//            ((TextView) findViewById(R.id.record_status)).setText("Error");
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {

            Thread t = new Thread(() -> {
                try {
                    checkSdCardFromFileList();
                } catch (Exception e) {
                    e.printStackTrace();
                    if(null != videoLogList)
                    videoLogList.add(new LogMsg("checkSdCardFromFileList error.", mLog.e));
                }
            });
            t.start();
            t.join();
        } catch (Exception e) {
            e.printStackTrace();
            if(null != videoLogList)
            videoLogList.add(new LogMsg("checkSdCard Service error.", mLog.e));
        }
    }
}

