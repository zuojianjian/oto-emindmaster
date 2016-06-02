package com.ygcompany.zuojj.ymfilemanager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ygcompany.zuojj.ymfilemanager.component.PopWinShare;
import com.ygcompany.zuojj.ymfilemanager.fragment.DeskFragment;
import com.ygcompany.zuojj.ymfilemanager.fragment.MusicFragment;
import com.ygcompany.zuojj.ymfilemanager.fragment.OnlineNeighborFragment;
import com.ygcompany.zuojj.ymfilemanager.fragment.PersonalStorageFragment;
import com.ygcompany.zuojj.ymfilemanager.fragment.PictrueFragment;
import com.ygcompany.zuojj.ymfilemanager.fragment.SdStorageFragment;
import com.ygcompany.zuojj.ymfilemanager.fragment.VideoFragment;
import com.ygcompany.zuojj.ymfilemanager.utils.DisplayUtil;
import com.ygcompany.zuojj.ymfilemanager.utils.T;
import com.ygcompany.zuojj.ymfilemanager.view.SystemSpaceFragment;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    FragmentManager manager = getSupportFragmentManager();
    //初始化控件
    @Bind(R.id.fl_mian)
    FrameLayout fl_mian;
    @Bind(R.id.tv_desk)
    LinearLayout tv_desk;
    @Bind(R.id.tv_music)
    LinearLayout tv_music;
    @Bind(R.id.tv_video)
    LinearLayout tv_video;
    @Bind(R.id.tv_picture)
    LinearLayout tv_picture;
    @Bind(R.id.tv_android)
    LinearLayout tv_android;
    @Bind(R.id.tv_storage)
    LinearLayout tv_storage;
    @Bind(R.id.tv_net_service)
    LinearLayout tv_net_service;
    @Bind(R.id.iv_back)
    ImageView iv_back;
    @Bind(R.id.iv_menu)
    ImageView iv_menu;

    private PopWinShare popWinShare;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(MainActivity.this);
        initView();
    }

    private void initView() {
        manager.beginTransaction().replace(R.id.fl_mian, new SdStorageFragment(manager)).commit();
        //设置焦点监听
        tv_desk.setOnClickListener(this);
        tv_music.setOnClickListener(this);
        tv_video.setOnClickListener(this);
        tv_picture.setOnClickListener(this);
        tv_android.setOnClickListener(this);
        iv_back.setOnClickListener(this);
        iv_menu.setOnClickListener(this);
        tv_storage.setOnClickListener(this);
        tv_net_service.setOnClickListener(this);
    }

    @Override
    public void onClick(View view){
        switch(view.getId()){
            case R.id.tv_desk:
                manager.popBackStack();
                manager.beginTransaction().replace(R.id.fl_mian, new DeskFragment()).commit();
                break;
            case R.id.tv_music:
                manager.beginTransaction().replace(R.id.fl_mian, new MusicFragment()).commit();
                break;
            case R.id.tv_video:
                manager.beginTransaction().replace(R.id.fl_mian, new VideoFragment()).commit();
                break;
            case R.id.tv_picture:
                manager.beginTransaction().replace(R.id.fl_mian, new PictrueFragment()).commit();
                break;
            case R.id.tv_android:
                manager.beginTransaction().replace(R.id.fl_mian, new SdStorageFragment(manager),"android").commit();
                break;
            case R.id.tv_storage:
                manager.beginTransaction().replace(R.id.fl_mian, new PersonalStorageFragment()).commit();
                break;
            case R.id.tv_net_service:
                manager.beginTransaction().replace(R.id.fl_mian, new OnlineNeighborFragment()).commit();
                break;
            case R.id.iv_back:
                onBackPressed();
                break;
            case R.id.iv_menu:
                T.showShort(MainActivity.this,"iv_menu");
                shownPopWidndow();
                break;
        }
    }

    private void shownPopWidndow() {
        if (popWinShare == null) {
            //自定义的单击事件
            OnClickLintener paramOnClickListener = new OnClickLintener();
            popWinShare = new PopWinShare(MainActivity.this, paramOnClickListener,
                    DisplayUtil.dip2px(MainActivity.this, 120), DisplayUtil.dip2px(MainActivity.this, 160));
            //监听窗口的焦点事件，点击窗口外面则取消显示
            popWinShare.getContentView().setOnFocusChangeListener(new View.OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        popWinShare.dismiss();
                    }
                }
            });
        }
        //设置默认获取焦点
        popWinShare.setFocusable(true);
        //以某个控件的x和y的偏移量位置开始显示窗口
        popWinShare.showAsDropDown(iv_menu, 10, 10);
        //如果窗口存在，则更新
        popWinShare.update();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentById(R.id.fl_mian);

        if(fm.getBackStackEntryCount() >= 1){
            fm.popBackStack();
        } else{
            finish();
        }
    }

    /**
     * 得到SystemSpaceFragment实例
     * @return
     */
    public SystemSpaceFragment getSystemSpaceFragment(){
        SystemSpaceFragment fragment = (SystemSpaceFragment) manager.findFragmentByTag("system_space_fragment");
        return fragment;
    }

    private class OnClickLintener implements View.OnClickListener {
        @Override
        public void onClick(View view) {

        }
    }
}


