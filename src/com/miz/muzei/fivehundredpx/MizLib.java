package com.miz.muzei.fivehundredpx;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class MizLib {

	public static boolean isWifiConnected(Context c) {
		if (c!= null) {
			ConnectivityManager connManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo[] connections = connManager.getAllNetworkInfo();
			int count = connections.length;
			for (int i = 0; i < count; i++)
				if (connections[i]!= null && connections[i].getType() == ConnectivityManager.TYPE_WIFI && connections[i].isConnectedOrConnecting() ||
				connections[i]!= null &&  connections[i].getType() == ConnectivityManager.TYPE_ETHERNET && connections[i].isConnectedOrConnecting())
					return true;
		}
		return false;
	}
	
}
