package com.ygcompany.zuojj.ymfilemanager.system;

import android.R.drawable;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.ygcompany.zuojj.ymfilemanager.MainActivity;
import com.ygcompany.zuojj.ymfilemanager.R;
import com.ygcompany.zuojj.ymfilemanager.utils.L;
import com.ygcompany.zuojj.ymfilemanager.utils.LocalCache;
import com.ygcompany.zuojj.ymfilemanager.utils.T;
import com.ygcompany.zuojj.ymfilemanager.view.SystemSpaceFragment;

import java.io.File;
import java.util.ArrayList;

public class FileViewInteractionHub implements FileOperationHelper.IOperationProgressListener {
    private static final String LOG_TAG = "FileViewInteractionHub";

    private IFileInteractionListener mFileViewListener;

    //被选中的item集合
    private ArrayList<FileInfo> mCheckedFileNameList = new ArrayList<>();

    private FileOperationHelper mFileOperationHelper;

    private FileSortHelper mFileSortHelper;

    private View mConfirmOperationBar;

    private ProgressDialog progressDialog;

    private View mNavigationBar;

    private TextView mNavigationBarText;

    private Context mContext;

    // 当前是复制还是移动
    private CopyOrMove copyOrMoveMode;

    public enum Mode {
        View, Pick
    }

    public enum CopyOrMove {
        Copy, Move
    }

    public FileViewInteractionHub(IFileInteractionListener fileViewListener) {
        assert (fileViewListener != null);
        mFileViewListener = fileViewListener;
        setup();
        mFileOperationHelper = new FileOperationHelper(this);
        mFileSortHelper = new FileSortHelper();
        mContext = mFileViewListener.getContext();
    }

