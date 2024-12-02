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
  public String musicPath;
  public Map<String,Track> tracks;
  public Map<String,Playlist> playlists;
  protected Set<Track> selectedTracks;
  
  public iTunesLibrary(String filePath) throws InvalidLibraryException
  {
    // Create a File object for the library file.
    libraryFile = new File(filePath);
    if(!libraryFile.isFile())
    {
      throw new InvalidLibraryException("The path \""+ filePath +"\" is not a file.");
    }
    if(!libraryFile.canRead())
    {
      throw new InvalidLibraryException("The path \""+ filePath +"\" cannot be read.");
    }
    
    // Parse the library file as XML.
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
      throw new InvalidLibraryException("ParserConfigurationException: The file \""+ filePath +"\" could not be parsed as XML. ("+ x1.getMessage() +")", x1);
    }
    catch(SAXException x2)
    {
      throw new InvalidLibraryException("SAXException: The file \""+ filePath +"\" could not be parsed as XML. ("+ x2.getMessage() +")", x2);
    }
    catch(IOException x3)
    {
      throw new InvalidLibraryException("IOException: The file \""+ filePath +"\" could not be parsed as XML. ("+ x3.getMessage() +")", x3);
    }
    
    // Traverse the library XML document to identify the tracks, playlists, etc.
    Map<String,Object> libraryXML = null;
    NodeList children = doc.getDocumentElement().getChildNodes();
    for(int i = 0; i < children.getLength(); i++)
    {
      Node child = children.item(i);
      if(child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("dict"))
      {
        libraryXML = iTunesLibrary.handleDict(child);
        break;
      }
    }
    if(libraryXML == null)
    {
      throw new InvalidLibraryException("The XML file \""+ filePath +"\" was not recognized as an iTunes library.");
    }
    
    // Verify that tracks, playlists, etc. were identified succesfully.
    Map<String,Object> tracksXML;
    if(libraryXML.get("Tracks") instanceof Map)
    {
      tracksXML = (Map<String,Object>)libraryXML.get("Tracks");
    }
    else
    {
      throw new InvalidLibraryException("No list of tracks was found in the iTunes library \""+ filePath +"\".");
    }
    
    List<Object> playlistsXML;
    if(libraryXML.get("Playlists") instanceof List)
    {
      playlistsXML = (List<Object>)libraryXML.get("Playlists");
    }
    else
    {
      throw new InvalidLibraryException("No list of playlists was found in the iTunes library \""+ filePath +"\".");
    }
    
    if(libraryXML.get("Music Folder") instanceof String)
    {
      String musicPathRaw = (String)libraryXML.get("Music Folder");
      try
      {
        musicPath = new URI(musicPathRaw).getPath().substring(1);
      }
      catch(URISyntaxException x)
      {
        throw new InvalidLibraryException("Music folder \""+ musicPathRaw +"\" defined in iTunes library \""+ filePath +"\" is not valid.");
      }
    }
    else
    {
      throw new InvalidLibraryException("No music folder was defined in this iTunes library \""+ filePath +"\".");
    }
    
    loadTracks(tracksXML);
    loadPlaylists(playlistsXML);
    
    // Success.
    System.out.println("iTunes library read into memory. "+tracks.size()+" tracks, "+playlists.size()+" playlists, music folder: " + musicPath);
    loaded = true;
  }
  
  protected void loadTracks(Map<String,Object> tracksXML)
  {
    tracks = new HashMap<String,Track>();
    Iterator<String> trackIDsIterator = tracksXML.keySet().iterator();
    while(trackIDsIterator.hasNext())
    {
      String trackID = trackIDsIterator.next();
      Map<String,Object> trackXML = (Map<String,Object>)tracksXML.get(trackID);
      String loc = (String)trackXML.get("Location");
      if(loc != null)
      {
        try
        {
          Track track = new Track(new URI(loc), trackXML);
          if(track.isFile())
          {
            tracks.put(trackID, track);
          }
          else
          {
            System.out.println("Invalid track file path in track " + trackID + ": " + loc);
          }
        }
        catch(URISyntaxException x)
        {
          System.out.println("Exception when parsing track file path in track " + trackID + ": " + x.toString());
        }
      }
      else
      {
        System.out.println("Empty track file path in track " + trackID + ".");
      }
    }
  }
  
  protected void loadPlaylists(List<Object> playlistsXML)
  {
    playlists = new HashMap<String,Playlist>();
    if(tracks == null || tracks.size() == 0)
    {
      System.out.println("Tracks must be loaded before playlists.");
      return;
    }

    Iterator<Object> playlistsXMLIterator = playlistsXML.iterator();
    while(playlistsXMLIterator.hasNext())
    {
      Map<String,Object> playlistXML = (Map<String,Object>)playlistsXMLIterator.next();
      Playlist playlist = new Playlist(playlistXML);
      if(playlist.size() > 0)
      {
        playlists.put(playlist.name, playlist);
      }
    }
  }
  
  public void updateSelectedTracks(String[] playlistNames)
  {
    if(playlistNames == null)
    {
      selectedTracks = null;
      System.out.println("Null playlist selection.");
      return;
    }
    if(playlistNames.length == 0)
    {
      selectedTracks = null;
      System.out.println("No playlists selected.");
      return;
    }
    selectedTracks = new HashSet<Track>();
    for(int i=0; i<playlistNames.length; i++)
    {
      Playlist playlist = playlists.get(playlistNames[i]);
      if(playlist != null)
        selectedTracks.addAll(playlist);
      else
        System.out.println("Invalid playlist \""+ playlistNames[i] +"\" selected.");
    }
    System.out.println(String.valueOf(selectedTracks.size()) + " files among selected playlists.");
  }
  
  public Set<Track> getSelectedTracks()
  {
    return selectedTracks;
  }
  
  public String[][] getTrackDisplayData()
  {
    String[][] result = new String[tracks.size()][Track.displayHeaders.length];
    Track[] tracksValues = tracks.values().toArray(new Track[0]);
    for(int i=0; i<tracksValues.length; i++)
      result[i] = tracksValues[i].displayData;
    return result;
  }
  
  public String[][] getPlaylistDisplayData()
  {
    String[][] result = new String[playlists.size()][Playlist.displayHeaders.length];
    Playlist[] playlistsValues = playlists.values().toArray(new Playlist[0]);
    for(int i=0; i<playlistsValues.length; i++)
      result[i] = playlistsValues[i].displayData;
    return result;
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
        else
        {
          // Encountered a value with no key.
          System.out.println("Encountered keyless child in dict while parsing library XML: " + child.getBaseURI());
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

  public class Track extends File implements iTunesToAndroid.Track
  {
    public static String[] displayHeaders = new String[]{"Name", "Artist", "Album", "Location"};
    
    protected String nameOnly;
    protected String extension;
    protected String relativePath;
    protected String noExtension;
    public DeviceLibrary.Track pairedTrack;
    public int id;
    public int size;
    public int duration;
    public int bitRate;
    public int sampleRate;
    public String hash;
    public String name;
    public String artist;
    public String album;
    public String comments;
    public String[] displayData;
    
    public Track(URI uri, Map<String,Object> trackXML)
    {
      super(uri.getPath().substring(1));
      int dot = getName().lastIndexOf('.');
      nameOnly = getName().substring(0, dot);
      extension = getName().substring(dot+1);
      relativePath = getAbsolutePath().replace("\\", "/");
      
      id = (int)trackXML.get("Track ID");
      size = (int)trackXML.get("Size");
      duration = (int)trackXML.get("Total Time");
      bitRate = (int)trackXML.get("Bit Rate");
      sampleRate = (int)trackXML.get("Sample Rate");
      hash = (String)trackXML.get("Persistent ID");
      name = (String)trackXML.get("Name");
      artist = (String)trackXML.get("Artist");
      album = (String)trackXML.get("Album");
      comments = (String)trackXML.get("Comments");
      if(comments == null)
        comments = "";
      
      if(relativePath.startsWith(iTunesLibrary.this.musicPath) || relativePath.substring(1).startsWith(iTunesLibrary.this.musicPath.substring(1)))
      {
        relativePath = relativePath.substring(iTunesLibrary.this.musicPath.length());
      }
      else
      {
        relativePath = (artist!=null ? artist+"/" : "") + (album!=null ? album+"/" : "") + getName();
      }
      noExtension = relativePath.substring(0, relativePath.lastIndexOf('.'));
      
      displayData = new String[]{name, artist, album, getAbsolutePath()};
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
    
    public DeviceLibrary.Track getPairedTrack()
    {
      return pairedTrack;
    }
    
    public boolean equals(Object other)
    {
      if(other instanceof DeviceLibrary.Track)
      {
        String myUnique = iTunesToAndroid.Track.normalizePath(getRelativePathNoExtension());
        String otherUnique = iTunesToAndroid.Track.normalizePath(((DeviceLibrary.Track)other).getRelativePathNoExtension());
        boolean equal = myUnique.equals(otherUnique);
        if(equal)
        {
          this.pairedTrack = (DeviceLibrary.Track)other;
          ((DeviceLibrary.Track)other).pairedTrack = this;
        }
        return equal;
      }
      else if(other instanceof iTunesLibrary.Track)
      {
        return super.equals(other);
      }
      else
        return false;
    }
  }
  
  public class Playlist extends ArrayList<Track>
  {
    public static String[] displayHeaders = new String[]{"Playlist Name", "Size"};
    
    public boolean master;
    public String id;
    public String hash;
    public String name;
    public String[] displayData;
    
    public Playlist(Map<String,Object> playlistXML)
    {
      List<Object> tracksXML;
      try
      {
        tracksXML = (List<Object>)playlistXML.get("Playlist Items");
        tracksXML.get(0);
      }
      catch(NullPointerException x)
      {
        tracksXML = new ArrayList<Object>();
      }
      
      Object masterRaw = playlistXML.get("Master");
      master = masterRaw != null && (boolean)masterRaw;
      id = playlistXML.get("Playlist ID").toString();
      hash = playlistXML.get("Playlist Persistent ID").toString();
      name = playlistXML.get("Name").toString();
      
      Iterator<Object> tracksXMLIterator = tracksXML.iterator();
      while(tracksXMLIterator.hasNext())
      {
        Map<String,Object> trackXML = (Map<String,Object>)tracksXMLIterator.next();
        String trackID = trackXML.get("Track ID").toString();
        Track track = iTunesLibrary.this.tracks.get(trackID);
        if(track != null)
        {
          this.add(track);
        }
        else
        {
          System.out.println("Playlist \"" + name + "\" includes unknown track ID " + trackID + ".");
        }
      }
      
      displayData = new String[]{name, String.valueOf(this.size())};
    }
    
    public File writeDeviceM3U() throws IOException
    {
      File file = new File(iTunesLibrary.this.musicPath + name + ".iTunesToAndroid.m3u");
      BufferedWriter out = new BufferedWriter(new FileWriter(file));
      out.write("#EXTM3U");
      out.newLine();
      out.write("#PLAYLIST:" + name);
      out.newLine();
      Iterator<Track> tracksIter = this.iterator();
      while(tracksIter.hasNext())
      {
        Track track = tracksIter.next();
        if(track.getPairedTrack() != null)
        {
          out.write(track.getPairedTrack().getRelativePath());
          out.newLine();
        }
      }
      out.close();
      System.out.println("Created playlist: " + file.getAbsolutePath());
      return file;
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