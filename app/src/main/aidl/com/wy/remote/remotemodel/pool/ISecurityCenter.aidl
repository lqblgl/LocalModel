package com.wy.remote.remotemodel.pool;

interface ISecurityCenter {

	String encrypt(String content);
	String decrypt(String password);
}
