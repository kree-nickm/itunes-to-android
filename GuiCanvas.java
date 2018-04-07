import java.awt.*;
import java.util.*;

/**
 * A container class designed to allow easy setup of multiple types of graphic user interfaces. Configured to use the <tt>GuiLayout</tt> layout for that purpose.
 * @see GuiLayout
 */
abstract public class GuiCanvas extends Container
{
	protected Map<Component,Container> volatileMap;
	
	/**
	 * Constructs a basic empty GUI and calls the <tt>create</tt> method to add the components.
	 */
	public GuiCanvas()
	{
		volatileMap = new HashMap<Component,Container>();
		setLayout(new GuiLayout());
		create();
	}
	
	/**
	 * The method that sets up all of the GUI components.
	 */
	abstract protected void create();
	
	/**
	 * Whenever the GuiCanvas is activated, this method ensures that all volatile components are present. It does this by adding all of the volatile components it needs to their respective place-holder containers.
	 */
	protected void activated()
	{
		Set<Component> comps = volatileMap.keySet();
		Iterator<Component> iter = comps.iterator();
		while(iter.hasNext())
		{
			Component comp = iter.next();
			volatileMap.get(comp).add(comp);
		}
	}
	
	/**
	 * Adds a volatile component. Volatile components are components that will be added to more than one GuiCanvas. Adding a component to a container effectively removes it from any container it was previously in, so in order to make sure such a component shows up, volatile components need to be re-added to a container any time that the container is activated. This method actually adds a place-holder component/container with the specified GuiConstrant, so that the GuiCanvas can know that there should be a component there without being active.
	 * @param comp the component to add.
	 * @param rect the GUI constraints to apply to the component in this window.
	 */
	protected void addVolatile(Component comp, GuiLayout.GuiConstraint rect)
	{
		Container cont = volatileMap.get(comp);
		if(cont == null)
		{
			cont = new Container();
			cont.setLayout(new GridLayout(1,1));
			volatileMap.put(comp, cont);
		}
		add(cont, rect);
	}
}