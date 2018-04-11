import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class iTunesLibrary
{
	public boolean loaded = false;
	public File libraryFile;
	public Map<String,Object> parsedXML;
	public Map<String,Object> allTracks;
	public List<Object> playlists;
	public String uriPath;
	public String path;
	protected Set<Track> playlistFiles;
	
	public iTunesLibrary(String filePath) throws InvalidLibraryException
	{
		// Initial setup.
		libraryFile = new File(filePath);
		if(!libraryFile.isFile())
		{
			throw new InvalidLibraryException("The path given is not a file.");
		}
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		Document doc;
		try
		{
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(libraryFile);
			doc.getDocumentElement().normalize();
		}
		catch(ParserConfigurationException x1)
		{
			throw new InvalidLibraryException("ParserConfigurationException: The file could not be parsed as XML. ("+ x1.getMessage() +")", x1);
		}
		catch(SAXException x2)
		{
			throw new InvalidLibraryException("SAXException: The file could not be parsed as XML. ("+ x2.getMessage() +")", x2);
		}
		catch(IOException x3)
		{
			throw new InvalidLibraryException("IOException: The file could not be parsed as XML. ("+ x3.getMessage() +")", x3);
		}
		
		// Parse XML into Map.
		NodeList children = doc.getDocumentElement().getChildNodes();
		for(int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("dict"))
			{
				parsedXML = iTunesLibrary.handleDict(child);
				break;
			}
		}
		if(parsedXML == null)
		{
			throw new InvalidLibraryException("The XML file was not recognized as an iTunes library.");
		}
		
		if(parsedXML.get("Tracks") instanceof Map)
			allTracks = (Map<String,Object>)parsedXML.get("Tracks");
		else
		{
			throw new InvalidLibraryException("No list of tracks was found in this iTunes library.");
		}
		if(parsedXML.get("Playlists") instanceof List)
			playlists = (List<Object>)parsedXML.get("Playlists");
		else
		{
			throw new InvalidLibraryException("No list of playlists was found in this iTunes library.");
		}
		if(parsedXML.get("Music Folder") instanceof String)
		{
			uriPath = (String)parsedXML.get("Music Folder");
			try
			{
				path = new URI(uriPath).getPath().substring(1);
			}
			catch(URISyntaxException x)
			{
				System.out.println("Music folder defined in iTunes library is not valid: "+ uriPath);
			}
		}
		else
		{
			throw new InvalidLibraryException("No music folder was defined in this iTunes library.");
		}
		System.out.println("iTunes library read into memory.");
		loaded = true;
	}
	
	public void buildPlaylistFiles(String[] playlistNames)
	{
		Arrays.sort(playlistNames);
		playlistFiles = new HashSet<Track>();
		Iterator iterLists = playlists.iterator();
		while(iterLists.hasNext())
		{
			Map<String,Object> plist = (Map<String,Object>)iterLists.next();
			List<Object> tracks;
			try
			{
				tracks = (List<Object>)plist.get("Playlist Items");
				tracks.get(0);
			}
			catch(NullPointerException x)
			{
				tracks = new ArrayList<Object>();
			}
			if(Arrays.binarySearch(playlistNames, plist.get("Name")) >= 0)
			{
				Iterator iterTracks = tracks.iterator();
				while(iterTracks.hasNext())
				{
					Map<String,Object> itemDict = (Map<String,Object>)iterTracks.next();
					Map<String,Object> trackDict = (Map<String,Object>)allTracks.get(itemDict.get("Track ID").toString());
					String loc = (String)trackDict.get("Location");
					if(loc != null)
					{
						try
						{
							Track track = new Track(new URI(loc), trackDict);
							if(track.isFile())
								playlistFiles.add(track);
						}
						catch(URISyntaxException x)
						{
							System.out.println(x);
						}
					}
					/*if(loc != null && loc.startsWith(uriPath))
					{
						try
						{
							// The .replace is needed because iTunes does not encode + signs, and Java thinks + signs are 'encoded' spaces. This fixes the translation error.
							playlistFiles.add(URLDecoder.decode(loc.substring(uriPath.length()).replace("+", "%2B"), "UTF-8"));
						}
						catch(UnsupportedEncodingException x)
						{
							System.out.println(x);
						}
					}*/
				}
			}
		}
		System.out.println(String.valueOf(playlistFiles.size()) + " files among selected playlists.");
	}
	
	public Set<Track> getPlaylistFiles()
	{
		if(playlistFiles != null)
			return playlistFiles;
		else
		{
			playlistFiles = new HashSet<Track>();
			return playlistFiles;
		}
	}
	
	public String[][] getDisplayData()
	{
		String[] cols = getDisplayColumns();
		String[][] result = new String[allTracks.size()][cols.length];
		Iterator iter = allTracks.values().iterator();
		int k = 0;
		while(iter.hasNext())
		{
			Map<String,Object> val = (Map<String,Object>)iter.next();
			for(int i = 0; i < cols.length; i++)
				result[k][i] = (String)val.get(cols[i]);
			k++;
		}
		return result;
	}
	
	public String[] getDisplayColumns()
	{
		return new String[]{"Name", "Artist", "Album", "Location"};
	}
	
	public String[][] getPlaylistDisplayData()
	{
		String[][] result = new String[playlists.size()][2];
		Iterator iter = playlists.iterator();
		int k = 0;
		while(iter.hasNext())
		{
			Map<String,Object> val = (Map<String,Object>)iter.next();
			List<Object> list = (List<Object>)val.get("Playlist Items");
			if(list == null)
				result[k] = new String[]{(String)val.get("Name"), "0"};
			else
				result[k] = new String[]{(String)val.get("Name"), String.valueOf(list.size())};
			k++;
		}
		return result;
	}
	
	public String[] getPlaylistDisplayColumns()
	{
		return new String[]{"Playlist Name", "Size"};
	}
	
	public static Map<String,Object> handleDict(Node node)
	{
		Map<String,Object> result = new HashMap<String,Object>();
		NodeList children = node.getChildNodes();
		String key = null;
		for(int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE)
			{
				if(child.getNodeName().equals("key"))
				{
					key = child.getTextContent();
				}
				else if(key != null)
				{
					if(child.getNodeName().equals("dict"))
					{
						result.put(key, iTunesLibrary.handleDict(child));
					}
					else if(child.getNodeName().equals("array"))
					{
						result.put(key, iTunesLibrary.handleArray(child));
					}
					else if(child.getNodeName().equals("true"))
					{
						result.put(key, true);
					}
					else if(child.getNodeName().equals("false"))
					{
						result.put(key, false);
					}
					else if(child.getNodeName().equals("integer"))
					{
						try
						{
							result.put(key, Integer.parseInt(child.getTextContent()));
						}
						catch(NumberFormatException x)
						{
							try
							{
								result.put(key, Long.parseLong(child.getTextContent()));
							}
							catch(NumberFormatException x2)
							{
								result.put(key, child.getTextContent());
							}
						}
					}
					else
					{
						result.put(key, child.getTextContent());
					}
					key = null;
				}
			}
		}
		return result;
	}
	
	public static List<Object> handleArray(Node node)
	{
		List<Object> result = new ArrayList<Object>();
		NodeList children = node.getChildNodes();
		for(int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE)
			{
				if(child.getNodeName().equals("dict"))
				{
					result.add(iTunesLibrary.handleDict(child));
				}
				else if(child.getNodeName().equals("array"))
				{
					result.add(iTunesLibrary.handleArray(child));
				}
				else
				{
					result.add(child.getTextContent());
				}
			}
		}
		return result;
	}

	public class Track extends File
	{
		protected String nameOnly;
		protected String extension;
		protected String relativePath;
		protected String noExtension;
		
		public Track(URI uri, Map<String,Object> trackDict)
		{
			super(uri.getPath().substring(1));
			int dot = getName().lastIndexOf('.');
			nameOnly = getName().substring(0, dot);
			extension = getName().substring(dot+1);
			relativePath = getAbsolutePath().replace("\\", "/");
			if(relativePath.startsWith(iTunesLibrary.this.path) || relativePath.substring(1).startsWith(iTunesLibrary.this.path.substring(1)))
			{
				relativePath = relativePath.substring(iTunesLibrary.this.path.length());
			}
			else
			{
				relativePath = (trackDict.get("Artist")!=null ? trackDict.get("Artist")+"/" : "") + (trackDict.get("Album")!=null ? trackDict.get("Album")+"/" : "") + getName();
			}
			noExtension = relativePath.substring(0, relativePath.lastIndexOf('.'));
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
			if(other instanceof DeviceLibrary.Track)
			{
				return getRelativePathNoExtension().equals(((DeviceLibrary.Track)other).getRelativePathNoExtension());
			}
			else if(other instanceof iTunesLibrary.Track)
			{
				return super.equals(other);
			}
			else
				return false;
		}
	}
	
	public static class InvalidLibraryException extends Exception
	{
		public InvalidLibraryException(String message)
		{
			super(message);
		}
		public InvalidLibraryException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}
}