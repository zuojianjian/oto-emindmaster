package com.emindsoft.openthos.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.emindsoft.openthos.BaseFragment;
import com.emindsoft.openthos.R;
import com.emindsoft.openthos.adapter.GroupAdapter;
import com.emindsoft.openthos.bean.ImageBean;
import com.emindsoft.openthos.system.Util;
import com.emindsoft.openthos.view.DetailFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * 显示图片集合缩略图页面
 * Created by zuojj on 16-5-18.
 */
public class PictrueFragment extends BaseFragment {

    private static final int SCAN_OK = 1;

    private GridView gv_pictrue;
    private TextView tv_no_pictrue;

    private GroupAdapter adapter;
    private ProgressDialog mProgressDialog;

    //图片分类封装集合
    private ArrayList<ImageBean> list = new ArrayList<>();
    private HashMap<String, List<String>> mGruopMap = new HashMap<>();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SCAN_OK:
                    //关闭进度条
                    mProgressDialog.dismiss();
                    list = subGroupOfImage(mGruopMap);
                    if (null != list) {
                        adapter = new GroupAdapter(getActivity(), list, gv_pictrue);
                    } else {
                        tv_no_pictrue.setVisibility(View.VISIBLE);
                    }
                    if (adapter != null) {
                        gv_pictrue.setAdapter(adapter);
                    }
                    mHandler.removeCallbacksAndMessages(null);
                    break;
            }
        }

    };
    private FragmentManager manager = getFragmentManager();
    private ContentResolver mContentResolver;

    @SuppressLint({"NewApi", "ValidFragment"})
    public PictrueFragment(FragmentManager manager) {
        this.manager = manager;
    }
    @SuppressLint({"NewApi", "ValidFragment"})
    public PictrueFragment() {
        super();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pictrue_fragment_layout, container, false);
        initView(view);
        initData();
        return view;
    }

    private void initView(View view) {
        gv_pictrue = (GridView) view.findViewById(R.id.gv_pictrue);
        tv_no_pictrue = (TextView) view.findViewById(R.id.tv_no_pictrue);
    }

    private void initData() {
        //清空上一次集合数据，防止数据重复加载
        if (null != list) {
            list.clear();
        }
        //查询手机中的图片数据，并封装进list集合中
        getImages();
        //文件夹点击显示全部图片
        gv_pictrue.setOnItemClickListener(new FolderOnItemClickListener());
    }

    private class FolderOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            //将fragment添加到BackStack
            DetailFragment fragment = new DetailFragment(mGruopMap, list, i);
            manager.beginTransaction().replace(R.id.fl_mian, fragment)
                    .addToBackStack(null).commit();//主页面
        }
    }

    /**
     * 组装分组界面GridView的数据源，因为我们扫描手机的时候将图片信息放在HashMap中
     * 所以需要遍历HashMap将数据组装成List
     *
     * @param mGruopMap
     * @return
     */
    private ArrayList<ImageBean> subGroupOfImage(HashMap<String, List<String>> mGruopMap) {
        if (mGruopMap.size() == 0) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : mGruopMap.entrySet()) {
            ImageBean mImageBean = new ImageBean();
            String key = entry.getKey();
            List<String> value = entry.getValue();

            mImageBean.setFolderName(key);
            mImageBean.setImageCounts(value.size());
            mImageBean.setTopImagePath(value.get(0));//获取该组的第一张图片
            list.add(mImageBean);
        }
        return list;
    }

    /**
     * 利用ContentProvider扫描手机中的图片数据，此方法在运行在子线程中
     */
    private void getImages() {
        if (!Util.isSDCardReady()) {
            Toast.makeText(getActivity(), "暂无外部存储", LENGTH_SHORT).show();
            return;
        }

        mContentResolver = getActivity().getContentResolver();

        if (mContentResolver != null) {
            //显示进度条
            mProgressDialog = ProgressDialog.show(getActivity(), null, "正在加载...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    //只查询jpeg和png的图片
                    Cursor mCursor = mContentResolver.query(mImageUri, null,
                            MediaStore.Images.Media.MIME_TYPE + "=? or "
                                    + MediaStore.Images.Media.MIME_TYPE + "=?",
                            new String[]{"image/jpeg", "image/png"},
                            MediaStore.Images.Media.DATE_MODIFIED);

                    assert mCursor != null;
                    while (mCursor.moveToNext()) {
                        //获取图片的路径
                        String path = mCursor.getString(mCursor
                                .getColumnIndex(MediaStore.Images.Media.DATA));
                        //获取该图片的父路径名
                        String parentName = new File(path).getParentFile().getName();
                        //根据父路径名将图片放入到不同的mGruopMap中
                        if (!mGruopMap.containsKey(parentName)) {
                            List<String> chileList = new ArrayList<>();
                            chileList.add(path);
                            mGruopMap.put(parentName, chileList);
                        } else {
                            mGruopMap.get(parentName).add(path);
                        }
                    }
                    mCursor.close();
                    //通知Handler扫描图片完成
                    mHandler.sendEmptyMessage(SCAN_OK);

                }
            }).start();
        }

    }

    /**
     * 当前是否可以发生回退操作
     *
     * @return
     */
    public boolean canGoBack() {
        return false;
    }


    /*执行回退操作*/
    public void goBack() {
    }
}
