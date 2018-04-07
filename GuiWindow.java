import java.awt.event.*;
import javax.swing.*;

public class GuiWindow extends JFrame
{
	protected iTunesToAndroid frontEnd;
	
	public GuiWindow(String name, iTunesToAndroid fe)
	{
		super(name);
		frontEnd = fe;
		addWindowListener(new GuiWindowListener());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		pack();
		setSize(1024, 768);
		setLocationRelativeTo(this);
		setVisible(true);
	}
	
	public void setCanvas(GuiCanvas canvas)
	{
		setContentPane(canvas);
		canvas.activated();
		setVisible(true);
	}
	
	protected class GuiWindowListener extends WindowAdapter
	{
		public void windowClosed(WindowEvent e)
		{
			frontEnd.exit();
		}
		
		public void windowClosing(WindowEvent e)
		{
			frontEnd.exit();
		}
	}
}