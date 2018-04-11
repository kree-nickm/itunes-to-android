import java.io.*;
//import java.net.*;
import java.util.*;

public class DeviceLibrary
{
	public static String checkForDevice() throws ADBException
	{
		try
		{
			ProcessBuilder pb = new ProcessBuilder("adb", "devices");
			pb.redirectErrorStream(true);
			Process p = pb.start();
			InputStream in = p.getInputStream();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String s;
			while((s = stdInput.readLine()) != null)
			{
				if(s.endsWith("device"))
					return s;
			}
		}
		catch(IOException x)
		{
			throw new ADBException(x.getMessage(), x);
		}
		return null;
	}
	
	public boolean loaded = false;
	public Set<Track> deviceFiles;
	public String path;
	
	public DeviceLibrary(String path) throws DeviceMissingException, ADBException
	{
		if(path.endsWith("/"))
			this.path = path;
		else
			this.path = path +"/";
		buildFileList();
	}
	
	public void buildFileList() throws DeviceMissingException, ADBException
	{
		loaded = false;
		deviceFiles = new HashSet<Track>();
		try
		{
			ProcessBuilder pb = new ProcessBuilder("adb", "shell", "ls", "-p", "-R", path);
			pb.redirectErrorStream(true);
			Process p = pb.start();
			InputStream in = p.getInputStream();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			//int numLines = 0;
			String folder = null;
			String s;
			while((s = stdInput.readLine()) != null)
			{
				//numLines++;
				//System.out.println(s);
				if(s.equals("error: no devices/emulators found"))
					throw new DeviceMissingException("No Android device was detected by Android Debug Bridge.");
				else if(s.endsWith(":"))
					folder = s.substring(path.length(), s.length()-1) +"/";
				else if(s.length() > 0 && !s.endsWith("/") && folder != null)
					deviceFiles.add(new Track(folder, s));
			}
		}
		catch(IOException x)
		{
			if(x.getMessage().startsWith("Cannot run program"))
				throw new ADBException(x.getMessage(), x);
			else
			{
				System.out.println(x);
				return;
			}
		}
		System.out.println("Device files loaded, "+ deviceFiles.size() +" found.");
		loaded = true;
	}
	
	public String[][] getDisplayData()
	{
		String[][] result = new String[deviceFiles.size()][1];
		Iterator<Track> iter = deviceFiles.iterator();
		int i = 0;
		while(iter.hasNext())
		{
			result[i] = new String[]{iter.next().getRelativePath()};
			i++;
		}
		return result;
	}
	
	public class Track
	{
		protected String name;
		protected String nameOnly;
		protected String extension;
		protected String relativePath;
		protected String noExtension;
		
		public Track(String folders, String file)
		{
			name = file;
			int dot = file.lastIndexOf('.');
			nameOnly = file.substring(0, dot);
			extension = file.substring(dot+1);
			relativePath = folders + file;
			noExtension = relativePath.substring(0, relativePath.lastIndexOf('.'));
		}
		
		public String getName()
		{
			return name;
		}
		
		public String getAbsolutePath()
		{
			return DeviceLibrary.this.path + relativePath;
		}
		
		public String getNameOnly()
		{
			return nameOnly;
		}
		
		public String getExtension()
		{
			return extension;
		}
		
		public String getRelativePath()
		{
			return relativePath;
		}
		
		public String getRelativePathNoExtension()
		{
			return noExtension;
		}
		
		public boolean equals(Object other)
		{
			if(other instanceof iTunesLibrary.Track)
			{
				return getRelativePathNoExtension().equals(((iTunesLibrary.Track)other).getRelativePathNoExtension());
			}
			else if(other instanceof DeviceLibrary.Track)
			{
				return relativePath.equals(((DeviceLibrary.Track)other).getRelativePath());
			}
			else
				return false;
		}
		
		public String toString()
		{
			return getAbsolutePath();
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