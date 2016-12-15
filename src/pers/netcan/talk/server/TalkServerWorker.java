/*************************************************************************
	> File Name: TalkServerWorker.java
	> Author: Netcan
	> Blog: http://www.netcan666.com
	> Mail: 1469709759@qq.com
	> Created Time: 2016-12-14 16:50:33 CST
 ************************************************************************/

package pers.netcan.talk.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pers.netcan.talk.common.TalkUser;

public class TalkServerWorker extends Thread {
	private Socket worker;
	private String userName; // 用户id
	private BufferedReader in;
	private PrintWriter out;
	private static Vector<TalkUser> Users;
	private boolean logout;
	public TalkServerWorker(Socket socket) {
		this.worker = socket;
	}


	@Override
	public void run() {
		Users = TalkServerMaster.Users;
		try {
			in  = new BufferedReader(new InputStreamReader(worker.getInputStream()));
			out = new PrintWriter(worker.getOutputStream());
			while(true) {
				/* TODO
				 * 这里完成聊天服务器相关请求
				 */
				Thread.sleep(300);
				String cmd = in.readLine();
				if(cmd == null)  {
					logout();
					break;
				}
				execute(cmd);
				if(logout) break;
				showMsg();
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			logout();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logout();
		}

		try {
			in.close();
			out.close();
			worker.close();
			Thread.currentThread().interrupt();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private TalkUser thisUser() { // 返回当前用户
		if(this.userName == null) return null;
		return Users.get(Users.indexOf(new TalkUser(userName)));
	}

	public void showMsg() { // 显示当前用户收到的信息
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

	public void getUsrs() {
		String usrs = "[USERS]";
		for(TalkUser usr: Users) {
			if(!usr.equals(thisUser())) {
				usrs += usr.getName() + ",";
			}
		}
		out.println(usrs);
		out.flush();
	}

	public boolean send(String userName, String msg) { // 发送消息
		int toUserId;
		if((toUserId = Users.indexOf(new TalkUser(userName))) != -1) {
			if(Users.get(toUserId).getName().equalsIgnoreCase(TalkServerMaster.Master))  // 群聊
				for(int i=toUserId+1; i<Users.size(); ++i) { // 这里的要大括号，防止if和下一个else匹配！
					if(i != Users.indexOf(thisUser()))
						Users.get(i).sendAll(this.userName, msg); // 依次发送
				}
			else if(userName.equals(this.userName))
				Users.get(toUserId).sendMsg(TalkServerMaster.Master, msg);
			else
				Users.get(toUserId).sendMsg(this.userName, msg);
			return true;
		}
		else return false;
	}

	public void logout() {
		if(userName == null) {
			logout = true;
			return;
		}
		Users.remove(new TalkUser(userName));
		log(this.userName + " has logged out");
		this.userName = null;
		logout = true;
		return;
	}

	public void execute(String cmd) { // 执行动作，语法[ACTION]ARG
		String regex = "\\[([\\w\\s]+)\\](.*)"; // 匹配[action]arg
		if(cmd == null || ! cmd.matches(regex)) return;
//		log(String.format("command: \"%s\"", cmd));
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(cmd);
		m.find(); // 必须要find才能group。。。
		String action = m.group(1);
		String arg = m.group(2);

		if(action.equalsIgnoreCase("REGISTER")) { // [REGISTER]userName
			if(register(arg)) {
				out.println("[OK]");
				out.flush();

				// hello world!
				send(userName,
						Base64.getEncoder().encodeToString(TalkServerMaster.WELCOME.getBytes(StandardCharsets.UTF_8))
						);
				out.flush();
				log("A new user: "+arg+" has registered");
			}
			else {
				out.println("[FAILED]");
				out.flush();
				log("A user: "+arg+" maybe existed");
			}
		} else if(action.contains("SENDTO")) { // [SENDTO xx]MSG
			String toUser = action.substring("SENDTO".length() + 1, action.length());
			if(send(toUser, arg))
				log(String.format("%s send to %s a message: %s, success", thisUser().getName(), toUser, arg));
			else
				log(String.format("%s send to %s a message: %s, failed", thisUser().getName(), toUser, arg));
		} else if(action.contains("LOGOUT")) { // LOGOUT
			logout();
		} else if(action.contains("GETUSRS")) { // 在线用户
//			log("get users");
			getUsrs();
		}
	}

	private void log(String log) {
		System.out.printf("[%s]%s\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()), log);;
	}
}
