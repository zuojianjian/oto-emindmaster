package com.ygcompany.zuojj.ymfilemanager.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 获取手机内存大小的工具类
 * Created by zuojj on 16-6-1.
 */
public class MemoryUtil {

    // 获取android当前可用内存大小
    public static long getAvailMemory(Context context) {

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        //mi.availMem; 当前系统的可用内存
        return mi.availMem;
        //return Formatter.formatFileSize(context, mi.availMem);// 将获取的内存大小规格化
    }

    public static long getTotalMemory(Context context) {
        String path = "/proc/meminfo";// 系统内存信息文件
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;

        try {
            FileReader localFileReader = new FileReader(path);
            BufferedReader localBufferedReader = new BufferedReader(
                    localFileReader, 8192);
            str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小

            arrayOfString = str2.split("\\s+");
            for (String num : arrayOfString) {
                Log.i(str2, num + "\t");
            }

            initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
            L.i("tag",initial_memory+"");
            localBufferedReader.close();

        } catch (IOException e) {

        }
        return initial_memory;
        //return Formatter.formatFileSize(context, initial_memory);// Byte转换为KB或者MB，内存大小规格化
    }

}