    private void showProgress(String msg) {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage(msg);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void sortCurrentList() {
        mFileViewListener.sortCurrentList(mFileSortHelper);
    }

    public boolean canShowCheckBox() {
        return mConfirmOperationBar.getVisibility() != View.VISIBLE;
    }

    private void showConfirmOperationBar(boolean show) {
        mConfirmOperationBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void addContextMenuSelectedItem() {
        if (mCheckedFileNameList.size() == 0) {
            int pos = mListViewContextMenuSelectedItem;
            if (pos != -1) {
                FileInfo fileInfo = mFileViewListener.getItem(pos);
                if (fileInfo != null) {
                    mCheckedFileNameList.add(fileInfo);
                }
            }
        }
    }

    // 获取选中的文件列表
    public ArrayList<FileInfo> getSelectedFileList() {
        return mCheckedFileNameList;
    }

    // 获取checkbox选中的文件列表
    public ArrayList<FileInfo> getCheckedFileList() {
        return mFileOperationHelper.getFileList();
    }

    // 设置选中的文件列表
    public void setCheckedFileList(ArrayList<FileInfo> fileInfoList, CopyOrMove copyOrMove) {
        if (fileInfoList != null && fileInfoList.size() > 0)
            mCheckedFileNameList.addAll(fileInfoList);
        switch (copyOrMove) {
            case Move:
                onOperationMove();
                break;
            default:
            case Copy:
                onOperationCopy();
                break;
        }
    }

    public CopyOrMove getCurCopyOrMoveMode() {
        return copyOrMoveMode;
    }

    public boolean canPaste() {
        return mFileOperationHelper.canPaste();
    }

    // operation finish notification
    @Override
    public void onFinish() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        mFileViewListener.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showConfirmOperationBar(false);
                clearSelection();
                refreshFileList();
            }
        });
    }

    public FileInfo getItem(int pos) {
        return mFileViewListener.getItem(pos);
    }

    public boolean isInSelection() {
        return mCheckedFileNameList.size() > 0;
    }

    public boolean isMoveState() {
        return mFileOperationHelper.isMoveState() || mFileOperationHelper.canPaste();
    }

    private void setup() {
        //listview中operation事件集合（如：删除，移动...）
        setupFileListView();
        //确定取消按钮
        setupOperationPane();
        //顶部导航栏（sd卡路径）
        setupNaivgationBar();
    }

    private void setupNaivgationBar() {
        mNavigationBar = mFileViewListener.getViewById(R.id.navigation_bar);
        mNavigationBarText = (TextView) mFileViewListener.getViewById(R.id.current_path_view);
    }

    // buttons
    private void setupOperationPane() {
        mConfirmOperationBar = mFileViewListener.getViewById(R.id.moving_operation_bar);
        setupClick(mConfirmOperationBar, R.id.button_moving_confirm);
        setupClick(mConfirmOperationBar, R.id.button_moving_cancel);
    }

    //底部contextmenu显示时的点击事件
    private void setupClick(View v, int id) {
        View button = (v != null ? v.findViewById(id) : mFileViewListener.getViewById(id));
        if (button != null)
            button.setOnClickListener(buttonClick);
    }

    private View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_operation_copy://复制
                    onOperationCopy();
                    break;
                case R.id.button_operation_move://移动
                    onOperationMove();
                    break;
                case R.id.button_operation_send://发送
                    onOperationSend();
                    break;
                case R.id.button_operation_delete://删除
                    onOperationDelete();
                    break;
                case R.id.button_operation_cancel://取消
                    onOperationSelectAllOrCancel();
                    break;
                case R.id.button_moving_confirm://提交
                    onOperationButtonConfirm();
                    break;
                case R.id.button_moving_cancel://取消移动
                    onOperationButtonCancel();
                    break;
            }
        }

    };

    //刷新列表
    public void onOperationReferesh() {
        refreshFileList();
        T.showShort(mContext,"刷新成功！");
    }

    //启动设置页面
    private void onOperationSetting() {
        Intent intent = new Intent(mContext, FileManagerPreferenceActivity.class);
        if (null != intent) {
            try {
                mContext.startActivity(intent);
                clearSelection();
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to start setting: " + e.toString());
            }
        }
    }

    //设置系统文件的显示和隐藏
    public void onOperationShowSysFiles() {
        Settings.instance().setShowDotAndHiddenFiles(!Settings.instance().getShowDotAndHiddenFiles());
        refreshFileList();
    }

    //如果全选则取消，如果没有则全选
    public void onOperationSelectAllOrCancel() {
        if (!isSelectedAll()) {
            onOperationSelectAll();
        } else {
            clearSelection();
        }
    }

    public void onOperationSelectAll() { //全选
        mCheckedFileNameList.clear();
        for (FileInfo f : mFileViewListener.getAllFiles()) {
            f.Selected = true;
            mCheckedFileNameList.add(f);
        }
        mFileViewListener.onDataChanged();
    }

    //TODO  返回上一级
    public boolean onOperationUpLevel() {
        if (mFileViewListener.onOperation(Constants.OPERATION_UP_LEVEL)) {
            return true;
        }
        if (!mRoot.equals(mCurrentPath)) {
            mCurrentPath = new File(mCurrentPath).getParent();
            refreshFileList();
            return true;
        }
        return false;
    }

    // 创建文件夹时获取输入字符
    public void onOperationCreateFolder() {
        TextInputDialog dialog = new TextInputDialog(mContext, mContext.getString(
                R.string.operation_create_folder), mContext.getString(R.string.operation_create_folder_message),
                mContext.getString(R.string.new_folder_name), new TextInputDialog.OnFinishListener() {
            @Override
            public boolean onFinish(String text) {
                return doCreateFolder(text);
            }
        });

        dialog.show();
    }

    //TODO（待用） 创建文件时获取输入字符(创建文件不能在长按选中时创建)   右键创建
    public void onOperationCreateFile() {
        TextInputDialog dialog = new TextInputDialog(mContext, mContext.getString(
                R.string.operation_create_file), mContext.getString(R.string.operation_create_file_message),
                mContext.getString(R.string.new_file_name), new TextInputDialog.OnFinishListener() {
            @Override
            public boolean onFinish(String text) {
                return doCreateFile(text);
            }
        });

        dialog.show();
    }

    //着手创建文件夹
    private boolean doCreateFolder(String text) {
        if (TextUtils.isEmpty(text)) {
            clearSelection();
            return false;
        }

        if (mFileOperationHelper.CreateFolder(mCurrentPath, text)) {
            mFileViewListener.addSingleFile(Util.GetFileInfo(Util.makePath(mCurrentPath, text)));
            if ("list".equals(LocalCache.getInstance(mContext).getViewTag())) {
                mFileListView.setSelection(mFileListView.getCount() - 1);
            } else if ("grid".equals(LocalCache.getInstance(mContext).getViewTag())) {
                mFileGridView.setSelection(mFileGridView.getCount() - 1);
            }

            clearSelection();
        } else {
            new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.fail_to_create_folder))
                    .setPositiveButton(R.string.confirm, null).create().show();
            clearSelection();
            return false;
        }

        return true;
    }

    //着手创建文件
    private boolean doCreateFile(String text) {
        if (TextUtils.isEmpty(text)) {
            clearSelection();
            return false;
        }

        if (!TextUtils.isEmpty(text)) {
            mFileViewListener.addSingleFile(Util.GetFileInfo(Util.makePath(mCurrentPath, text)));
            clearSelection();
        } else {
            new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.fail_to_create_folder))
                    .setPositiveButton(R.string.confirm, null).create().show();
            clearSelection();
            return false;
        }

        return true;
    }

    public void onOperationSearch() {

    }

    public void onSortChanged(FileSortHelper.SortMethod s) {
        if (mFileSortHelper.getSortMethod() != s) {
            mFileSortHelper.setSortMethog(s);
            sortCurrentList();
        }
    }

    //复制操作
    public void onOperationCopy() {
        copyOrMoveMode = CopyOrMove.Copy;
        onOperationCopy(getSelectedFileList());
    }

    public void onOperationCopy(ArrayList<FileInfo> files) {
        mFileOperationHelper.Copy(files);
        clearSelection();

        showConfirmOperationBar(true);
        View confirmButton = mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
    }

    //复制路径操作
    public void onOperationCopyPath() {
        if (getSelectedFileList().size() == 1) {
            copy(getSelectedFileList().get(0).filePath);
        }
        clearSelection();
    }

    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) mContext.getSystemService(
                Context.CLIPBOARD_SERVICE);
        cm.setText(text);
    }

    private void onOperationPaste() {
        if (mFileOperationHelper.Paste(mCurrentPath)) {
            showProgress(mContext.getString(R.string.operation_pasting));
        }
    }

    //移动操作
    public void onOperationMove() {
        mFileOperationHelper.StartMove(getSelectedFileList());
        clearSelection();
        showConfirmOperationBar(true);
        View confirmButton = mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
        copyOrMoveMode = CopyOrMove.Move;
    }

    public void refreshFileList() {
        clearSelection();
        //更新导航栏
        updateNavigationPane();
        // onRefreshFileList returns true indicates list has changed
        mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);

        // update move operation button state
        updateConfirmButtons();
    }

    //更新选择那种方式的提交按钮显示
    private void updateConfirmButtons() {
        if (mConfirmOperationBar.getVisibility() == View.GONE)
            return;

        Button confirmButton = (Button) mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        int text = R.string.operation_paste;
        if (isSelectingFiles()) {
            confirmButton.setEnabled(mCheckedFileNameList.size() != 0);
            text = R.string.operation_send;
        } else if (isMoveState()) {
            confirmButton.setEnabled(mFileOperationHelper.canMove(mCurrentPath));
        }

        confirmButton.setText(text);
    }

    //更新导航栏
    private void updateNavigationPane() {
//        mNavigationBarText.setText(mFileViewListener.getDisplayPath(mCurrentPath));
        mNavigationBarText.setText(mFileViewListener.getDisplayPath(mCurrentPath));
    }

    //发送
    public void onOperationSend() {
        ArrayList<FileInfo> selectedFileList = getSelectedFileList();
        for (FileInfo f : selectedFileList) {
            if (f.IsDir) {
                AlertDialog dialog = new AlertDialog.Builder(mContext).setMessage(
                        R.string.error_info_cant_send_folder).setPositiveButton(R.string.confirm, null).create();
                dialog.show();
                return;
            }
        }

        Intent intent = IntentBuilder.buildSendFile(selectedFileList);
        if (intent != null) {
            try {
                mFileViewListener.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to view file: " + e.toString());
            }
        }
        clearSelection();
    }

    //重命名
    public void onOperationRename() {
        int pos = mListViewContextMenuSelectedItem;
        if (pos == -1)
            return;

        if (getSelectedFileList().size() == 0)
            return;

        final FileInfo f = getSelectedFileList().get(0);
        clearSelection();

        TextInputDialog dialog = new TextInputDialog(mContext, mContext.getString(R.string.operation_rename),
                mContext.getString(R.string.operation_rename_message), f.fileName, new TextInputDialog.OnFinishListener() {
            @Override
            public boolean onFinish(String text) {
                return doRename(f, text);
            }

        });

        dialog.show();
    }

    private boolean doRename(final FileInfo f, String text) {
        if (TextUtils.isEmpty(text))
            return false;

        if (mFileOperationHelper.Rename(f, text)) {
            f.fileName = text;
            mFileViewListener.onDataChanged();
        } else {
            new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.fail_to_rename))
                    .setPositiveButton(R.string.confirm, null).create().show();
            return false;
        }

        return true;
    }

    private void notifyFileSystemChanged(String path) {
        if (path == null)
            return;
        final File f = new File(path);
        if (Build.VERSION.SDK_INT >= 19 /*Build.VERSION_CODES.KITKAT*/) { //添加此判断，判断SDK版本是不是4.4或者高于4.4
            String[] paths = new String[]{Environment.getExternalStorageDirectory().toString()};
            paths = new String[]{path};
            MediaScannerConnection.scanFile(mContext, paths, null, null);
        } else {
            final Intent intent;
            if (f.isDirectory()) {
                intent = new Intent(Intent.ACTION_MEDIA_MOUNTED);
                intent.setClassName("com.android.providers.media", "com.android.providers.media.MediaScannerReceiver");
                intent.setData(Uri.fromFile(Environment.getExternalStorageDirectory()));
                Log.v(LOG_TAG, "directory changed, send broadcast:" + intent.toString());
            } else {
                intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(new File(path)));
                Log.v(LOG_TAG, "file changed, send broadcast:" + intent.toString());
            }
            mContext.sendBroadcast(intent);
        }
    }

    //删除操作
    public void onOperationDelete() {
        doOperationDelete(getSelectedFileList());
    }

    public void onOperationDelete(int position) {
        FileInfo file = mFileViewListener.getItem(position);
        if (file == null)
            return;

        ArrayList<FileInfo> selectedFileList = new ArrayList<FileInfo>();
        selectedFileList.add(file);
        doOperationDelete(selectedFileList);
    }

    private void doOperationDelete(final ArrayList<FileInfo> selectedFileList) {
        final ArrayList<FileInfo> selectedFiles = new ArrayList<>(selectedFileList);
        Dialog dialog = new AlertDialog.Builder(mContext)
                .setMessage(mContext.getString(R.string.operation_delete_confirm_message))
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mFileOperationHelper.Delete(selectedFiles)) {
                            showProgress(mContext.getString(R.string.operation_deleting));
                        }
                        clearSelection();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearSelection();
                    }
                }).create();
        dialog.show();
    }

    //显示当前文件的信息
    public void onOperationInfo() {
        if (getSelectedFileList().size() == 0)
            return;

        FileInfo file = getSelectedFileList().get(0);
        if (file == null)
            return;

        InformationDialog dialog = new InformationDialog(mContext, file, mFileViewListener
                .getFileIconHelper());
        dialog.show();
        clearSelection();
    }

    public void onOperationButtonConfirm() {
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(mCheckedFileNameList);
            mSelectFilesCallback = null;
            clearSelection();
        } else if (mFileOperationHelper.isMoveState()) {
            if (mFileOperationHelper.EndMove(mCurrentPath)) {
                showProgress(mContext.getString(R.string.operation_moving));
            }
        } else {
            onOperationPaste();
        }
    }

    //退出
    public void onOperationButtonCancel() {
        mFileOperationHelper.clear();
        showConfirmOperationBar(false);
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(null);
            mSelectFilesCallback = null;
            clearSelection();
        } else if (mFileOperationHelper.isMoveState()) {
            // refresh to show previously selected hidden files
            mFileOperationHelper.EndMove(null);
            refreshFileList();
        } else {
            refreshFileList();
        }
    }

    //中间context长按menu
    private OnCreateContextMenuListener mListViewContextMenuListener = new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (isInSelection() || isMoveState())
                return;
            clearSelection();

            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            //二级分类menu
            SubMenu sortMenu = menu.addSubMenu(0, MENU_SORT, 0, R.string.menu_item_sort).setIcon(
                    R.drawable.ic_menu_sort);
            addMenuItem(sortMenu, MENU_SORT_NAME, 0, R.string.menu_item_sort_name);
            addMenuItem(sortMenu, MENU_SORT_SIZE, 1, R.string.menu_item_sort_size);
            addMenuItem(sortMenu, MENU_SORT_DATE, 2, R.string.menu_item_sort_date);
            addMenuItem(sortMenu, MENU_SORT_TYPE, 3, R.string.menu_item_sort_type);
            sortMenu.setGroupCheckable(0, true, true);
            sortMenu.getItem(0).setChecked(true);

            //长按menu按钮
            addMenuItem(menu, Constants.MENU_NEW_FOLDER, 0, R.string.operation_create_folder);
            addMenuItem(menu, Constants.MENU_NEW_FILE, 0, R.string.operation_create_file);
            addMenuItem(menu, Constants.MENU_COPY, 0, R.string.operation_copy);
            addMenuItem(menu, Constants.MENU_COPY_PATH, 0, R.string.operation_copy_path);
            addMenuItem(menu, Constants.MENU_PASTE, 0,
                    R.string.operation_paste);
            addMenuItem(menu, Constants.MENU_MOVE, 0, R.string.operation_move);
            addMenuItem(menu, MENU_SEND, 0, R.string.operation_send);
            addMenuItem(menu, MENU_RENAME, 0, R.string.operation_rename);
            addMenuItem(menu, MENU_DELETE, 0, R.string.operation_delete);
            addMenuItem(menu, MENU_INFO, 0, R.string.operation_info);

            addMenuItem(menu, Constants.MENU_SHOWHIDE, 0, R.string.operation_show_sys,
                    R.drawable.ic_menu_show_sys);
            addMenuItem(menu, MENU_REFRESH, 0, R.string.operation_refresh,
                    R.drawable.ic_menu_refresh);
            addMenuItem(menu, MENU_SETTING, 0, R.string.menu_setting, drawable.ic_menu_preferences);
