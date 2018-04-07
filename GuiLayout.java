import java.awt.*;
import java.util.*;

/**
 * The GuiLayout allows more intuitive placement of GUI elements than any other LayoutManager. Every other LayoutManager places each component relative to one another, whereas the GuiLayout is designed to allow every element to be placed individually without regard for any other.
 */
public class GuiLayout implements LayoutManager2
{
	protected Map<Component,GuiConstraint> components;
	
	public GuiLayout()
	{
		components = new HashMap<Component,GuiConstraint>();
	}
	
	public void addLayoutComponent(String name, Component comp)
	{
		synchronized(comp.getTreeLock()) {
			throw new IllegalArgumentException("call to the String version of the addLayoutComponent method");
		}
	}
	
	public void addLayoutComponent(Component comp, Object constraints)
	{
		synchronized(comp.getTreeLock()) {
			if(constraints instanceof GuiConstraint)
			{
				components.put(comp, (GuiConstraint)constraints);
			}
			else
			{
				throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + constraints);
			}
		}
	}
	
	public void removeLayoutComponent(Component comp)
	{
		synchronized(comp.getTreeLock()) {
			components.remove(comp);
		}
	}
	
	public Dimension maximumLayoutSize(Container parent)
	{
		synchronized(parent.getTreeLock()) {
			return preferredLayoutSize(parent);
		}
	}
	
	public Dimension minimumLayoutSize(Container parent)
	{
		synchronized(parent.getTreeLock()) {
			return preferredLayoutSize(parent);
		}
	}
	
	public Dimension preferredLayoutSize(Container parent)
	{
		synchronized(parent.getTreeLock()) {
			int x = Integer.MAX_VALUE;
			int y = Integer.MAX_VALUE;
			int w = 0;
			int h = 0;
			Collection<GuiConstraint> coll = components.values();
			Iterator<GuiConstraint> iter = coll.iterator();
			while(iter.hasNext())
			{
				Rectangle r = iter.next().getRectangle();
				if(x > r.x)
					x = r.x;
				if(y > r.y)
					y = r.y;
				if(w < r.width + r.x)
					w = r.width + r.x;
				if(h < r.height + r.y)
					h = r.height + r.y;
			}
			return new Dimension(w-x, h-y);
		}
	}
	
	public void layoutContainer(Container parent)
	{
		synchronized(parent.getTreeLock()) {
			Set<Component> comps = components.keySet();
			Iterator<Component> iter = comps.iterator();
			while(iter.hasNext())
			{
				Component comp = iter.next();
				comp.setBounds(components.get(comp).getRectangle());
			}
		}
	}
	
	public float getLayoutAlignmentX(Container target)
	{
		return 0.0f;
	}
	
	public float getLayoutAlignmentY(Container target)
	{
		return 0.0f;
	}
	
	public void invalidateLayout(Container target)
	{
	}
	
	public static class GuiConstraint
	{
		// Units
		public static final int PIXELS = 1;
		public static final int PERCENT = 2;
		// Alignments
		public static final int LEFT = 10;
		public static final int RIGHT = 11;
		public static final int CENTER = 12;
		
		protected Container parent;
		protected Component target;
		protected int x;
		protected int y;
		protected int width;
		protected int height;
		protected int xUnit;
		protected int yUnit;
		protected int widthUnit;
		protected int heightUnit;
		protected int xAlign;
		protected int yAlign;
		protected int widthAnchor;
		protected int heightAnchor;
		
		public GuiConstraint(Container parent, String x, String y, Component target)
		{
			this(parent, x, y, "0", "0", target);
		}
		
		public GuiConstraint(Container parent, String x, String y, String w, String h)
		{
			this(parent, x, y, w, h, null);
		}
		
		public GuiConstraint(Container parent, String x, String y, String w, String h, Component target)
		{
			this.parent = parent;
			this.target = target;
			int[] xVals = parseValue(x);
			int[] yVals = parseValue(y);
			int[] wVals = parseValue(w);
			int[] hVals = parseValue(h);
			this.x = xVals[0];
			this.xUnit = xVals[1];
			this.xAlign = xVals[2];
			this.y = yVals[0];
			this.yUnit = yVals[1];
			this.yAlign = yVals[2];
			this.width = wVals[0];
			this.widthUnit = wVals[1];
			this.widthAnchor = wVals[2];
			this.height = hVals[0];
			this.heightUnit = hVals[1];
			this.heightAnchor = hVals[2];
		}
		
		protected int[] parseValue(String s)
		{
			int[] result = new int[9];
			String[] temp = s.split(" ");
			for(int i = 0; i < temp.length && i < 3; i++)
			{
				int k = temp[i].indexOf("%");
				if(k == -1)
				{
					k = temp[i].indexOf("px");
					result[1+3*i] = PIXELS;
				}
				else
					result[1+3*i] = PERCENT;
				if(k == -1)
					k = temp[i].indexOf(",");
				
				String num;
				if(k > -1)
					num = temp[i].substring(0, k);
				else
					num = temp[i];
				result[0+3*i] = Integer.parseInt(num);
				
				k = temp[i].indexOf(",");
				if(k > -1)
				{
					String prop = temp[i].substring(k+1);
					if("right".equalsIgnoreCase(prop) || "bottom".equalsIgnoreCase(prop))
						result[2+3*i] = RIGHT;
					else if("center".equalsIgnoreCase(prop) || "middle".equalsIgnoreCase(prop))
						result[2+3*i] = CENTER;
					else
						result[2+3*i] = LEFT;
				}
				else
					result[2+3*i] = LEFT;
			}
			if(temp.length < 3)
			{
				result[6] = result[0];
				result[7] = result[1];
				result[8] = result[2];
			}
			if(temp.length < 2)
			{
				result[3] = result[0];
				result[4] = result[1];
				result[5] = result[2];
			}
			return result;
		}
		
		public Rectangle getRectangle()
		{
			int tmpX = 0;
			int tmpY = 0;
			int tmpW = 0;
			int tmpH = 0;
			
			// Translate units to pixels
			if(xUnit == PIXELS)
				tmpX = x;
			else if(xUnit == PERCENT)
				tmpX = (int)(parent.getWidth() * x / 100.0);
			
			if(yUnit == PIXELS)
				tmpY = y;
			else if(yUnit == PERCENT)
				tmpY = (int)(parent.getHeight() * y / 100.0);
			
			if(widthUnit == PIXELS)
				tmpW = width;
			else if(widthUnit == PERCENT)
				tmpW = (int)(parent.getWidth() * width / 100.0);
			
			if(heightUnit == PIXELS)
				tmpH = height;
			else if(heightUnit == PERCENT)
				tmpH = (int)(parent.getHeight() * height / 100.0);
			
			// Check for default size
			if(tmpW == 0 && widthAnchor == LEFT && target != null)
				tmpW = (int)target.getPreferredSize().getWidth();
			if(tmpH == 0 && heightAnchor == LEFT && target != null)
				tmpH = (int)target.getPreferredSize().getHeight();
			
			// Translate alignments
			if(xAlign == RIGHT)
			{
				if(widthAnchor == RIGHT)
				{
				}
				else
					tmpX = parent.getWidth() - tmpX - tmpW;
			}
			else if(xAlign == CENTER)
				tmpX = (parent.getWidth() - tmpW) / 2 + tmpX;
			else if(widthAnchor == RIGHT)
				tmpW = parent.getWidth() - tmpW - tmpX;
			
			if(yAlign == RIGHT)
			{
				if(heightAnchor == RIGHT)
				{
				}
				else
					tmpY = parent.getHeight() - tmpY - tmpH;
			}
			else if(yAlign == CENTER)
				tmpY = (parent.getHeight() - tmpH) / 2 + tmpY;
			else if(heightAnchor == RIGHT)
				tmpH = parent.getHeight() - tmpH - tmpY;
			
			// All done
			return new Rectangle(tmpX, tmpY, tmpW, tmpH);
		}
	}
}