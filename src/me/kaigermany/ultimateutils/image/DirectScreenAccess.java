package me.kaigermany.ultimateutils.image;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.peer.RobotPeer;
import java.lang.reflect.Method;

public class DirectScreenAccess {
	private static final RobotPeer robotPeer;
	private static Method robotPeerGetRGBPixelsMethod;
	private static final Robot robot;
	
	static{
		{
			Robot r = null;
		    try{
		    	r = new Robot();
		    }catch(Exception e){
		    	e.printStackTrace();
		    }
		    robot = r;
		}
		
	    Toolkit localToolkit = Toolkit.getDefaultToolkit();
	    RobotPeer peerInstance = null;
	    Method peerMethod = null;
	    
	    
	    
	    try{
	    	GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	    	//peer = ((sun.awt.ComponentFactory)localToolkit).createRobot(this, graphicsDevice);
	    	//this should call:
	    	//"sun.awt.windows.WToolkit.createRobot(Robot var1, GraphicsDevice var2){
	    	//	return new sun.awt.windows.WRobotPeer(var2);
	    	//}"
	    	Object robotPeer = localToolkit.getClass()
		    	.getMethod("createRobot", Robot.class, GraphicsDevice.class)
		    	.invoke(localToolkit, robot, graphicsDevice);
	    	
	    	peerInstance = (RobotPeer)robotPeer;
	    	peerMethod = robotPeer.getClass().getDeclaredMethod("getRGBPixels", int.class, int.class, int.class, int.class, int[].class);
	    	peerMethod.setAccessible(true);
	    	//localToolkit
	    }catch(Exception e){
	    	peerMethod = null;
	    	if(peerInstance != null) {
	    		//cleanup if allocated.
	    		peerInstance.dispose();
	    		peerInstance = null;
	    	}
	    	e.printStackTrace();
	    }
	    
	    robotPeer = peerInstance;
	    robotPeerGetRGBPixelsMethod = peerMethod;
	}
	
	
	public static void getScreenshot(int x, int y, int w, int h, int[] outputPixelBuffer){
		if(outputPixelBuffer == null){
			throw new IndexOutOfBoundsException("expected " + (w * h) + ", got null");
		}
		if(outputPixelBuffer.length != w * h){
			throw new IndexOutOfBoundsException("expected " + (w * h) + ", got " + outputPixelBuffer.length);
		}
		
		if(robotPeerGetRGBPixelsMethod != null){
			try {
				robotPeerGetRGBPixelsMethod.invoke(robotPeer, x, y, w, h, outputPixelBuffer);
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//fallback:
		robot.createScreenCapture(new Rectangle(x, y, w, h)).getRGB(0, 0, w, h, outputPixelBuffer, 0, w);
	}
	
	public static void enforceDispose(){
		if(robotPeer != null) {
			robotPeer.dispose();
			robotPeerGetRGBPixelsMethod = null;
		}
	}
}
