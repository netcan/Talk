/*************************************************************************
	> File Name: TalkClient.java
	> Author: Netcan
	> Blog: http://www.netcan666.com
	> Mail: 1469709759@qq.com
	> Created Time: 2016-12-14 16:49:54 CST
 ************************************************************************/

package pers.netcan.talk.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import pers.netcan.talk.common.TalkEmoji;
import pers.netcan.talk.server.TalkServerMaster;

public class TalkClient extends Application  {
	private static Socket client;
	private static BufferedReader in;
	private static PrintWriter out;
	private static Stage pStage; // 主窗口
	private static Stage emojiStage; // 表情窗口
	private static String VERSION = "0.4";
	private static String talkRecordDir = "TalkRecords";
	private ObservableList<String> usrsList;
	private Map<String, String> usrsMsg; // 保存信息
	private Map<String, Boolean> usrsMsgNotify; // 消息提示
	private TextArea message, sendMsg; // 消息框
	private ListView<String> usrsListView;
	private boolean messageGotoEndLine; // 切换消息滚到最后一行
	String ip = "", usrName = "";

	private void loginScene() throws IOException {
		File confFile = new File("Talk.conf");

		if(!confFile.exists())
			confFile.createNewFile();
		else {
			FileReader fr = new FileReader(confFile);
			BufferedReader br = new BufferedReader(fr);
			String line;
			String ipRegex = "IP = (.*)";
			String usrNameRegex = "USERNAME = (.*)";
			while((line = br.readLine()) != null) {
				if(Pattern.matches(ipRegex, line)) {
					Matcher m = Pattern.compile(ipRegex).matcher(line);
					m.find();
					ip = m.group(1);
				} else if(Pattern.matches(usrNameRegex, line)) {
					Matcher m = Pattern.compile(usrNameRegex).matcher(line);
					m.find();
					usrName = m.group(1);
				}
			}
			br.close();
			fr.close();
		}
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text scenetitle = new Text("登录");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

        Label ipAddr = new Label("ip地址:");
        grid.add(ipAddr, 0, 1);

        TextField ipAddrField = new TextField();
        ipAddrField.setText(ip);
        grid.add(ipAddrField, 1, 1);

        Label userName = new Label("用户名：");
        grid.add(userName, 0, 2);

        TextField userNameTextField = new TextField();
        userNameTextField.setText(usrName);
        grid.add(userNameTextField, 1, 2);


        Button btn = new Button("登录");
        HBox hbBtn = new HBox(0);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);

        Scene scene = new Scene(grid, 320, 240);

		pStage.setScene(scene);
		pStage.show();

		btn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				Alert alert = new Alert(AlertType.ERROR);
				if(!Pattern.matches("\\w+", userNameTextField.getText())) {
					alert.setContentText("请检查名字!");
					alert.showAndWait();
					return;
				}
				if(!Pattern.matches(
						"([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}",
						ipAddrField.getText()))  {
					alert.setContentText("请检查ip地址！");
					alert.showAndWait();
					return;
				}

