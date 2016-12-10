package pers.netcan.talk.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import static javafx.geometry.HPos.RIGHT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import pers.netcan.talk.server.TalkServerMaster;

public class TalkClient extends Application  {
	private static Socket client;
	private static BufferedReader in;
	private static PrintWriter out;
	private static Stage pStage;
	private ObservableList<String> usrsList;
	private Map<String, String> usrsMsg; // 保存信息
	private Map<String, Boolean> usrsMsgNotify; // 消息提示
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
	
	public void getUsrs(String usrsList) {
		String []usr = usrsList.split(",");
//		ObservableList<String> us = FXCollections.observableArrayList(usr);
		this.usrsList.clear();
		for(String u: usr) {
			if(u!=null) {
				if(usrsMsgNotify.get(u) != null && usrsMsgNotify.get(u))
					this.usrsList.add(u + "(*)");
				else
					this.usrsList.add(u);
			}
		}
	}
	
	public void storeMsg(String fromUsr, String msg, boolean all) {
		String Msg = String.format("[%s %s] %s\n", 
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), 
				fromUsr, msg
				);
		if(all) fromUsr = TalkServerMaster.Master;
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
		System.out.println(action);
		if(action.equalsIgnoreCase("USERS")) {
			getUsrs(arg);
		} else if(action.contains("ALLFROM")) { // 存取消息
			String fromUser = action.substring("ALLFROM".length() + 1, action.length());
			storeMsg(fromUser, arg, true);
		} else if(action.contains("FROM")) { // 存取消息
			String fromUser = action.substring("FROM".length() + 1, action.length());
			storeMsg(fromUser, arg, false);
		}
	}
	
	private void talkScene() { // 聊天界面
		usrsMsg = new HashMap<String, String>();
		usrsMsgNotify = new HashMap<String, Boolean>();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        usrsList = FXCollections.observableArrayList();
        ListView<String> usrsListView = new ListView<>(usrsList);
        usrsListView.setItems(usrsList);

        VBox usrListBox = new VBox();
        VBox.setVgrow(usrsListView, Priority.ALWAYS);
        usrListBox.getChildren().addAll(usrsListView);
        
        VBox sendBox = new VBox();
        TextArea message = new TextArea();
        TextArea sendMsg = new TextArea();
        message.setEditable(false);
        message.setPrefRowCount(20);
        sendMsg.setPrefRowCount(5);
        sendBox.getChildren().add(message);
        sendBox.getChildren().add(sendMsg);

        Button btn = new Button("发送");
        HBox hbBtn = new HBox(0);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        sendBox.getChildren().add(hbBtn);

        grid.add(usrListBox, 0, 0);
        grid.add(sendBox, 1, 0);
		Scene scene = new Scene(grid);
		pStage.setScene(scene);

		Task task = new Task<Void>() {
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
								int setUserId = usrsListView.getSelectionModel().getSelectedIndex();
								if(setUserId == -1) setUserId = 0;
//								System.out.println(cmd);
								execute(cmd);
								if(setUserId >= usrsList.size()) setUserId = 0;
								usrsListView.getSelectionModel().select(setUserId);
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
		pStage = primaryStage;
		pStage.setTitle("Talk by netcan");
		pStage.setResizable(false);
		loginScene();
	}
	
	@Override 
	public void stop() {
		try {
			if(in != null && out != null) {
				out.println("[LOGOUT]");
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		launch(args);
	}

}
