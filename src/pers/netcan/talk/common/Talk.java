package pers.netcan.talk.common;

import javafx.application.Application;
import pers.netcan.talk.client.TalkClient;
import pers.netcan.talk.server.*;

public class Talk {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length > 0 && args[0].equals("--server")) {
			TalkServerMaster server = new TalkServerMaster();
			server.boot();
		} else {
			Application.launch(TalkClient.class, args); // 启动方法比较特殊= =
		}
	}

}
