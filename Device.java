import java.io.*;
//import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.tree.*;

public class Device
{
  public static String[] cmd(String... args) throws IOException
  {
    List<String> resultList = new ArrayList<String>();
    ProcessBuilder pb = new ProcessBuilder(args);
    pb.redirectErrorStream(true);
    Process p = pb.start();
    InputStream in = p.getInputStream();
    BufferedReader stdInput = new BufferedReader(new InputStreamReader(in, "UTF-8"));
    String s;
    while((s = stdInput.readLine()) != null)
      resultList.add(s);
    
    String[] result = resultList.toArray(new String[0]);
    return result;
  }
  
  public static String getSerialFromLine(String line)
  {
		Pattern deviceRegex = Pattern.compile(".*\\b(device|unauthorized)\\b.*");
    if(deviceRegex.matcher(line).matches())
    {
      String[] parts = line.split("\\s+");
      return parts[0];
    }
    else
      return null;
  }
  
	public String rawLine;
	public String serial;
	public String status;
	public boolean available;
	public String product;
	public String model;
	public String device;
	public String usb;
	public String transport;
	public DeviceLibrary library;
  public String[] displayData;
	
	public Device(String line)
	{
    rawLine = line;
    String[] parts = line.split("\\s+");
    serial = parts[0];
    load(parts);
	}
  
  public void load(String line)
  {
    load(line.split("\\s+"));
  }
  
  public void load(String[] parts)
  {
    if("device".equals(parts[1]))
    {
      status = parts[1];
      available = true;
      for(int i = 2; i < parts.length; i++)
      {
        if("product:".equals(parts[i].substring(0, 8)))
          product = parts[i].substring(8);
        else if("model:".equals(parts[i].substring(0, 6)))
          model = parts[i].substring(6);
        else if("device:".equals(parts[i].substring(0, 7)))
          device = parts[i].substring(7);
        else if("usb:".equals(parts[i].substring(0, 4)))
          usb = parts[i].substring(4);
        else if("transport_id:".equals(parts[i].substring(0, 13)))
          transport = parts[i].substring(13);
      }
    }
    else
    {
      status = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
      available = false;
    }
    
    displayData = new String[]{serial, status, product!=null?product:"-", model!=null?model:"-", device!=null?device:"-", usb!=null?usb:"-", transport!=null?transport:"-"};
  }
  public static String[] displayHeaders = new String[]{"Serial No.", "Status", "Product", "Model", "Device", "USB", "TransportID"};
  
  public void createLibrary(String path) throws DeviceMissingException, ADBException
  {
    library = new DeviceLibrary(path);
  }
  
  public String[] ls()
  {
    return ls("/");
  }
  
  public String[] ls(String path)
  {
    try
    {
      String[] output = cmd("adb", "-s", serial, "shell", "find", path, "-mindepth", "1", "-maxdepth", "1", "-type", "d", "!", "-type", "l");
      ArrayList<String> result = new ArrayList<String>();
      for(String dir : output)
      {
        if(dir.startsWith("/"))
          result.add(dir.substring(path.length()));
      }
      result.sort(new Comparator<String>(){
        public int compare(String a, String b)
        {
          return a.compareTo(b);
        }
      });
      return result.toArray(new String[0]);
    }
    catch(IOException x)
    {
      return new String[0];
    }
  }
  
	public static class DeviceMissingException extends Exception
	{
		public DeviceMissingException(String message)
		{
			super(message);
		}
		public DeviceMissingException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}
	
	public static class ADBException extends Exception
	{
		public ADBException(String message)
		{
			super(message);
		}
		public ADBException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}
}