				// write to config file
				FileWriter fw;
				BufferedWriter bw;
				try {
					fw = new FileWriter(confFile);
					bw = new BufferedWriter(fw);
					bw.write("IP = " + ipAddrField.getText() + '\n');
					bw.write("USERNAME = " + userNameTextField.getText() + '\n');
					bw.close();
					fw.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				ip = ipAddrField.getText();
				usrName = userNameTextField.getText();
				if(checkLogin()) { // 登录成功
					grid.getChildren().clear();
					talkScene();
				} else {
					alert.setContentText("登录失败！");
					alert.showAndWait();
				}

			}
		});
	}

	public void getUsrs(String usrsList) { // 刷新在线用户列表
		String []usr = usrsList.split(",");
		ObservableList<String> us = FXCollections.observableArrayList(usr);
		
		// 标记离线状态
		for(int i=0; i<this.usrsList.size(); ++i) {
			if(this.usrsList.get(i).equals(TalkServerMaster.Master)) continue;
			String un = getUsrName(this.usrsList.get(i));
			if(un != null) {
				if( !us.contains(un)) this.usrsList.set(i, un + " (Offline)");
				else this.usrsList.set(i, un);
			}
		}

		// 刷新列表
		for(String u: usr) {
			if(u!=null && !this.usrsList.contains(u) && !this.usrsList.contains(u + " (*)") && !this.usrsList.contains(u + " (Offline)"))
				this.usrsList.add(u);
		}

		// 消息提醒
		for(int i=0; i<this.usrsList.size(); ++i) {
			String u = getUsrName(this.usrsList.get(i));
			if(u != null && usrsMsgNotify.get(u) != null && usrsMsgNotify.get(u)) { // 有新消息
				this.usrsList.set(i, u + " (*)");
			}
		}

	}

	public void storeMsg(String fromUsr, String msg, boolean all) {
		String Msg = String.format("[%s <%s>] %s\n",
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
				fromUsr, new String(Base64.getDecoder().decode(msg), StandardCharsets.UTF_8)
				);
		if(all) fromUsr = TalkServerMaster.Master;
		if(usrsMsg.get(fromUsr) == null)
			usrsMsg.put(fromUsr, Msg);
		else if(! fromUsr.equals(TalkServerMaster.Master))
			usrsMsg.put(fromUsr, usrsMsg.get(fromUsr) + Msg);
		usrsMsgNotify.put(fromUsr, true);
	}

	public void execute(String cmd) {
		String regex = "\\[([\\w\\s]+)\\](.*)"; // 匹配[action]arg
		if(cmd == null || ! cmd.matches(regex)) return;
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(cmd);
		m.find(); // 必须要find才能group。。。
		String action = m.group(1);
		String arg = m.group(2);
		if(action.equalsIgnoreCase("USERS")) {
			getUsrs(arg);
		} else if(action.contains("ALLFROM")) { // 存取消息
			String fromUser = action.substring("ALLFROM".length() + 1, action.length());
			storeMsg(fromUser, arg, true);
		} else if(action.contains("FROM")) { // 存取消息
//			System.out.println(cmd);
			String fromUser = action.substring("FROM".length() + 1, action.length());
			storeMsg(fromUser, arg, false);
		}
	}
	
	/**
	 * 加载聊天记录
	 */
	private void loadTalkRecord() {
		File dir = new File(talkRecordDir);
		Pattern p = Pattern.compile("(.+)\\.txt$");
		if(! dir.exists()) return;
		for(File file: dir.listFiles()) { // 遍历记录文件
			Matcher m = p.matcher(file.getName());
			if(m.find()) {
				if(m.group(1).equals(usrName)) continue; // 跳过自己的聊天记录

				try {
					FileInputStream fis = new FileInputStream(file);
					byte[] data = new byte[(int)file.length()];
					fis.read(data);
					fis.close();
					usrsMsg.put(m.group(1), new String(data, "UTF-8"));
					if(!m.group(1).equals(TalkServerMaster.Master)) usrsList.add(m.group(1));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/** 
	 * 保存聊天记录
	 */
	private void saveTalkRecord() {
		File dir = new File(talkRecordDir);
		if(! dir.exists()) dir.mkdir();
		if(usrsMsg == null) return;
		for(Map.Entry<String, String> usr: usrsMsg.entrySet()) {
			File file = new File(talkRecordDir + "/" + usr.getKey() + ".txt");
			try {
				FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
				osw.write(usr.getValue());
				osw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	// 获取用户名，例如"xxx (*)"，得到xxx
	private String getUsrName(String u) {
		if(u == null) return null;
		/*
		 * 用户有3种状态：
		 * users: 在线状态
		 * users (*): 该用户发来消息
		 * users (offline): 该用户离线了
		 */
		Pattern p = Pattern.compile("([\\w\\d]+)( \\((\\*|Offline)\\))?"); 
		Matcher m = p.matcher(u);
		m.find();
		return m.group(1);
	}

	/**
	 * 判断用户是否离线
	 * @param u
	 * @return 是否离线
	 */
	private boolean usrIsOffline(String u) { 
		if(u == null) return false;
		Pattern p = Pattern.compile("([\\w\\d]+)( \\((\\*|Offline)\\))?"); 
		Matcher m = p.matcher(u);
		m.find();
		return m.group(3) != null && m.group(3).equals("Offline");
	}


	// 发送消息
	private void sendMsg() {
		String curUsr = getUsrName(usrsListView.getSelectionModel().getSelectedItem());
		if(sendMsg.getText() != null && ! Pattern.matches("\\n*", sendMsg.getText())) {
			out.printf("[SENDTO %s]%s\n", curUsr, Base64.getEncoder().encodeToString(sendMsg.getText().getBytes(StandardCharsets.UTF_8)));
			out.flush();
			String Msg = String.format("[%s <%s>] %s\n",
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
					usrName, sendMsg.getText()
					);
			if(usrsMsg.get(curUsr) != null)
				usrsMsg.put(curUsr, usrsMsg.get(curUsr) + Msg);
			else
				usrsMsg.put(curUsr, Msg);
			message.appendText(Msg);
			sendMsg.setText("");
		} else {
			sendMsg.setText("");
		}
	}

	private void talkScene() { // 聊天界面
		usrsMsg = new HashMap<String, String>();
		usrsMsgNotify = new HashMap<String, Boolean>();
        usrsList = FXCollections.observableArrayList(TalkServerMaster.Master);
		loadTalkRecord();
		pStage.setTitle("Talk by netcan v"+ VERSION +" [当前用户: "+usrName+"]");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));


        // 在线用户
        usrsListView = new ListView<>(usrsList);
        usrsListView.setItems(usrsList);
        usrsListView.getSelectionModel().select(0);

        VBox usrListBox = new VBox();
        VBox.setVgrow(usrsListView, Priority.ALWAYS);
        usrListBox.getChildren().addAll(usrsListView);


        // 消息界面
        VBox sendBox = new VBox();
        message = new TextArea();
        sendMsg = new TextArea();
        message.setEditable(false);
        message.setPrefRowCount(20);
        message.setWrapText(true);
        sendMsg.setPrefRowCount(5);
        sendMsg.setWrapText(true);

        message.setStyle("-fx-font-size: 17; -fx-font-family: \"OpenSansEmoji\";"); 
        sendMsg.setStyle("-fx-font-size: 17; -fx-font-family: \"OpenSansEmoji\";"); 

        sendBox.getChildren().add(message);
        sendBox.getChildren().add(sendMsg);

        Button btn = new Button("发送");
        Button emojiBtn = new Button(TalkEmoji.emoji[0] + "表情");
        emojiBtn.setStyle("-fx-font-family: \"OpenSansEmoji\";"); 
        HBox hbBtn = new HBox(0);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(emojiBtn);
        hbBtn.getChildren().add(btn);
        sendBox.getChildren().add(hbBtn);

        grid.add(usrListBox, 0, 0);
        grid.add(sendBox, 1, 0);
		Scene scene = new Scene(grid);
		pStage.setScene(scene);


		// 事件处理
		// 消息切换
        usrsListView.getSelectionModel().selectedItemProperty().addListener(
        		(ObservableValue<? extends String> ov, String old_val,
        				String new_val) -> {
        					String oldV = getUsrName(old_val);
        					String newV = getUsrName(new_val);
//        					System.out.println("old:" + oldV);
//        					System.out.println("new:" + newV);
        					if(! oldV.equals(newV)) { // 切换对话
        						if(usrsMsg.get(newV) != null) {
        							message.setText(usrsMsg.get(newV));
        							usrsMsgNotify.put(newV, false); // 已读
        							messageGotoEndLine = true; // 因为事件处理无法滚到最后，只能通过这种方式让外部滚动了
        						} else {
        							message.setText("");
        						}
        					}
        				}
        		);

        // 发送消息
        btn.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event) {
        		sendMsg();
        	}
		});

        sendMsg.setOnKeyPressed(new EventHandler<KeyEvent>() {
        	   @Override
        	    public void handle(KeyEvent keyEvent) {
        	        if (keyEvent.getCode() == KeyCode.ENTER)  {
        	        	sendMsg();
        	        }
        	    }
		});
        
        // 表情处理
        Button []emojis = new Button[TalkEmoji.emoji.length];
        for(int i=0; i<TalkEmoji.emoji.length; ++i) { // 将表情显示到按钮上
        	emojis[i] = new Button(TalkEmoji.emoji[i]);
        	emojis[i].setPrefSize(56, 56);
        	emojis[i].setStyle("-fx-font-size: 20; -fx-focus-color: transparent; -fx-font-family: \"OpenSansEmoji\";"); // 表情大小，清除选择的框框
        	emojis[i].setOnAction((event) -> { 
        		sendMsg.appendText(((Button) event.getSource()).getText()); // 黑科技，获取事件源对象
        	});
        }
        emojiStage = new Stage();
        
        pStage.setOnCloseRequest(event -> {
        	if(emojiStage.isShowing()) emojiStage.close();
        });

        emojiBtn.setOnAction(event1 -> { // 弹出表情选择
			emojiBtn.setDisable(true);
			emojiStage.setTitle("Select Emoji");
			FlowPane pane = new FlowPane();
			for(int i=0; i<TalkEmoji.emoji.length; ++i) 
				pane.getChildren().add(emojis[i]);
			Scene emojiScene = new Scene(pane);
			emojiStage.setScene(emojiScene);
			emojiStage.setResizable(false);
			emojiStage.setX(pStage.getX() + pStage.getWidth() / 3);
			emojiStage.setY(pStage.getY());
			emojiStage.show();

			emojiStage.setOnCloseRequest(event2 -> {
				emojiBtn.setDisable(false);
			});
		});

		// 线程处理消息接收
		Task<Void> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				while(out != null && in != null) {
					try {
						Thread.sleep(300);
						//	这里处理客户端请求
						// 获取用户列表
						out.println("[GETUSRS]");
						out.flush();

						String cmd = in.readLine();
						if(cmd == null) break;
						// 处理响应
						Platform.runLater(new Runnable() { // 刷新UI要放到runLater刷新
							@Override
							public void run() {
								// TODO Auto-generated method stub
								// 刷新在线用户
								execute(cmd);
								if(messageGotoEndLine) { // 滚到最后
									message.setScrollTop(Double.MAX_VALUE);
									messageGotoEndLine = false;
								}
								// 刷新选中用户最新消息
								int curUsrId = usrsListView.getSelectionModel().getSelectedIndex();
								String curUsr = getUsrName(usrsListView.getSelectionModel().getSelectedItem());

								// 用户离线，消息禁用
								if(usrIsOffline(usrsListView.getSelectionModel().getSelectedItem())) {
									sendMsg.setDisable(true);
									btn.setDisable(true);
								}
								else {
									sendMsg.setDisable(false);
									btn.setDisable(false);
								}

								if(usrsMsgNotify.get(curUsr) != null && usrsMsgNotify.get(curUsr)) { // 有新消息了
									usrsMsgNotify.put(curUsr, false); // 已读
									usrsList.set(curUsrId, curUsr); // 删除(*)提醒
//									System.out.println(curUsr);
									// 附加消息
									message.appendText(usrsMsg.get(curUsr).substring(message.getText().length(), usrsMsg.get(curUsr).length()));
								}
							}
						});


					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return null;
			}
		};
		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();

	}

	private boolean checkLogin() {
		// 检测有效性
		Alert alert = new Alert(AlertType.ERROR);

		// connect to master
		try {
			client = new Socket(ip, TalkServerMaster.PORT);
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			out = new PrintWriter(client.getOutputStream());
			out.println("[REGISTER]"+usrName);
			out.flush();
			String result = in.readLine();
			if(result.equals("[FAILED]"))
				return false;
			else if(result.equals("[OK]"))
				return true;
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			alert.setContentText(e1.toString());
			alert.showAndWait();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			alert.setContentText(e1.toString());
			alert.showAndWait();
		}
		return false;
	}


	// login window
	@Override
	public void start(Stage primaryStage) throws IOException {
		Font.loadFont(getClass().getResource("/assets/OpenSansEmoji.ttf").toExternalForm(), 16);

//		Font.getFamilies();
//		for(String s: Font.getFamilies()) {
//			System.out.println(s);
//		}
		pStage = primaryStage;
		pStage.setTitle("Talk by netcan v"+ VERSION);
		pStage.setResizable(false);
		loginScene();
	}

	@Override
	public void stop() {
		try {
			if(in != null && out != null && client != null) {
				out.println("[LOGOUT]");
				saveTalkRecord();
				out.flush();
				in.close();
				out.close();
				in = null;
				out = null;
				client.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
