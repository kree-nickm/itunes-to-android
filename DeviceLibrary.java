import java.io.*;
//import java.net.*;
import java.util.*;

public class DeviceLibrary
{
	public boolean loaded = false;
	public Set<Track> deviceFiles;
	public String path;
	
	public DeviceLibrary(String path)
	{
		if(path.endsWith("/"))
			this.path = path;
		else
			this.path = path +"/";
		buildFileList();
	}
	
	public void buildFileList()
	{
		loaded = false;
		deviceFiles = new HashSet<Track>();
		Process p;
		try
		{
			p = Runtime.getRuntime().exec("adb shell ls -p -R '"+ path +"'");
		}
		catch(IOException x1)
		{
			System.out.println(x1);
			return;
		}
		BufferedReader stdInput;
		try
		{
			stdInput = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
		}
		catch(UnsupportedEncodingException x)
		{
			System.out.println(x);
			return;
		}
		String folder = null;
		String s;
		try
		{
			while((s = stdInput.readLine()) != null)
			{
				if(s.endsWith(":"))
					folder = s.substring(path.length(), s.length()-1) +"/";
				else if(s.length() > 0 && !s.endsWith("/") && folder != null)
					deviceFiles.add(new Track(folder, s));
			}
		}
		catch(IOException x)
		{
			System.out.println(x);
			return;
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
}