import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.InetAddress;

public class ServerWindow extends Frame {
    private FileServer s = new FileServer(12345);
    private Label label;
    private Button button;
    private Button button1;

    public ServerWindow(String title) {
        super(title);

        setLayout(new FlowLayout(FlowLayout.LEFT, 20, 5));
        label = new Label();
        label.setText("Welcome to file transfer application!");

        button=new Button("Start server");

        button1=new Button("Stop server");
        add(label);
        add(button);
        add(button1);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                label.setText("Serber has started.");
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            s.start();
                        } catch (Exception e) {
                            // e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                label.setText("Server has stopped.");
                s.quit();
            }
        });


        this.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
//                new Thread(new Runnable() {
//                    public void run() {
//                        try {
//                            s.start();
//                        } catch (Exception e) {
//                            // e.printStackTrace();
//                        }
//                    }
//                }).start();
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
//                s.quit();
                System.exit(0);
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }
        });
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        ServerWindow window = new ServerWindow("文件上传服务端：" + address.getHostAddress());
        window.setSize(450, 150);
        window.setVisible(true);

        int windowWidth = window.getWidth(); //获得窗口宽
        int windowHeight = window.getHeight(); //获得窗口高
        Toolkit kit = Toolkit.getDefaultToolkit(); //定义工具包
        Dimension screenSize = kit.getScreenSize(); //获取屏幕的尺寸
        int screenWidth = screenSize.width; //获取屏幕的宽
        int screenHeight = screenSize.height; //获取屏幕的高
        window.setLocation(screenWidth/2-windowWidth/2, screenHeight/2-windowHeight/2);//设置窗口居中显示
    }

}