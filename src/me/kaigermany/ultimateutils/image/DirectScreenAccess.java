package me.kaigermany.ultimateutils.image;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.peer.RobotPeer;
import java.lang.reflect.Method;

public class DirectScreenAccess {
	private final Robot robot;
	
	private RobotPeer robotPeer;
	private Method robotPeerGetRGBPixelsMethod;
	
	/**
	 * allocated all internal fields.
	 * it is highly recommended to do not spam-create class instances, prefer reuse it as long as possible,
	 * because the allocation process may take some time dune to some internal reflection and construction calls.
	 * 
	 * @throws AWTException if "new Robot()" ctor call fails. critical because this is the fallback foundation.
	 */
	public DirectScreenAccess() throws AWTException {
		robot = new Robot();//throws AWTException exclusively.
		
	    Toolkit localToolkit = Toolkit.getDefaultToolkit();
	    
	    try{
	    	GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	    	
	    	//peer = ((sun.awt.ComponentFactory)localToolkit).createRobot(this, graphicsDevice);
	    	//this should call:
	    	//"sun.awt.windows.WToolkit.createRobot(Robot var1, GraphicsDevice var2){
	    	//	return new sun.awt.windows.WRobotPeer(var2);
	    	//}"
	    	robotPeer = (RobotPeer)localToolkit.getClass()
		    	.getMethod("createRobot", Robot.class, GraphicsDevice.class)
		    	.invoke(localToolkit, robot, graphicsDevice);
	    	
	    	robotPeerGetRGBPixelsMethod = robotPeer.getClass().getDeclaredMethod("getRGBPixels", int.class, int.class, int.class, int.class, int[].class);
	    	robotPeerGetRGBPixelsMethod.setAccessible(true);
	    	//localToolkit
	    }catch(Exception e){
	    	//notify the user. even if this is ignored, any createScreenshot() will return false then.
	    	e.printStackTrace();
	    	//deallocate the RobotPeer instance.
	    	dispose();
	    }
	}
	
	/**
	 * this is the alternative to java.awt.Robot.createScreenCapture().
	 * @param x screen offset X
	 * @param y screen offset Y
	 * @param w screen frame Width
	 * @param h screen frame Height
	 * @param outputPixelBuffer raw Pixels, in ARGB standard format.
	 * @return true if shortcut was used or false if fallback standard API was used.
	 * @throws IndexOutOfBoundsException if the array did not match the requested size (w* h).
	 */
	public boolean createScreenshot(int x, int y, int w, int h, int[] outputPixelBuffer){
		if(outputPixelBuffer == null){
			throw new IndexOutOfBoundsException("expected " + (w * h) + ", got null");
		}
		if(outputPixelBuffer.length != w * h){
			throw new IndexOutOfBoundsException("expected " + (w * h) + ", got " + outputPixelBuffer.length);
		}
		
		if(robotPeerGetRGBPixelsMethod != null){
			try {
				//now directly invoke the native method:
				robotPeerGetRGBPixelsMethod.invoke(robotPeer, x, y, w, h, outputPixelBuffer);
				return true;
			} catch (Exception ignored) {}
		}
		
		//fallback: classic approach but uses way more temporary memory.
		robot.createScreenCapture(new Rectangle(x, y, w, h)).getRGB(0, 0, w, h, outputPixelBuffer, 0, w);
		return false;
	}
	
	/**
	 * deallocated the interface. the fallback will take over for any follow-up calls.
	 */
	public void dispose(){
		if(robotPeer != null) {
			robotPeer.dispose();
			robotPeer = null;
		}
		robotPeerGetRGBPixelsMethod = null;
	}
	
	/**
	 * ensure dealloocation.
	 */
	@Override
	protected void finalize() throws Throwable {
		dispose();
		super.finalize();
	}
}
