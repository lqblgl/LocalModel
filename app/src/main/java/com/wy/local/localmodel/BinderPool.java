package com.wy.local.localmodel;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.wy.remote.remotemodel.pool.IBinderPool;

import java.util.concurrent.CountDownLatch;

/**
 * 版本：V1.2.5
 * 时间： 2018/5/24 10:18
 * 创建人：laoqb
 * 作用：
 */
public class BinderPool {
    private static final String TAG = "BinderPool";
    public static final int BINDER_COMPUTE = 0;
    public static final int BINDER_SECURITY_CENTER = 1;
    private Context mContext;
    private IBinderPool mBinderPool;
    private static volatile BinderPool mInstance;
    private CountDownLatch countDownLatch;

    private BinderPool(Context context) {
        this.mContext = context.getApplicationContext();
        connectBinderPoolService();
    }

    public static BinderPool getInstance(Context context) {
        if (null == mInstance) {
            synchronized (BinderPool.class) {
                if (null == mInstance) {
                    mInstance = new BinderPool(context);
                }
            }
        }
        return mInstance;
    }

    private synchronized void connectBinderPoolService() {
        countDownLatch = new CountDownLatch(1);
        Intent intent = new Intent("com.wy.remote.remotemodel.pool.service");
        intent.setPackage("com.wy.remote.remotemodel");
        mContext.bindService(intent, mBinderPoolConnection, Context.BIND_AUTO_CREATE);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public IBinder queryBinder(int code) {
        IBinder iBinder = null;
        if (null != mBinderPool) {
            try {
                iBinder = mBinderPool.queryBinder(code);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return iBinder;
    }

    private ServiceConnection mBinderPoolConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinderPool = IBinderPool.Stub.asInterface(service);
            try {
                mBinderPool.asBinder().linkToDeath(mBinderPoolDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            countDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private IBinder.DeathRecipient mBinderPoolDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            mBinderPool.asBinder().unlinkToDeath(mBinderPoolDeathRecipient, 0);
            mBinderPool = null;
            connectBinderPoolService();
        }
    };

}
