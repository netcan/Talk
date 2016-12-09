import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TalkServerWorker extends Thread {
	private Socket worker;
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
			}
			worker.close();
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
