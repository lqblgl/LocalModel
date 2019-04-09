// Login.aidl
package com.wy.remote.remotemodel.login;
import com.wy.remote.remotemodel.login.LoginListener;
// Declare any non-default types here with import statements

interface Login {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void dologin(String name,String pass);
    void registerListener(LoginListener listener);
    void unregisterListener(LoginListener listener);
}
