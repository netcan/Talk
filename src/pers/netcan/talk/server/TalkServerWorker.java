package pers.netcan.talk.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pers.netcan.talk.common.TalkUser;

public class TalkServerWorker extends Thread {
	private Socket worker;
	private String userName; // 用户id
	private boolean logout;
	private static Vector<TalkUser> Users;
	public TalkServerWorker(Socket socket) {
		this.worker = socket;
	}
	
	@Override
	public void run() {
		Users = TalkServerMaster.Users;
		logout = false;
		try {
			BufferedReader in  = new BufferedReader(new InputStreamReader(worker.getInputStream()));
			PrintWriter out = new PrintWriter(worker.getOutputStream());
			while(true) {
				/* TODO
				 * 这里完成聊天服务器相关请求
				 */
				if(logout) break;
				String cmd = in.readLine();
				execute(cmd);
				showMsg(out);
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		try {
			worker.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private TalkUser thisUser() { // 返回当前用户
		if(this.userName == null) return null;
		return Users.get(Users.indexOf(new TalkUser(userName)));
	}
	
	public void showMsg(PrintWriter out) { // 显示当前用户收到的信息
		if(userName == null) return;
		String msg;
		while((msg = thisUser().getAMsg()) != null) {
			out.println(msg);
			out.flush();
		}
	}
	
	public boolean register(String userName) { // 注册用户
		if(this.userName != null)  return false; // 已注册过
		if(userName == null || Users.contains(new TalkUser(userName)))
			return false;
		this.userName = userName;
		Users.add(new TalkUser(userName));
		return true;
	}

	public boolean send(String userName, String msg) { // 发送消息
		int toUserId;
		if((toUserId = Users.indexOf(new TalkUser(userName))) != -1) {
			if(Users.get(toUserId).getName().equalsIgnoreCase("Master"))  // 群聊
				for(int i=toUserId+1; i<Users.size(); ++i) { // 这里的要大括号，防止if和下一个else匹配！
					if(i != Users.indexOf(thisUser())) 
						Users.get(i).senAll(this.userName, msg); // 依次发送
				}
			else Users.get(toUserId).sendMsg(this.userName, msg);
			return true;
		}
		else return false;
	}

	public void logout() {
		Users.remove(new TalkUser(userName));
		this.userName = null;
		logout = true;
	}

	public void execute(String cmd) { // 执行动作，语法[ACTION]ARG
		String regex = "\\[([\\w\\s]+)\\](.*)"; // 匹配[action]arg
		if(cmd == null || ! cmd.matches(regex)) return;
		log(String.format("command: \"%s\"", cmd));
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(cmd);
		m.find(); // 必须要find才能group。。。
		String action = m.group(1);
		String arg = m.group(2);

		if(action.equalsIgnoreCase("REGISTER")) { // [REGISTER]userName
			if(register(arg))
				log("A new user: "+arg+" has registered");
			else
				log("A user: "+arg+" maybe existed");
		} else if(action.contains("SENDTO")) { // [SENDTO xx]MSG
			String toUser = action.substring("SENDTO".length() + 1, action.length());
			if(send(toUser, arg))
				log(String.format("%s send to %s a message: %s, success", thisUser().getName(), toUser, arg));
			else
				log(String.format("%s send to %s a message: %s, failed", thisUser().getName(), toUser, arg));
		} else if(action.contains("LOGOUT")) { // LOGOUT
			logout();
		}
	}
	
	private void log(String log) {
		System.out.printf("[%s]%s\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()), log);;
	}
}