//            addMenuItem(menu, MENU_SELECTALL, 0, R.string.operation_selectall);
            addMenuItem(menu, MENU_EXIT, 8, R.string.menu_exit, drawable.ic_menu_close_clear_cancel);

            if (!canPaste()) {
                MenuItem menuItem = menu.findItem(Constants.MENU_PASTE);
                if (menuItem != null)
                    menuItem.setEnabled(false);
            }
        }
    };

    // File List view setup
    private GridView mFileGridView;
    private ListView mFileListView;

    private int mListViewContextMenuSelectedItem;

    private void setupFileListView() {
        final String title = LocalCache.getInstance(mContext).getViewTag();
        if ("list".equals(title)) {
            mFileListView = (ListView) mFileViewListener.getViewById(R.id.file_path_list);
            //设置list鼠标动作的事件监听
            mFileListView.setOnGenericMotionListener(new ListOnGenericMotionListener());
        } else if ("grid".equals(title)) {
            mFileGridView = (GridView) mFileViewListener.getViewById(R.id.file_path_grid);
            //设置grid鼠标动作的事件监听
            mFileGridView.setOnGenericMotionListener(new GridOnGenericMotionListener());
        }

    }

    // menu
    private static final int MENU_SEARCH = 1;
    private static final int MENU_SORT = 3;
    private static final int MENU_SEND = 7;
    private static final int MENU_RENAME = 8;
    private static final int MENU_DELETE = 9;
    private static final int MENU_INFO = 10;
    private static final int MENU_SORT_NAME = 11;
    private static final int MENU_SORT_SIZE = 12;
    private static final int MENU_SORT_DATE = 13;
    private static final int MENU_SORT_TYPE = 14;
    private static final int MENU_REFRESH = 15;
    private static final int MENU_SELECTALL = 16;
    private static final int MENU_SETTING = 17;
    private static final int MENU_EXIT = 18;

    //the bottom and context menu的点击事件
    private OnMenuItemClickListener menuItemClick = new OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {

            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            mListViewContextMenuSelectedItem = info != null ? info.position : -1;

            int itemId = item.getItemId();
            if (mFileViewListener.onOperation(itemId)) {
                return true;
            }

            addContextMenuSelectedItem();

            switch (itemId) {
                case MENU_SEARCH://搜索功能（待定）
                    onOperationSearch();
                    break;
                case Constants.MENU_NEW_FOLDER://创建文件夹
                    onOperationCreateFolder();
                    break;
                case MENU_REFRESH://刷新
                    onOperationReferesh();
                    break;
                case MENU_SELECTALL://全选（取消）
                    onOperationSelectAllOrCancel();
                    break;
                case Constants.MENU_SHOWHIDE: //显示隐藏文件或文件夹
                    onOperationShowSysFiles();
                    break;
                case MENU_SETTING://设置页面
                    onOperationSetting();
                    break;
                case MENU_EXIT://退出
                    ((MainActivity) mContext).finish();
                    break;
                case MENU_SORT_NAME:// sort分类二级选择目录
                    item.setChecked(true);
                    onSortChanged(FileSortHelper.SortMethod.name);
                    break;
                case MENU_SORT_SIZE: //大小
                    item.setChecked(true);
                    onSortChanged(FileSortHelper.SortMethod.size);
                    break;
                case MENU_SORT_DATE: //日期
                    item.setChecked(true);
                    onSortChanged(FileSortHelper.SortMethod.date);
                    break;
                case MENU_SORT_TYPE: //类型
                    item.setChecked(true);
                    onSortChanged(FileSortHelper.SortMethod.type);
                    break;
                case Constants.MENU_COPY://复制
                    onOperationCopy();
                    break;
                case Constants.MENU_COPY_PATH://路径复制
                    onOperationCopyPath();
                    break;
                case Constants.MENU_PASTE://粘贴
                    onOperationPaste();
                    break;
                case Constants.MENU_MOVE://移动
                    onOperationMove();
                    break;
                case MENU_SEND://发送
                    onOperationSend();
                    break;
                case MENU_RENAME://重命名
                    onOperationRename();
                    break;
                case MENU_DELETE://删除
                    onOperationDelete();
                    break;
                case MENU_INFO://属性
                    onOperationInfo();
                    break;
                default:
                    return false;
            }

            mListViewContextMenuSelectedItem = -1;
            return true;
        }

    };

    private FileViewInteractionHub.Mode mCurrentMode;

    private String mCurrentPath;

    private String mRoot;

    private SystemSpaceFragment.SelectFilesCallback mSelectFilesCallback;

