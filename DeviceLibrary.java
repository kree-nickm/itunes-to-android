import java.io.*;
//import java.net.*;
import java.util.*;
import java.util.regex.*;

public class DeviceLibrary
{
	public boolean loaded = false;
	public Set<Track> deviceFiles;
	public String path;
	
	public DeviceLibrary(String path) throws Device.DeviceMissingException, Device.ADBException
	{
		if(path.endsWith("/"))
			this.path = path;
		else
			this.path = path +"/";
		buildFileList();
	}
	
	public void buildFileList() throws Device.DeviceMissingException, Device.ADBException
	{
		loaded = false;
		deviceFiles = new HashSet<Track>();
		try
		{
      String[] output = Device.cmd("adb", "shell", "ls", "-p", "-R", "-1", path);
			String folder = null;
			for(String s : output)
			{
				if(s.equals("error: no devices/emulators found"))
					throw new Device.DeviceMissingException("No Android device was detected by Android Debug Bridge.");
				else if(s.endsWith(":") && s.length() > path.length())
					folder = s.substring(path.length(), s.length()-1) +"/";
				else if(s.length() > 0 && !s.endsWith("/") && folder != null)
					deviceFiles.add(new Track(folder, s));
			}
		}
		catch(IOException x)
		{
			if(x.getMessage().startsWith("Cannot run program"))
				throw new Device.ADBException(x.getMessage(), x);
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
	
	public class Track implements iTunesToAndroid.Track
	{
		protected String name;
		protected String nameOnly;
		protected String extension;
		protected String relativePath;
		protected String noExtension;
    public iTunesLibrary.Track pairedTrack;
		
		public Track(String folders, String file)
		{
			name = file.replace("\\ ", " ");
			int dot = name.lastIndexOf('.');
			if(dot > -1)
			{
				nameOnly = name.substring(0, dot);
				extension = name.substring(dot+1);
			}
			else
			{
				nameOnly = name;
				extension = "";
				dot = name.length();
			}
			relativePath = folders + name;
			dot += folders.length();
			noExtension = relativePath.substring(0, dot);
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
    
    public iTunesLibrary.Track getPairedTrack()
    {
      return pairedTrack;
    }
		
		public boolean equals(Object other)
		{
			if(other instanceof iTunesLibrary.Track)
			{
        String myUnique = iTunesToAndroid.Track.normalizePath(getRelativePathNoExtension());
        String otherUnique = iTunesToAndroid.Track.normalizePath(((iTunesLibrary.Track)other).getRelativePathNoExtension());
        boolean equal = myUnique.equals(otherUnique);
        if(equal)
        {
          this.pairedTrack = (iTunesLibrary.Track)other;
          ((iTunesLibrary.Track)other).pairedTrack = this;
        }
        return equal;
			}
			else if(other instanceof DeviceLibrary.Track)
			{
				return getRelativePath().equals(((DeviceLibrary.Track)other).getRelativePath());
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