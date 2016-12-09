package pers.netcan.talk.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import pers.netcan.talk.common.TalkUser;

public class TalkServerWorker extends Thread {
	private Socket worker;
	private int userId; // 用户id
	public TalkServerWorker(Socket socket) {
		this.worker = socket;
	}
	
	@Override
	public void run() {
		try {
			BufferedReader in  = new BufferedReader(new InputStreamReader(worker.getInputStream()));
			PrintWriter out = new PrintWriter(worker.getOutputStream());
			while(true) {
				/* TODO
				 * 这里完成聊天服务器相关请求
				 */
				System.out.println(register("netcan"));
			}
//			worker.close();
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public boolean register(String userName) { // 注册用户
		if(TalkServerMaster.Users.contains(new TalkUser(userName)))
			return false;
		TalkServerMaster.Users.add(new TalkUser(userName));
		userId = TalkServerMaster.Users.size() - 1;
		return true;
	}
}
