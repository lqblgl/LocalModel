package com.wy.local.localmodel;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.wy.remote.remotemodel.login.Login;
import com.wy.remote.remotemodel.login.LoginListener;
import com.wy.remote.remotemodel.pool.ICompute;
import com.wy.remote.remotemodel.pool.ISecurityCenter;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Button search_user_btn, login_btn, register_btn, start_service_btn, start_launch_btn;
    private EditText name_edt, pass_edt;
    private Login mLogin;
    private boolean isBind = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        name_edt = findViewById(R.id.name_edt);
        pass_edt = findViewById(R.id.pass_edt);
        search_user_btn = findViewById(R.id.search_user_btn);
        login_btn = findViewById(R.id.login_btn);
        register_btn = findViewById(R.id.register_btn);
        start_service_btn = findViewById(R.id.start_service_btn);
        start_launch_btn = findViewById(R.id.start_launch_btn);
        search_user_btn.setOnClickListener(this);
        login_btn.setOnClickListener(this);
        register_btn.setOnClickListener(this);
        start_service_btn.setOnClickListener(this);
        start_launch_btn.setOnClickListener(this);
        getContentResolver().registerContentObserver(Uri.parse("content://com.wy.remote.remotemodel/user"), true, contentObserver);
        Intent intent = new Intent("com.wy.remote.remotemodel.remote.service");
        intent.setPackage("com.wy.remote.remotemodel");
        isBind = bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != contentObserver) {
            getContentResolver().unregisterContentObserver(contentObserver);
        }
        if (null != mLogin && mLogin.asBinder().isBinderAlive()) {
            try {
                mLogin.unregisterListener(loginListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (isBind) {
            if (null != connection) {
                unbindService(connection);
                isBind = false;
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Login login = Login.Stub.asInterface(service);
            mLogin = login;
            try {
                mLogin.registerListener(loginListener);
                mLogin.asBinder().linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private LoginListener loginListener = new LoginListener.Stub() {
        @Override
        public void success() throws RemoteException {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "登陆成功", Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void fail() throws RemoteException {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "登陆失败", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (null == mLogin) {
                return;
            }
            mLogin.asBinder().unlinkToDeath(deathRecipient, 0);
            mLogin = null;
            Intent intent = new Intent("com.wy.remote.remotemodel.remote.service");
            intent.setPackage("com.wy.remote.remotemodel");
            isBind = bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    };

    private ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.e(TAG, "uri有新数据产生" + selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Log.e(TAG, "uri有新数据产生" + selfChange + "---" + uri);
        }
    };

    @Override
    public void onClick(View v) {
        Cursor userCursor = getContentResolver().query(Uri.parse("content://com.wy.remote.remotemodel/user"), new String[]{"NAME", "PASS", "PHONE"}, null, null, null);
        switch (v.getId()) {
            case R.id.search_user_btn:
                while (userCursor.moveToNext()) {
                    Log.e(TAG, "query user:" + userCursor.getString(userCursor.getColumnIndex("NAME")) + "---" + userCursor.getString(userCursor.getColumnIndex("PASS")) + "---" + userCursor.getLong(userCursor.getColumnIndex("PHONE")));
                }
                userCursor.close();
                Bundle bundle = new Bundle();
                bundle.putBoolean("method", true);
                Bundle call = getContentResolver().call(Uri.parse("content://com.wy.remote.remotemodel/user"), "method", "arg", bundle);
                Log.e(TAG, "call: " + call.getBoolean("method"));
                break;
            case R.id.login_btn:
                if (TextUtils.isEmpty(name_edt.getText().toString()) || TextUtils.isEmpty(pass_edt.getText().toString())) {
                    Toast.makeText(this, "请输入完整的用户数据", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    mLogin.dologin(name_edt.getText().toString(), pass_edt.getText().toString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.register_btn:
                if (TextUtils.isEmpty(name_edt.getText().toString()) || TextUtils.isEmpty(pass_edt.getText().toString())) {
                    Toast.makeText(this, "请输入完整的用户数据", Toast.LENGTH_LONG).show();
                    return;
                }
                ContentValues values = new ContentValues();
                values.put("NAME", name_edt.getText().toString());
                values.put("PASS", pass_edt.getText().toString());
                getContentResolver().insert(Uri.parse("content://com.wy.remote.remotemodel/user"), values);
                name_edt.setText("");
                pass_edt.setText("");
                break;
            case R.id.start_service_btn:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BinderPool instance = BinderPool.getInstance(MainActivity.this);
                        IBinder iBinder = instance.queryBinder(BinderPool.BINDER_SECURITY_CENTER);
                        ISecurityCenter iSecurityCenter = ISecurityCenter.Stub.asInterface(iBinder);
                        try {
                            Log.e(TAG, "onCreate: " + iSecurityCenter.decrypt("name password"));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        iBinder = instance.queryBinder(BinderPool.BINDER_COMPUTE);
                        ICompute iCompute = ICompute.Stub.asInterface(iBinder);
                        try {
                            Log.e(TAG, "onCreate: " + iCompute.add(21, 2));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.start_launch_btn:
//                PackageManager packageManager = getPackageManager();
//                if (checkPackInfo("com.wy.remote.remotemodel")) {
//                    Intent intent = packageManager.getLaunchIntentForPackage("com.wy.remote.remotemodel");
//                    startActivity(intent);
//                } else {
//                    Toast.makeText(MainActivity.this, "没有安装" + "com.wy.remote.remotemodel", Toast.LENGTH_LONG).show();
//                }
                Intent intent = new Intent();
                //第一种方式
                ComponentName cn = new ComponentName("com.wy.remote.remotemodel", "com.wy.remote.remotemodel.StartByLocalActivity");
                try {
                    intent.setComponent(cn);
                    //第二种方式
                    //intent.setClassName("com.example.fm", "com.example.fm.MainFragmentActivity");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    //TODO  可以在这里提示用户没有安装应用或找不到指定Activity，或者是做其他的操作
                }
                break;
        }
    }

    /**
     * 检查包是否存在
     *
     * @param packname
     * @return
     */
    private boolean checkPackInfo(String packname) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(packname, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo != null;
    }
}
