package pers.netcan.talk.common;

import java.util.Vector;

public class TalkUser {
	private String userName; // 用户名
	private Vector<String> message; // 消息队列

	public TalkUser(String name) {
		// TODO Auto-generated constructor stub
		message = new Vector<String>();
		this.userName = name;
	}
	public String getName() {
		return userName;
	}
	public void sendMsg(String sender, String msg) { // sender发送msg给此user
		message.add("[FROM " + sender + "]" + msg); // [FROM SENDER]msg
	}
	@Override
	public boolean equals(Object user) { // 判断用户是否相等，主要用来判断是否存在
		TalkUser u = (TalkUser)user;
		return this.userName.equalsIgnoreCase(u.userName);
	}
}