//    //  创建list底部menu 设置返回true
//    public boolean onCreateOptionsMenu(Menu menu) {
//        return true;
//    }
//
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        updateMenuItems(menu);
//        return true;
//    }

//    private void updateMenuItems(Menu menu) {
//        menu.findItem(MENU_SELECTALL).setTitle(
//                isSelectedAll() ? R.string.operation_cancel_selectall : R.string.operation_selectall);
//        menu.findItem(MENU_SELECTALL).setEnabled(mCurrentMode != Mode.Pick);
//
//        MenuItem menuItem = menu.findItem(Constants.MENU_SHOWHIDE);
//        if (menuItem != null) {
//            menuItem.setTitle(Settings.instance().getShowDotAndHiddenFiles() ? R.string.operation_hide_sys
//                    : R.string.operation_show_sys);
//        }
//    }

    private void addMenuItem(Menu menu, int itemId, int order, int string) {
        addMenuItem(menu, itemId, order, string, -1);
    }

    private void addMenuItem(Menu menu, int itemId, int order, int string, int iconRes) {
        if (!mFileViewListener.shouldHideMenu(itemId)) {
            MenuItem item = menu.add(0, itemId, order, string).setOnMenuItemClickListener(menuItemClick);
            if (iconRes > 0) {
                item.setIcon(iconRes);
            }
        }
    }

    public boolean isFileSelected(String filePath) {
        return mFileOperationHelper.isFileSelected(filePath);
    }

    //listview或gridview的item左键点击事件监听
    public void onListItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo lFileInfo = mFileViewListener.getItem(position);
        if (lFileInfo == null) {
            Log.e(LOG_TAG, "file does not exist on position:" + position);
            return;
        }

        if (isInSelection()) {
            boolean selected = lFileInfo.Selected;
            if (selected) {
                mCheckedFileNameList.remove(lFileInfo);
            } else {
                mCheckedFileNameList.add(lFileInfo);
            }
            lFileInfo.Selected = !selected;
            return;
        }

        if (!lFileInfo.IsDir) {
            if (mCurrentMode == Mode.Pick) {
                mFileViewListener.onPick(lFileInfo);
            } else {
                viewFile(lFileInfo);
            }
            return;
        }

        mCurrentPath = getAbsoluteName(mCurrentPath, lFileInfo.fileName);
        refreshFileList();
    }

    //listview或gridview的item鼠标右键点击不做任何操作只刷新
    public void onListItemRightClick(AdapterView<?> parent, View view, int position, long id) {
        refreshFileList();
    }

    public void setMode(Mode m) {
        mCurrentMode = m;
    }

    public Mode getMode() {
        return mCurrentMode;
    }

    public void setRootPath(String path) {
        mRoot = path;
        mCurrentPath = path;
    }

    public String getRootPath() {
        return mRoot;
    }

    //获取当前路径
    public String getCurrentPath() {
        return mCurrentPath;
    }

    //设置当前路径
    public void setCurrentPath(String path) {
        mCurrentPath = path;
    }

    //获取绝对路径
    private String getAbsoluteName(String path, String name) {
        return path.equals(Constants.ROOT_PATH) ? path + name : path + File.separator + name;
    }

    // check or uncheck
    public boolean onCheckItem(FileInfo f, View v) {
        if (isMoveState())
            return false;

        if (isSelectingFiles() && f.IsDir)
            return false;

        if (f.Selected) {
            mCheckedFileNameList.add(f);
        } else {
            mCheckedFileNameList.remove(f);
        }
        return true;
    }

    private boolean isSelectingFiles() {
        return mSelectFilesCallback != null;
    }

    public boolean isSelectedAll() {  //      是否全部选中
        return mFileViewListener.getItemCount() != 0 && mCheckedFileNameList.size() == mFileViewListener.getItemCount();
    }

    //清空集合选中数据通知更新
    public void clearSelection() {
        if (mCheckedFileNameList.size() > 0) {
            for (FileInfo f : mCheckedFileNameList) {
                if (f == null) {
                    continue;
                }
                f.Selected = false;
            }
            mCheckedFileNameList.clear();
            mFileViewListener.onDataChanged();
        }
    }

    //查看文件
    private void viewFile(FileInfo lFileInfo) {
        try {
            IntentBuilder.viewFile(mContext, lFileInfo.filePath);
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "fail to view file: " + e.toString());
        }
    }

    //回退 如果全选则取消
    public boolean onBackPressed() {
        if (isInSelection()) {
            clearSelection();
        } else if (!onOperationUpLevel()) {
            return false;
        }
        return true;
    }

    //
    public void copyFile(ArrayList<FileInfo> files) {
        mFileOperationHelper.Copy(files);
    }

    public void moveFileFrom(ArrayList<FileInfo> files) {
        mFileOperationHelper.StartMove(files);
        showConfirmOperationBar(true);
        updateConfirmButtons();
        // refresh to hide selected files
        refreshFileList();
    }

    @Override
    public void onFileChanged(String path) {
        notifyFileSystemChanged(path);
    }

    public void startSelectFiles(SystemSpaceFragment.SelectFilesCallback callback) {
        mSelectFilesCallback = callback;
        showConfirmOperationBar(true);
        updateConfirmButtons();
    }

    private class ListOnGenericMotionListener implements View.OnGenericMotionListener {
        //根据MotionEvent事件来判断鼠标的操作执行对应的动作
        @Override
        public boolean onGenericMotion(View view, MotionEvent event) {
            switch (event.getButtonState()) {
                case MotionEvent.BUTTON_PRIMARY:   // BUTTON_PRIMARY鼠标左键点击
                    mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            onListItemClick(parent, view, position, id);
                        }
                    });
