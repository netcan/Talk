/*************************************************************************
	> File Name: TalkUser.java
	> Author: Netcan
	> Blog: http://www.netcan666.com
	> Mail: 1469709759@qq.com
	> Created Time: 2016-12-14 16:50:15 CST
 ************************************************************************/

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
	public void sendAll(String sender, String msg) { // sender发送msg给此user
		message.add("[ALLFROM " + sender + "]" + msg); // [FROM SENDER]msg
	}
	public String getAMsg() { // 获得一条信息
		if(message.size() == 0) return null;
		return message.remove(0);
	}
	@Override
	public boolean equals(Object user) { // 判断用户是否相等，主要用来判断是否存在
		TalkUser u = (TalkUser)user;
		if(u == null || u.userName == null || this.userName == null) return false;
		return this.userName.equalsIgnoreCase(u.userName);
	}
}
