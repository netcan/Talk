package pers.netcan.talk.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Vector;
import pers.netcan.talk.common.TalkUser;

public class TalkServerMaster {
	private static ServerSocket server;
	public static int PORT = 2334;
	public static String Master = "Master";
	static Vector<TalkUser> Users; // 所有用户

	public void boot() {
		Users = new Vector<TalkUser>();
		Users.add(new TalkUser(Master));
		try {
			server = new ServerSocket(PORT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(true) {
			TalkServerWorker worker = null;
			try {
				worker = new TalkServerWorker(server.accept());
				worker.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