//                    T.showShort(mContext, "鼠标左键单击事件");
                    break;
                case MotionEvent.BUTTON_SECONDARY:    //BUTTON_SECONDARY鼠标右键点击
                    //点击鼠标右键且让点击事件不起作用只弹出contextmenu
//                    mFileListView.setLongClickable(true);
                    mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            onListItemRightClick(parent, view, position, id);
                        }
                    });
                    mFileListView.setOnCreateContextMenuListener(mListViewContextMenuListener);
//                    T.showShort(mContext, "鼠标右键单击事件");
                    break;
                case MotionEvent.BUTTON_TERTIARY:   //BUTTON_TERTIARY鼠标滚轮点击
                    mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            onListItemClick(parent, view, position, id);
                        }
                    });
//                    T.showShort(mContext, "鼠标滑轮单击事件");
                    break;
                case MotionEvent.ACTION_SCROLL:   //ACTION_SCROLL鼠标滚轮滑动
                    MouseScrollAction(event);
                    break;
            }
            return false;
        }
    }

    private class GridOnGenericMotionListener implements View.OnGenericMotionListener {
        //根据MotionEvent事件来判断鼠标的操作执行对应的动作
        @Override
        public boolean onGenericMotion(View view, MotionEvent event) {
            switch (event.getButtonState()) {
                case MotionEvent.BUTTON_PRIMARY:   // BUTTON_PRIMARY鼠标左键点击
                    mFileGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            onListItemClick(parent, view, position, id);
                        }
                    });
                    break;
                case MotionEvent.BUTTON_SECONDARY:    //BUTTON_SECONDARY鼠标右键点击
                    //点击鼠标右键且让点击事件不起作用只弹出contextmenu
//                    mFileGridView.setLongClickable(true);
                    mFileGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            onListItemRightClick(parent, view, position, id);
                        }
                    });
                    mFileGridView.setOnCreateContextMenuListener(mListViewContextMenuListener);
                    break;
                case MotionEvent.BUTTON_TERTIARY:   //BUTTON_TERTIARY鼠标滚轮点击
                    mFileGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            onListItemClick(parent, view, position, id);
                        }
                    });
                    break;
                case MotionEvent.ACTION_SCROLL:   //ACTION_SCROLL鼠标滚轮滑动
                    MouseScrollAction(event);
                    break;
            }
            return false;
        }
    }

    //鼠标滚轮滑动
    private void MouseScrollAction(MotionEvent event) {
        if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
            L.i("fortest::onGenericMotionEvent", "down");
            T.showShort(mContext, "向下滚动...");
        }
        //获得垂直坐标上的滚动方向,也就是滚轮向上滚
        else {
            L.i("fortest::onGenericMotionEvent", "up");
            T.showShort(mContext, "向上滚动...");
        }
    }
}