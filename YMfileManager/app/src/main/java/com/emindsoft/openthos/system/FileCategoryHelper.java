package com.emindsoft.openthos.system;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

import com.emindsoft.openthos.R;

import java.io.FilenameFilter;
import java.util.HashMap;


/**
 * 文件分类管理帮助类
 */
public class FileCategoryHelper {
//    public static final int COLUMN_ID = 0;
//
//    public static final int COLUMN_PATH = 1;
//
//    public static final int COLUMN_SIZE = 2;
//
//    public static final int COLUMN_DATE = 3;

    private static final String LOG_TAG = "FileCategoryHelper";
//以枚举的形式定义不同文件类型
    public enum FileCategory {
        All, Music, Video, Picture, Theme, Doc, Zip, Apk, Custom, Other, Favorite
    }

    private static String[] ZIP_EXTS  = new String[] {
            "zip", "rar"
    };

    public static HashMap<FileCategory, FilenameExtFilter> filters = new HashMap<>();

    public static HashMap<FileCategory, Integer> categoryNames = new HashMap<>();

    static {
        categoryNames.put(FileCategory.All, R.string.category_all);
        categoryNames.put(FileCategory.Music, R.string.category_music);
        categoryNames.put(FileCategory.Video, R.string.category_video);
        categoryNames.put(FileCategory.Picture, R.string.category_picture);
        categoryNames.put(FileCategory.Theme, R.string.category_theme);
        categoryNames.put(FileCategory.Doc, R.string.category_document);
        categoryNames.put(FileCategory.Zip, R.string.category_zip);
        categoryNames.put(FileCategory.Apk, R.string.category_apk);
        categoryNames.put(FileCategory.Other, R.string.category_other);
        categoryNames.put(FileCategory.Favorite, R.string.category_favorite);
    }

    public static FileCategory[] sCategories = new FileCategory[] {
            FileCategory.Music, FileCategory.Video, FileCategory.Picture, FileCategory.Theme,
            FileCategory.Doc, FileCategory.Zip, FileCategory.Apk, FileCategory.Other
    };

    private FileCategory mCategory;

    private Context mContext;

    public FileCategoryHelper(Context context) {
        mContext = context;

        mCategory = FileCategory.All;
    }

    public FileCategory getCurCategory() {
        return mCategory;
    }

    public void setCurCategory(FileCategory c) {
        mCategory = c;
    }

    public int getCurCategoryNameResId() {
        return categoryNames.get(mCategory);
    }

    public void setCustomCategory(String[] exts) {
        mCategory = FileCategory.Custom;
        if (filters.containsKey(FileCategory.Custom)) {
            filters.remove(FileCategory.Custom);
        }

        filters.put(FileCategory.Custom, new FilenameExtFilter(exts));
    }

    public FilenameFilter getFilter() {
        return filters.get(mCategory);
    }

    private HashMap<FileCategory, CategoryInfo> mCategoryInfo = new HashMap<>();

    public HashMap<FileCategory, CategoryInfo> getCategoryInfos() {
        return mCategoryInfo;
    }

    public CategoryInfo getCategoryInfo(FileCategory fc) {
        if (mCategoryInfo.containsKey(fc)) {
            return mCategoryInfo.get(fc);
        } else {
            CategoryInfo info = new CategoryInfo();
            mCategoryInfo.put(fc, info);
            return info;
        }
    }

    public class CategoryInfo {
        public long count;

        public long size;
    }

    private void setCategoryInfo(FileCategory fc, long count, long size) {
        CategoryInfo info = mCategoryInfo.get(fc);
        if (info == null) {
            info = new CategoryInfo();
            mCategoryInfo.put(fc, info);
        }
        info.count = count;
        info.size = size;
    }

//对于文档文件（Doc）的查詢條件是
    private String buildDocSelection() {
        StringBuilder selection = new StringBuilder();
        for (String aSDocMimeTypesSet : Util.sDocMimeTypesSet) {
            selection.append("(" + FileColumns.MIME_TYPE + "=='" + aSDocMimeTypesSet + "') OR ");
        }
        return  selection.substring(0, selection.lastIndexOf(")") + 1);
    }
//因为只有MediaStore.Files中的文件内容需要区分，因此按条件查询只需要对这个进行即可。
    private String buildSelectionByCategory(FileCategory cat) {
        String selection;
        switch (cat) {
            case Theme:
                selection = FileColumns.DATA + " LIKE '%.mtz'";
                break;
            case Doc:
                selection = buildDocSelection();
                break;
            case Zip:
                selection = "(" + FileColumns.MIME_TYPE + " == '" + Util.sZipFileMimeType + "')";
                break;
            case Apk:
                selection = FileColumns.DATA + " LIKE '%.apk'";
                break;
            default:
                selection = null;
        }
        return selection;
    }

