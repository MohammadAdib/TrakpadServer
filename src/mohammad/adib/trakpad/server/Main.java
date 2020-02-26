package mohammad.adib.trakpad.server;

import java.awt.*;
import java.awt.event.InputEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.Random;

public class Main {

    private static boolean debug;
    private static boolean isRunning = true;
    private static Robot robot;
    private static String computerName;
    private static final int PORT = 18256;
    private static final int MAX_SCROLL = 1;

    public static void main(String[] args) {
        computerName = getComputerName();
        debug = args.length > 0 && args[0].equals("--debug");
        System.out.println("  ______           __                   __\n" +
                " /_  __/________ _/ /______  ____ _____/ /\n" +
                "  / / / ___/ __ `/ //_/ __ \\/ __ `/ __  / \n" +
                " / / / /  / /_/ / ,< / /_/ / /_/ / /_/ /  \n" +
                "/_/ /_/   \\__,_/_/|_/ .___/\\__,_/\\__,_/   \n" +
                "                   /_/                    ");
        new Thread(Main::startUDPServer).start();
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private static String getComputerName() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("COMPUTERNAME"))
            return env.get("COMPUTERNAME");
        else if (env.containsKey("HOSTNAME"))
            return env.get("HOSTNAME");
        else
            return "TRAKPAD-" + new Random().nextInt(500);
    }

    private static void startUDPServer() {
        DatagramSocket wSocket = null;
        DatagramPacket wPacket = null;
        byte[] wBuffer = null;

        try {
            wSocket = new DatagramSocket(PORT);
            System.out.println("\nServer started. Please connect your phone to the same Wi-Fi network and enjoy");
            System.out.println("Server name: " + computerName);
            while (isRunning) {
                try {
                    wBuffer = new byte[2048];
                    wPacket = new DatagramPacket(wBuffer, wBuffer.length);
                    wSocket.receive(wPacket);
                    handlePacket(wPacket, wBuffer);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (SocketException e) {
            System.out.println("Error: \n" + e.getMessage());
            System.exit(1);
        }
    }

    private static void handlePacket(DatagramPacket p, byte[] b) throws Exception {
        String rawMessage = new String(b).trim();
        if (debug) System.out.println(rawMessage + " from " + p.getAddress() + ":" + p.getPort());
        if (rawMessage.startsWith("trakpad-ack")) {
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(computerName.getBytes(), computerName.length(), p.getAddress(), PORT);
            socket.send(packet);
        } else if (rawMessage.equals("tap")) {
            clickMouse(InputEvent.BUTTON1_DOWN_MASK);
        } else if (rawMessage.equals("2tap")) {
            clickMouse(InputEvent.BUTTON3_DOWN_MASK);
        } else {
            float x = Float.parseFloat(rawMessage.split(" ")[0]);
            float y = Float.parseFloat(rawMessage.split(" ")[1]);
            float pointers = Float.parseFloat(rawMessage.split(" ")[2]);
            if (pointers == 1) {
                moveMouse(x, y);
            } else {
                scrollMouse(y);
            }
        }
    }

    private static void moveMouse(float x, float y) {
        Point location = MouseInfo.getPointerInfo().getLocation();
        robot.mouseMove((int) (location.x + x + 0.5f), (int) (location.y + y + 0.5f));
    }

    private static void scrollMouse(float y) {
        robot.mouseWheel(Math.max(-MAX_SCROLL, Math.min(MAX_SCROLL, (int) y)));
    }

    private static void clickMouse(int button) {
        new Thread(() -> {
            robot.mousePress(button);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            robot.mouseRelease(button);
        }).start();
    }
}