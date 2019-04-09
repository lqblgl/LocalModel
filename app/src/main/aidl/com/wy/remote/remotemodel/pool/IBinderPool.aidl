package com.wy.remote.remotemodel.pool;

import android.os.IBinder;

interface IBinderPool {

	IBinder queryBinder(int bindercode);
}