    private Uri getContentUriByCategory(FileCategory cat) {
        Uri uri;
        String volumeName = "external";
        switch(cat) {
            case Theme:
            case Doc:
            case Zip:
            case Apk:
                uri = Files.getContentUri(volumeName);
                break;
            case Music:
                uri = Audio.Media.getContentUri(volumeName);
                break;
            case Video:
                uri = Video.Media.getContentUri(volumeName);
                break;
            case Picture:
                uri = Images.Media.getContentUri(volumeName);
                break;
           default:
               uri = null;
        }
        return uri;
    }
//文件的排序规则
    private String buildSortOrder(FileSortHelper.SortMethod sort) {
        String sortOrder = null;
        switch (sort) {
            case name:
                sortOrder = FileColumns.TITLE + " asc";
                break;
            case size:
                sortOrder = FileColumns.SIZE + " asc";
                break;
            case date:
                sortOrder = FileColumns.DATE_MODIFIED + " desc";
                break;
            case type:
                sortOrder = FileColumns.MIME_TYPE + " asc, " + FileColumns.TITLE + " asc";
                break;
        }
        return sortOrder;
    }
//查询文件路径，名称，排序并返回
    public Cursor query(FileCategory fc, FileSortHelper.SortMethod sort) {
        Uri uri = getContentUriByCategory(fc);
        String selection = buildSelectionByCategory(fc);
        String sortOrder = buildSortOrder(sort);

        if (uri == null) {
            Log.e(LOG_TAG, "invalid uri, category:" + fc.name());
            return null;
        }

        String[] columns = new String[] {
                FileColumns._ID, FileColumns.DATA, FileColumns.SIZE, FileColumns.DATE_MODIFIED
        };

        return mContext.getContentResolver().query(uri, columns, selection, null, sortOrder);
    }
//设置各种文件查询的uri
    public void refreshCategoryInfo() {
        // clear
        for (FileCategory fc : sCategories) {
            setCategoryInfo(fc, 0, 0);
        }

        // query database
        String volumeName = "external";

        Uri uri = Audio.Media.getContentUri(volumeName);
        refreshMediaCategory(FileCategory.Music, uri); //音频文件

        uri = Video.Media.getContentUri(volumeName);
        refreshMediaCategory(FileCategory.Video, uri);//视频文件

        uri = Images.Media.getContentUri(volumeName);//图片文件
        refreshMediaCategory(FileCategory.Picture, uri);

        uri = Files.getContentUri(volumeName);//其他文件
        refreshMediaCategory(FileCategory.Theme, uri);
        refreshMediaCategory(FileCategory.Doc, uri);
        refreshMediaCategory(FileCategory.Zip, uri);
        refreshMediaCategory(FileCategory.Apk, uri);
    }
//进行了对ContentProvider的查询，根据buildSelectionByCategory方法中设置的条件来查询。
    private boolean refreshMediaCategory(FileCategory fc, Uri uri) {
        String[] columns = new String[] {
                "COUNT(*)", "SUM(_size)"
        };
        Cursor c = mContext.getContentResolver().query(uri, columns, buildSelectionByCategory(fc), null, null);
        if (c == null) {
            Log.e(LOG_TAG, "fail to query uri:" + uri);
            return false;
        }

        if (c.moveToNext()) {
            setCategoryInfo(fc, c.getLong(0), c.getLong(1));
            Log.v(LOG_TAG, "Retrieved " + fc.name() + " info >>> count:" + c.getLong(0) + " size:" + c.getLong(1));
            c.close();
            return true;
        }

        return false;
    }

    public static FileCategory getCategoryFromPath(String path) {
        MediaFile.MediaFileType type = MediaFile.getFileType(path);
        if (type != null) {
            if (MediaFile.isAudioFileType(type.fileType)) return FileCategory.Music;
            if (MediaFile.isVideoFileType(type.fileType)) return FileCategory.Video;
            if (MediaFile.isImageFileType(type.fileType)) return FileCategory.Picture;
            if (Util.sDocMimeTypesSet.contains(type.mimeType)) return FileCategory.Doc;
        }

        int dotPosition = path.lastIndexOf('.');
        if (dotPosition < 0) {
            return FileCategory.Other;
        }

        String ext = path.substring(dotPosition + 1);
        String APK_EXT = "apk";
        if (ext.equalsIgnoreCase(APK_EXT)) {
            return FileCategory.Apk;
        }
        String THEME_EXT = "mtz";
        if (ext.equalsIgnoreCase(THEME_EXT)) {
            return FileCategory.Theme;
        }

        if (matchExts(ext, ZIP_EXTS)) {
            return FileCategory.Zip;
        }

        return FileCategory.Other;
    }

    private static boolean matchExts(String ext, String[] exts) {
        for (String ex : exts) {
            if (ex.equalsIgnoreCase(ext))
                return true;
        }
        return false;
    }
}
