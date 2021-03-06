package com.emindsoft.openthos.system;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.emindsoft.openthos.R;

import java.util.HashMap;
public class FileIconHelper implements FileIconLoader.IconLoadFinishListener {

    private static HashMap<ImageView, ImageView> imageFrames = new HashMap<>();

    private static HashMap<String, Integer> fileExtToIcons = new HashMap<>();

    private FileIconLoader mIconLoader;

    static {
        addItem(new String[] {
            "mp3"
        }, R.mipmap.music_default_bg);
        addItem(new String[] {
            "wma"
        }, R.mipmap.music_default_bg);
        addItem(new String[] {
            "wav"
        }, R.mipmap.music_default_bg);
        addItem(new String[] {
            "mid"
        }, R.mipmap.music_default_bg);
        addItem(new String[] {
                "mp4", "wmv", "mpeg", "m4v", "3gp", "3gpp", "3g2", "3gpp2", "asf"
        }, R.mipmap.video_default_icon);
        addItem(new String[] {
                "jpg", "jpeg", "gif", "png", "bmp", "wbmp"
        }, R.mipmap.category_icon_picture);
        addItem(new String[] {
                "txt", "log", "xml", "ini", "lrc"
        }, R.mipmap.file_icon_txt);
        addItem(new String[] {
                "doc", "ppt", "docx", "pptx", "xsl", "xslx",
        }, R.mipmap.file_icon_office);
        addItem(new String[] {
            "pdf"
        }, R.mipmap.file_icon_pdf);
        addItem(new String[] {
            "zip"
        }, R.mipmap.file_icon_zip);
        addItem(new String[] {
            "mtz"
        }, R.mipmap.file_icon_theme);
        addItem(new String[] {
            "rar"
        }, R.mipmap.file_icon_rar);
    }

    public FileIconHelper(Context context) {
        mIconLoader = new FileIconLoader(context, this);
    }

    private static void addItem(String[] exts, int resId) {
        if (exts != null) {
            for (String ext : exts) {
                fileExtToIcons.put(ext.toLowerCase(), resId);
            }
        }
    }

    public static int getFileIcon(String ext) {
        Integer i = fileExtToIcons.get(ext.toLowerCase());
        if (i != null) {
            return i;
        } else {
            return R.mipmap.file_icon_default;
        }

    }

    public void setIcon(FileInfo fileInfo, ImageView fileImage, ImageView fileImageFrame) {
        String filePath = fileInfo.filePath;
        long fileId = fileInfo.dbId;
        String extFromFilename = Util.getExtFromFilename(filePath);
        FileCategoryHelper.FileCategory fc = FileCategoryHelper.getCategoryFromPath(filePath);
        fileImageFrame.setVisibility(View.GONE);

        boolean set;
        int id = getFileIcon(extFromFilename);
        fileImage.setImageResource(id);

        mIconLoader.cancelRequest(fileImage);
        switch (fc) {
            case Apk:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
                break;
            case Picture:
            case Video:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
                if (set)
                    fileImageFrame.setVisibility(View.VISIBLE);
                else {
                    fileImage.setImageResource(fc == FileCategoryHelper.FileCategory.Picture ? R.drawable.file_icon_picture
                            : R.mipmap.video_default_icon);
                    imageFrames.put(fileImage, fileImageFrame);
                    set = true;
                }
                break;
            default:
                set = true;
                break;
        }

        if (!set)
            fileImage.setImageResource(R.mipmap.file_icon_default);
    }

    @Override
    public void onIconLoadFinished(ImageView view) {
        ImageView frame = imageFrames.get(view);
        if (frame != null) {
            frame.setVisibility(View.VISIBLE);
            imageFrames.remove(view);
        }
    }
}
