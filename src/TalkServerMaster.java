import java.io.IOException;
import java.net.ServerSocket;

public class TalkServerMaster {
	private static ServerSocket server;
	public void boot() {
		try {
			server = new ServerSocket(2333);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(true) {
			TalkServerWorker worker;
			try {
				worker = new TalkServerWorker(server.accept());
				worker.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
