package com.emindsoft.openthos;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.emindsoft.openthos.component.AppManager;

/**
 * activity基类
 * Created by zuojj on 16-5-18.
 */
public abstract class BaseActivity extends FragmentActivity {


    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //添加activity到堆栈
        AppManager.getAppManager().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 结束Activity从堆栈中移除
        AppManager.getAppManager().finishActivity(this);
    }

    //顶部导航栏显示的路径
    public abstract void setNavigationBar(String displayPath);
}
