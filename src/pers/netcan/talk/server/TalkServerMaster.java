package pers.netcan.talk.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Vector;
import pers.netcan.talk.common.TalkUser;

public class TalkServerMaster {
	private static ServerSocket server;
	public static int PORT = 2334;
	public static String Master = "Master";
	public static String WELCOME = "\n欢迎使用Talk，Talk是Netcan的第一个Java作品，项目已开源：https://github.com/netcan/Talk，欢迎Star！\n" +
						"个人博客：http://www.netcan666.com/";
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
