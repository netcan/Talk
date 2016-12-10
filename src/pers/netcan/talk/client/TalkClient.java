package pers.netcan.talk.client;

import javafx.application.Application;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import jdk.jfr.events.FileWriteEvent;
import pers.netcan.talk.server.TalkServerMaster;
import pers.netcan.talk.server.TalkServerWorker;

public class TalkClient extends Application  {
	private static Socket client;
	private static BufferedReader in;
	private static PrintWriter out;
	private static Stage pStage;
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
	
	private void talkScene() { // 聊天界面
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
		Scene scene = new Scene(grid, 800, 600);
		pStage.setScene(scene);
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
			if(result.equals("[FAILED]")) {
				return false;
			} else if(result.equals("[OK]"))
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
		loginScene();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		launch(args);
	}

}
