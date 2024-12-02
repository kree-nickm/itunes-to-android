import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.beans.*;
import java.util.*;
import java.util.function.*;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.filechooser.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class iTunesToAndroid
{
  public static void main(String[] args) throws Exception
  {
    new iTunesToAndroid();
  }
  
  protected GuiWindow window;
  protected Map<String,GuiCanvas> guiMap;
  protected iTunesLibrary localLibrary;
  protected Set<iTunesLibrary.Track> filesNotOnDevice;
  protected Set<DeviceLibrary.Track> filesNotInPlaylists;
  protected Preferences prefs;
  protected Map<String,Device> devices;
  protected String selectedDeviceSerial;
  
  public iTunesToAndroid()
  {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        prefs = Preferences.userRoot().node("itunes-to-android");
        devices = new HashMap<String,Device>();
        window = new GuiWindow("iTunes to Android", iTunesToAndroid.this);
        constructGUIs();
        setCanvas("FrontEnd");
      }
    });
  }
  
  public void buildDifferences()
  {
    Device device = devices.get(selectedDeviceSerial);
    if(filesNotOnDevice == null)
      filesNotOnDevice = new HashSet<iTunesLibrary.Track>();
    else
      filesNotOnDevice.clear();
    if(filesNotInPlaylists == null)
      filesNotInPlaylists = new HashSet<DeviceLibrary.Track>();
    else
      filesNotInPlaylists.clear();
    
    // Verify that the device library and iTunes library have both been loaded.
    if(localLibrary == null || !localLibrary.loaded || localLibrary.getSelectedTracks() == null || device.library == null || !device.library.loaded)
      return;
    
    // Iterate through all files in the selected playlists and see which ones are not on the device.
    for(iTunesLibrary.Track localTrack : localLibrary.getSelectedTracks())
    {
      boolean found = false;
      for(DeviceLibrary.Track remoteTrack : device.library.deviceFiles)
      {
        if(localTrack.equals(remoteTrack))
        {
          found = true;
          break;
        }
      }
      if(!found)
        filesNotOnDevice.add(localTrack);
    }
    
    // Iterate through all files on the device and see which ones are not in the selected playlists.
    for(DeviceLibrary.Track remoteTrack : device.library.deviceFiles)
    {
      boolean found = false;
      for(iTunesLibrary.Track localTrack : localLibrary.getSelectedTracks())
      {
        if(remoteTrack.equals(localTrack))
        {
          found = true;
          break;
        }
      }
      if(!found)
        filesNotInPlaylists.add(remoteTrack);
    }
    
    // Report.
    System.out.println(filesNotOnDevice.size() +" files to copy. "+ filesNotInPlaylists.size() +" files to remove.");
  }
  
  public void constructGUIs()
  {
    guiMap = new HashMap<String,GuiCanvas>();
    
    guiMap.put("FrontEnd", new GuiCanvas() {
      protected JTabbedPane tabs;
      protected JScrollPane libraryScroller;
      protected JScrollPane playlistScroller;
      protected JScrollPane playlistFilesScroller;
      protected JScrollPane deviceFilesScroller;
      protected JScrollPane notOnDeviceScroller;
      protected JScrollPane notInPlaylistsScroller;
      protected JScrollPane deviceListScroller;
      protected JTable libraryTable;
      protected JTable playlistTable;
      protected JTable playlistFilesTable;
      protected JTable deviceFilesTable;
      protected JTable notOnDeviceTable;
      protected JTable notInPlaylistsTable;
      protected JTable deviceListTable;
      protected JButton collectPlaylists;
      protected JButton copyToDevice;
      protected JButton deleteFromDevice;
      protected JButton itunesLoadButton;
      protected JButton itunesBrowseButton;
      protected JPanel playlistPanel;
      protected JPanel setupPanel;
      protected JFileChooser itunesChooser;
      
      protected String[] playlistNames = new String[0];
      
      protected JDialog progressDialog;
      protected JProgressBar filesProgress;
      protected JProgressBar transferProgress;
      protected JTextArea progressLog;
      protected JScrollPane progressLogScroller;
      
      protected JDialog deleteDialog;
      protected JProgressBar deleteProgress;
      
      protected SwingWorker transferTask;
      protected SwingWorker deleteTask;
      
      protected short prevScanStatus = -2;
      
      protected void create()
      {
        //---------------------------------------------------------------------
        // Set up the tabs of the window.
        //---------------------------------------------------------------------
        tabs = new JTabbedPane();
        
        //-------------------------------------------------------------------
        //---- The "Setup" tab.
        //-------------------------------------------------------------------
        setupPanel = new JPanel(new GuiLayout());
        
        //-----------------------------------------------------------------
        //-------- Loading of an iTunes library.
        //-----------------------------------------------------------------
        JLabel iTunesLabel = new JLabel("Select iTunes Library XML File:");
        
        JTextField itunesInput = new JTextField(prefs.get("iTunesXMLFile", System.getProperty("user.home") +"\\Music\\iTunes\\iTunes Music Library.xml"));
        itunesChooser = new JFileChooser(prefs.get("iTunesXMLPath", System.getProperty("user.home") +"\\Music\\iTunes\\"));
        itunesChooser.setFileFilter(new FileNameExtensionFilter("XML File", "xml"));
        
        itunesLoadButton = new JButton("Load");
        itunesLoadButton.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            loadLibraryFromInput(itunesInput.getText(), true);
          }
        });
        
        itunesBrowseButton = new JButton("Browse");
        itunesBrowseButton.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            int returnVal = itunesChooser.showOpenDialog(window);
            if(returnVal == JFileChooser.APPROVE_OPTION)
            {
              itunesInput.setText(itunesChooser.getSelectedFile().getAbsolutePath());
              loadLibraryFromInput(itunesInput.getText(), true);
            }
          }
        });
        
        //-----------------------------------------------------------------
        //-------- Loading of an Android device.
        //-----------------------------------------------------------------
        // Device list.
        JLabel deviceLabel = new JLabel("Select Connected Android Device:");
        JButton deviceScanButton = new JButton("Scan Devices");
        deviceScanButton.addActionListener(new ActionListener(){
          // Get the list of connected devices from ADB.
          public void actionPerformed(ActionEvent e)
          {
            scanDevices();
          }
        });
        
        selectedDeviceSerial = prefs.get("DeviceSerial", "");
        deviceListTable = new JTable(new DefaultTableModel(new String[]{"Serial #","Status","Product","Model","Device","USB"}, 0)){
          public boolean isCellEditable(int row, int column) {return false;}
        };
        deviceListTable.setFillsViewportHeight(true);
        deviceListTable.setAutoCreateRowSorter(true);
        deviceListTable.getRowSorter().toggleSortOrder(0);
        deviceListScroller = new JScrollPane(deviceListTable);
        
        // Path to device music directory and Load button.
        JLabel devicePathLabel = new JLabel("Select Device Music Directory:");
        JTextField devicePathInput = new JTextField(prefs.get("DeviceMusicPath", ""));
        devicePathInput.setEditable(false);
        
        JButton deviceLoadButton = new JButton("Load");
        deviceLoadButton.addActionListener(new ActionListener(){
          // Load the device music library from the selected device and directory.
          public void actionPerformed(ActionEvent e)
          {
            loadDeviceFromInput(devicePathInput.getText(), true);
          }
        });
        
        JTree deviceDirectoryTree = new JTree(new DefaultMutableTreeNode("/"));
        deviceDirectoryTree.setRootVisible(false);
        deviceDirectoryTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        deviceDirectoryTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener(){
          // Code for when the user selects a directory.
          public void valueChanged(TreeSelectionEvent e)
          {
            // Determine path from node selection.
            StringBuffer result = new StringBuffer("/");
            for(Object treeNode : e.getPath().getPath())
            {
              String dir = treeNode.toString();
              if(!"/".equals(dir))
                result.append(dir + "/");
            }
            String path = result.toString();
  
            // Populate the sub directories.
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.getPath().getLastPathComponent();
            node.removeAllChildren();
            for(String dir : devices.get(selectedDeviceSerial).ls(path))
            {
              node.add(new DefaultMutableTreeNode(dir));
            }
            if(node.getChildCount() > 0)
              deviceDirectoryTree.scrollPathToVisible(new TreePath(((DefaultMutableTreeNode)node.getFirstChild()).getPath()));
            devicePathInput.setText(path);
          }
        });
        JScrollPane deviceTreeScroller = new JScrollPane(deviceDirectoryTree);
        
        deviceListTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
          // Code for when the user selects a device.
          public void valueChanged(ListSelectionEvent e)
          {
            if(deviceListTable.getSelectedRow() > -1)
            {
              String serial = (String)deviceListTable.getValueAt(deviceListTable.getSelectedRow(), 0);
              Device formerSelectedDevice = devices.get(serial);
              if(formerSelectedDevice != null && !serial.equals(selectedDeviceSerial))
              {
                System.out.println(serial);
                selectedDeviceSerial = serial;
                Device selectedDevice = devices.get(selectedDeviceSerial);
                DefaultMutableTreeNode root = (DefaultMutableTreeNode)deviceDirectoryTree.getModel().getRoot();
                root.removeAllChildren();
                if(selectedDevice.available)
                {
                  for(String dir : selectedDevice.ls())
                  {
                    root.add(new DefaultMutableTreeNode(dir));
                  }
                  if(root.getChildCount() > 0)
                    deviceDirectoryTree.scrollPathToVisible(new TreePath(((DefaultMutableTreeNode)root.getFirstChild()).getPath()));
                }
              }
            }
          }
        });
        
        // Finish by adding all the components to the tab.
        setupPanel.add(iTunesLabel, new GuiLayout.GuiConstraint(setupPanel,     "0",  "0",  "200",  "25", iTunesLabel));
        setupPanel.add(itunesInput, new GuiLayout.GuiConstraint(setupPanel,     "200",  "0",  "400",  "25", itunesInput));
        setupPanel.add(itunesLoadButton, new GuiLayout.GuiConstraint(setupPanel,  "600",  "0",  "100",  "25", itunesLoadButton));
        setupPanel.add(itunesBrowseButton, new GuiLayout.GuiConstraint(setupPanel,  "700",  "0",  "100",  "25", itunesBrowseButton));
        JSeparator separator1 = new JSeparator();
        setupPanel.add(separator1, new GuiLayout.GuiConstraint(setupPanel,     "5%",  "35",  "90%",  "5", separator1));
        setupPanel.add(deviceLabel, new GuiLayout.GuiConstraint(setupPanel,     "0",  "45",  "200",  "25", deviceLabel));
        setupPanel.add(deviceScanButton, new GuiLayout.GuiConstraint(setupPanel,  "200",  "45",  "170",  "25", deviceScanButton));
        setupPanel.add(deviceListScroller, new GuiLayout.GuiConstraint(setupPanel,    "0",  "70", "100%", "70", deviceListScroller));
        JSeparator separator2 = new JSeparator();
        setupPanel.add(separator2, new GuiLayout.GuiConstraint(setupPanel,     "5%",  "150",  "90%",  "5", separator2));
        setupPanel.add(devicePathLabel, new GuiLayout.GuiConstraint(setupPanel,     "0",  "160",  "200",  "25", devicePathLabel));
        setupPanel.add(devicePathInput, new GuiLayout.GuiConstraint(setupPanel,     "200",  "160",  "400",  "25", devicePathInput));
        setupPanel.add(deviceLoadButton, new GuiLayout.GuiConstraint(setupPanel,  "600",  "160",  "100",  "25", deviceLoadButton));
        setupPanel.add(deviceTreeScroller, new GuiLayout.GuiConstraint(setupPanel,    "0",  "185", "100%", "0,bottom", deviceTreeScroller));
        
        //---------------------------------------------------------------------
        //---- The "Sync" tab.
        //---------------------------------------------------------------------
        playlistPanel = new JPanel(new GuiLayout());
        
        //-----------------------------------------------------------------
        //-------- List of playlists.
        //-----------------------------------------------------------------
        playlistTable = new JTable(new DefaultTableModel()){
          public boolean isCellEditable(int row, int column) {return false;}
        };
        playlistTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
          public void valueChanged(ListSelectionEvent e)
          {
            collectPlaylists.setEnabled(playlistTable.getSelectedRowCount() > 0);
          }
        });
        playlistScroller = new JScrollPane(playlistTable);
        
        collectPlaylists = new JButton("Build File List From Playlists");
        collectPlaylists.setEnabled(false);
        collectPlaylists.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            int[] rows = playlistTable.getSelectedRows();
            playlistNames = new String[rows.length];
            for(int i = 0; i < rows.length; i++)
            {
              playlistNames[i] = (String)playlistTable.getValueAt(rows[i], 0);
            }
            setupTransferTables();
          }
        });
        
        //-----------------------------------------------------------------
        //-------- List of files in the selected playlists.
        //-----------------------------------------------------------------
        playlistFilesTable = new JTable(new DefaultTableModel(new String[]{"Local Files In Playlists"}, 0)){
          public boolean isCellEditable(int row, int column) {return false;}
        };
        playlistFilesTable.setFillsViewportHeight(true);
        playlistFilesTable.setAutoCreateRowSorter(true);
        playlistFilesTable.getRowSorter().toggleSortOrder(0);
        playlistFilesScroller = new JScrollPane(playlistFilesTable);
        
        //-----------------------------------------------------------------
        //-------- List of files on the Android device.
        //-----------------------------------------------------------------
        deviceFilesTable = new JTable(new DefaultTableModel(new String[]{"Device Files"}, 0)){
          public boolean isCellEditable(int row, int column) {return false;}
        };
        deviceFilesTable.setFillsViewportHeight(true);
        deviceFilesTable.setAutoCreateRowSorter(true);
        deviceFilesTable.getRowSorter().toggleSortOrder(0);
        deviceFilesScroller = new JScrollPane(deviceFilesTable);
        
        //-----------------------------------------------------------------
        //-------- List of files in the iTunes playlist(s), but not on the Android device.
        //-----------------------------------------------------------------
        notOnDeviceTable = new JTable(new DefaultTableModel(new String[]{"Files To Copy To Device"}, 0)){
          public boolean isCellEditable(int row, int column) {return false;}
        };
        notOnDeviceTable.setFillsViewportHeight(true);
        notOnDeviceTable.setAutoCreateRowSorter(true);
        notOnDeviceTable.getRowSorter().toggleSortOrder(0);
        notOnDeviceScroller = new JScrollPane(notOnDeviceTable);
        
        copyToDevice = new JButton("copyToDevice");
        copyToDevice.setEnabled(false);
        copyToDevice.addActionListener(new ActionListener(){
          //---------------------------------------------------------------
          //------------ Copy files to device.
          //---------------------------------------------------------------
          public void actionPerformed(ActionEvent e)
          {
            filesProgress.setMaximum(filesNotOnDevice.size());
            filesProgress.setValue(0);
            transferProgress.setValue(0);
            progressLog.setText("");
            progressDialog.setVisible(true);
            
            transferTask = new SwingWorker<Void,Void>() {
              public Void doInBackground()
              {
                Device device = devices.get(selectedDeviceSerial);
                if(device == null || device.library == null || !device.available)
                  return null;
                
                // Copy Tracks
                iTunesLibrary.Track file = null;
                int k = 0;
                for(iTunesLibrary.Track localTrack : filesNotOnDevice)
                {
                  if(isCancelled())
                    break;
                  
                  firePropertyChange("filenumber", k, ++k);
                  firePropertyChange("file", (file!=null ? file.getRelativePath() : ""), (file = localTrack).getRelativePath());
                  firePropertyChange("copied", k>1 ? 100 : 0, 0);
                  
                  try
                  {
                    String[] result = Device.cmd("adb", "push", file.getAbsolutePath(), device.library.path + iTunesToAndroid.Track.sanitizePath(file.getRelativePath()));
                    int progress = 0;
                    for(int i=0; i<result.length; i++)
                    {
                      try
                      {
                        firePropertyChange("copied", progress, progress = Integer.parseInt(result[i].substring(1, result[i].indexOf('%')).trim()));
                      }
                      catch(NumberFormatException x1)
                      {
                        firePropertyChange("log", "", result[i]);
                      }
                      catch(StringIndexOutOfBoundsException x2)
                      {
                        firePropertyChange("log", "", result[i]);
                      }
                    }
                  }
                  catch(IOException x)
                  {
                    System.out.println(x);
                    firePropertyChange("log", "", "IOException caught trying to run command for file: "+ file);
                  }
                }
                
                // Create placeholder playlists and copy them.
                for(String playlistName : playlistNames)
                {
                  try
                  {
                    iTunesLibrary.Playlist playlist = localLibrary.playlists.get(playlistName);
                    if(playlist != null)
                    {
                      File playlistFile = playlist.writeDeviceM3U();
                      String[] result = Device.cmd("adb", "push", playlistFile.getAbsolutePath(), device.library.path + playlistName + "--iTunes.m3u");
                      int progress = 0;
                      for(int i=0; i<result.length; i++)
                      {
                        System.out.println("Playlist copied: " + result[i]);
                        firePropertyChange("log", "", result[i]);
                      }
                    }
                    else
                    {
                      System.out.println("Failed to copy invalid playlist: " + playlistName);
                    }
                  }
                  catch(IOException x)
                  {
                    System.out.println(x);
                  }
                }
                
                return null;
              }
              
              public void done()
              {
                System.out.println("File transfer thread finished."+ (isCancelled() ? " Cancelled prematurely." : ""));
                progressLog.append("Finished.");
                //progressDialog.setVisible(false);
                try
                {
                  Device device = devices.get(selectedDeviceSerial);
                  if(device != null)
                    device.library.buildFileList();
                }
                catch(Device.DeviceMissingException x1)
                {
                  JOptionPane.showMessageDialog(window, x1.getMessage(), "Error Accessing Device", JOptionPane.ERROR_MESSAGE);
                  System.out.println(x1);
                }
                catch(Device.ADBException x2)
                {
                  JOptionPane.showMessageDialog(window, x2.getMessage(), "Error Running ADB", JOptionPane.ERROR_MESSAGE);
                  System.out.println(x2);
                }
                setupTransferTables();
              }
            };
            
            // Listener for updating the popup's display to show the progess of the transfer.
            transferTask.addPropertyChangeListener(new PropertyChangeListener(){
              public void propertyChange(PropertyChangeEvent e)
              {
                if("copied".equals(e.getPropertyName()))
                {
                  transferProgress.setValue((Integer)e.getNewValue());
                }
                else if("filenumber".equals(e.getPropertyName()))
                {
                  int n = (Integer)e.getNewValue();
                  filesProgress.setValue(n);
                  filesProgress.setString("File "+ n +" of "+ filesProgress.getMaximum());
                }
                else if("file".equals(e.getPropertyName()))
                {
                  transferProgress.setString((String)e.getNewValue());
                }
                else if("log".equals(e.getPropertyName()))
                {
                  System.out.println(e.getNewValue());
                  progressLog.append((String)e.getNewValue() + System.getProperty("line.separator"));
                }
              }
            });
            
            transferTask.execute();
          }
        });
        
        //-----------------------------------------------------------------
        //-------- List of files on the Android device, but not in the iTunes playlist(s).
        //-----------------------------------------------------------------
        notInPlaylistsTable = new JTable(new DefaultTableModel(new String[]{"Files To Remove From Device"}, 0)){
          public boolean isCellEditable(int row, int column) {return false;}
        };
        notInPlaylistsTable.setFillsViewportHeight(true);
        notInPlaylistsTable.setAutoCreateRowSorter(true);
        notInPlaylistsTable.getRowSorter().toggleSortOrder(0);
        notInPlaylistsScroller = new JScrollPane(notInPlaylistsTable);
        
        deleteFromDevice = new JButton("deleteFromDevice");
        deleteFromDevice.setEnabled(false);
        deleteFromDevice.addActionListener(new ActionListener(){
          //---------------------------------------------------------------
          //------------ Delete files from device.
          //---------------------------------------------------------------
          public void actionPerformed(ActionEvent e)
          {
            deleteProgress.setMaximum(filesNotInPlaylists.size());
            deleteProgress.setValue(0);
            deleteDialog.setVisible(true);
            
            deleteTask = new SwingWorker<Void,Void>() {
              public Void doInBackground()
              {
                Iterator<DeviceLibrary.Track> iter = filesNotInPlaylists.iterator();
                String file = null;
                int k = 0;
                while(!isCancelled() && iter.hasNext())
                {
                  firePropertyChange("filenumber", k, ++k);
                  firePropertyChange("file", file, file = iter.next().getAbsolutePath());
                  try
                  {
                    String[] result = Device.cmd("adb", "shell", "rm", "\""+ file
                      .replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("(", "\\(")
                      .replace(")", "\\)")
                      .replace("<", "\\<")
                      .replace(">", "\\>")
                      .replace("|", "\\|")
                      .replace(";", "\\;")
                      .replace("&", "\\&")
                      .replace("*", "\\*")
                      .replace("~", "\\~")
                      .replace("'", "\\'")
                      .replace(" ", "\\ ")
                    +"\"");
                    for(String s : result)
                    {
                      System.out.println(s);
                    }
                  }
                  catch(IOException x)
                  {
                    System.out.println(x);
                  }
                }
                return null;
              }
              
              public void done()
              {
                System.out.println("File delete thread finished."+ (isCancelled() ? " Cancelled prematurely." : ""));
                deleteDialog.setVisible(false);
                try
                {
                  Device device = devices.get(selectedDeviceSerial);
                  if(device != null)
                    device.library.buildFileList();
                }
                catch(Device.DeviceMissingException x1)
                {
                  JOptionPane.showMessageDialog(window, x1.getMessage(), "Error Accessing Device", JOptionPane.ERROR_MESSAGE);
                  System.out.println(x1);
                }
                catch(Device.ADBException x2)
                {
                  JOptionPane.showMessageDialog(window, x2.getMessage(), "Error Running ADB", JOptionPane.ERROR_MESSAGE);
                  System.out.println(x2);
                }
                setupTransferTables();
              }
            };
            
            // Listener for updating the popup's display to show the progess of the deletion.
            deleteTask.addPropertyChangeListener(new PropertyChangeListener(){
              public void propertyChange(PropertyChangeEvent e)
              {
                if("filenumber".equals(e.getPropertyName()))
                {
                  int n = (Integer)e.getNewValue();
                  deleteProgress.setValue(n);
                  deleteProgress.setString("File "+ n +" of "+ deleteProgress.getMaximum());
                }
              }
            });
            
            deleteTask.execute();
          }
        });
        
        // Finish by adding all the components to the tab.
        playlistPanel.add(deviceFilesScroller, new GuiLayout.GuiConstraint(playlistPanel, "0",  "0",  "33%",  "0,bottom", deviceFilesScroller));
        playlistPanel.add(playlistScroller, new GuiLayout.GuiConstraint(playlistPanel,    "33%",  "0",  "33%",  "200", playlistScroller));
        playlistPanel.add(collectPlaylists, new GuiLayout.GuiConstraint(playlistPanel,    "33%",  "200",  "33%",  "30", collectPlaylists));
        playlistPanel.add(playlistFilesScroller, new GuiLayout.GuiConstraint(playlistPanel, "33%",  "230",  "33%",  "0,bottom", playlistFilesScroller));
        playlistPanel.add(notOnDeviceScroller, new GuiLayout.GuiConstraint(playlistPanel, "66%",  "0",  "34%",  "45%", notOnDeviceScroller));
        playlistPanel.add(copyToDevice, new GuiLayout.GuiConstraint(playlistPanel,      "66%",  "45%",  "34%",  "5%", copyToDevice));
        playlistPanel.add(notInPlaylistsScroller, new GuiLayout.GuiConstraint(playlistPanel,"66%",  "50%",  "34%",  "45%", notInPlaylistsScroller));
        playlistPanel.add(deleteFromDevice, new GuiLayout.GuiConstraint(playlistPanel,    "66%",  "95%",  "34%",  "5%", deleteFromDevice));
        
        //---------------------------------------------------------------------
        //---- The "iTunes Library" tab.
        //---------------------------------------------------------------------
        libraryTable = new JTable(new DefaultTableModel()){
          public boolean isCellEditable(int row, int column) {return false;}
        };
        libraryTable.setFillsViewportHeight(true);
        libraryTable.setAutoCreateRowSorter(true);
        libraryScroller = new JScrollPane(libraryTable);
        
        // Finish by adding all the tabs to the pane, and the pane to the canvas.
        tabs.addTab("Setup", setupPanel);
        tabs.addTab("Sync", playlistPanel);
        tabs.addTab("iTunes Library", libraryScroller);
        add(tabs, new GuiLayout.GuiConstraint(this, "0,left", "0,top",  "0,right",  "0,bottom", tabs));
        
        //---------------------------------------------------------------------
        // Create the popup that will show transfer progress.
        //---------------------------------------------------------------------
        progressDialog = new JDialog(window, "Copying Files");
        progressDialog.setLocationRelativeTo(window);
        progressDialog.setLayout(new GuiLayout());
        progressDialog.addWindowListener(new WindowAdapter(){
          public void windowClosed(WindowEvent e)
          {
            transferTask.cancel(false);
          }
          public void windowClosing(WindowEvent e)
          {
            transferTask.cancel(false);
          }
        });
        
        filesProgress = new JProgressBar(0, 0);
        filesProgress.setStringPainted(true);
        transferProgress = new JProgressBar(0, 100);
        transferProgress.setStringPainted(true);
        progressLog = new JTextArea();
        progressLog.setEditable(false);
        progressLogScroller = new JScrollPane(progressLog);
        progressDialog.add(filesProgress, new GuiLayout.GuiConstraint(progressDialog, "0,center", "5",  "300",  "40", filesProgress));
        progressDialog.add(transferProgress, new GuiLayout.GuiConstraint(progressDialog,"0,center", "55", "300",  "40", transferProgress));
        progressDialog.add(progressLogScroller, new GuiLayout.GuiConstraint(progressDialog,"0", "105",  "600",  "200", progressLogScroller));
        progressDialog.pack();
        
        //---------------------------------------------------------------------
        // Create the popup that will show deleting progress.
        //---------------------------------------------------------------------
        deleteDialog = new JDialog(window, "Deleting Files");
        deleteDialog.setLayout(new GuiLayout());
        deleteDialog.setLocationRelativeTo(window);
        deleteDialog.addWindowListener(new WindowAdapter(){
          public void windowClosed(WindowEvent e)
          {
            deleteTask.cancel(false);
          }
          public void windowClosing(WindowEvent e)
          {
            deleteTask.cancel(false);
          }
        });
        
        deleteProgress = new JProgressBar(0, 0);
        deleteProgress.setStringPainted(true);
        deleteDialog.add(deleteProgress, new GuiLayout.GuiConstraint(deleteDialog,  "0",  "0",  "300",  "40", deleteProgress));
        deleteDialog.pack();
        
        //---------------------------------------------------------------------
        // Populate the components and load any auto-filled inputs.
        //---------------------------------------------------------------------
        loadLibraryFromInput(itunesInput.getText(), false);
        scanDevices();
        loadDeviceFromInput(devicePathInput.getText(), false);
      }
      
      public void scanDevices()
      {
        Collection<Device> currentDevices = devices.values();
        for(Device device : currentDevices)
        {
          device.available = false;
          device.status = "disconnected";
        }
        String[][] data;
        
        try
        {
          String[] output = Device.cmd("adb", "devices", "-l");
          for(int i=1; i<output.length; i++)
          {
            String serial = Device.getSerialFromLine(output[i]);
            if(serial != null)
            {
              Device device = devices.get(serial);
              if(device == null)
              {
                devices.put(serial, new Device(output[i]));
              }
              else
              {
                device.load(output[i]);
              }
            }
          }
          
          currentDevices = devices.values();
          data = new String[currentDevices.size()][Device.displayHeaders.length];
          int i = 0;
          for(Device device : currentDevices)
          {
            data[i] = device.displayData;
            i++;
          }
        }
        catch(IOException x)
        {
          data = new String[0][Device.displayHeaders.length];
          System.out.println(x);
        }
        ((DefaultTableModel)deviceListTable.getModel()).setDataVector(data, Device.displayHeaders);
        // TODO: Set selected row based on selectedDeviceSerial
      }
      
      public void loadLibraryFromInput(String filename, boolean popupResults)
      {
        try
        {
          localLibrary = new iTunesLibrary(filename);
          setupLibraryComponents();
          prefs.put("iTunesXMLFile", localLibrary.libraryFile.getAbsolutePath());
          prefs.put("iTunesXMLPath", localLibrary.libraryFile.getParentFile().getAbsolutePath());
          if(popupResults)
            JOptionPane.showMessageDialog(window, "The iTunes library has been successfully loaded.", "Library Loaded", JOptionPane.INFORMATION_MESSAGE);
          System.out.println("The iTunes library has been successfully loaded.");
        }
        catch(iTunesLibrary.InvalidLibraryException x)
        {
          if(popupResults)
            JOptionPane.showMessageDialog(window, x.getMessage(), "Error Loading Library", JOptionPane.WARNING_MESSAGE);
          System.out.println(x);
        }
      }
      
      public void loadDeviceFromInput(String path, boolean popupResults)
      {
        Device device = devices.get(selectedDeviceSerial);
        if(device != null && !"".equals(path) && device.available)
        {
          try
          {
            device.createLibrary(path);
            setupDeviceComponents();
            prefs.put("DeviceSerial", selectedDeviceSerial);
            prefs.put("DeviceMusicPath", path);
            if(popupResults)
              JOptionPane.showMessageDialog(window, "The device library has been successfully loaded.", "Library Loaded", JOptionPane.INFORMATION_MESSAGE);
            System.out.println("The device library has been successfully loaded.");
          }
          catch(Exception x)
          {
            if(popupResults)
              JOptionPane.showMessageDialog(window, x.getMessage(), "Error Loading Library", JOptionPane.WARNING_MESSAGE);
            System.out.println(x);
          }
        }
        else
        {
          if(popupResults)
            JOptionPane.showMessageDialog(window, "Invalid device or directory selection.", "Error Loading Library", JOptionPane.WARNING_MESSAGE);
        }
      }
      
      /**
       * Fill up the iTunes library track list tab, and the playlist selector.
       */
      public void setupLibraryComponents()
      {
        if(localLibrary != null && localLibrary.loaded)
        {
          ((DefaultTableModel)libraryTable.getModel()).setDataVector(localLibrary.getTrackDisplayData(), iTunesLibrary.Track.displayHeaders);
          ((DefaultTableModel)playlistTable.getModel()).setDataVector(localLibrary.getPlaylistDisplayData(), iTunesLibrary.Playlist.displayHeaders);
          tabs.setEnabledAt(2, true);
        }
        else
        {
          ((DefaultTableModel)libraryTable.getModel()).setRowCount(0);
          ((DefaultTableModel)playlistTable.getModel()).setRowCount(0);
          tabs.setEnabledAt(2, false);
        }
      }
      
      /**
       * Fill up the device library track list.
       */
      public void setupDeviceComponents()
      {
        Device device = devices.get(selectedDeviceSerial);
        if(device.library != null && device.library.loaded)
        {
          ((DefaultTableModel)deviceFilesTable.getModel()).setDataVector(device.library.getDisplayData(), new String[]{"Device Files"});
        }
        else
        {
          ((DefaultTableModel)deviceFilesTable.getModel()).setRowCount(0);
        }
        setupTransferTables();
      }
      
      public void setupTransferTables()
      {
        // Populate the table that displays all of the tracks that are contained in the selected iTunes playlists.
        ((DefaultTableModel)playlistFilesTable.getModel()).setRowCount(0);
        if(localLibrary != null && localLibrary.loaded)
        {
          localLibrary.updateSelectedTracks(playlistNames);
          if(localLibrary.getSelectedTracks() != null)
          {
            Iterator<iTunesLibrary.Track> iter = localLibrary.getSelectedTracks().iterator();
            while(iter.hasNext())
              ((DefaultTableModel)playlistFilesTable.getModel()).addRow(new String[]{iter.next().getRelativePath()});
          }
        }
        
        // Populate the tables that show all files that need to be copied and all files that can be deleted.
        ((DefaultTableModel)notOnDeviceTable.getModel()).setRowCount(0);
        ((DefaultTableModel)notInPlaylistsTable.getModel()).setRowCount(0);
        buildDifferences();
        Iterator<iTunesLibrary.Track> iter2 = filesNotOnDevice.iterator();
        while(iter2.hasNext())
          ((DefaultTableModel)notOnDeviceTable.getModel()).addRow(new String[]{iter2.next().getRelativePath()});
        Iterator<DeviceLibrary.Track> iter3 = filesNotInPlaylists.iterator();
        while(iter3.hasNext())
          ((DefaultTableModel)notInPlaylistsTable.getModel()).addRow(new String[]{iter3.next().getRelativePath()});
        
        // Set status of the copy/delete buttons based on whether there is anything to copy/delete.
        copyToDevice.setEnabled(notOnDeviceTable.getRowCount() > 0);
        deleteFromDevice.setEnabled(notInPlaylistsTable.getRowCount() > 0);
      }
    });
  }
  
  public void setCanvas(String canvas)
  {
    window.setCanvas(guiMap.get(canvas));
  }
  
  public void exit()
  {
    try
    {
      prefs.flush();
    }
    catch(BackingStoreException x)
    {
    }
    System.exit(0);
  }
  
  public static interface Track
  {
    public static String sanitizePath(String path)
    {
      path = path.replaceAll("[^-_ !'()/\\.a-zA-Z0-9]", "_").replaceAll("^\\.+", "").replaceAll("/\\.+", "/").replaceAll("\\.+$", "").replaceAll("\\.+/", "/");
      return path;
    }
    
    public static String normalizePath(String path)
    {
      path = sanitizePath(path).toLowerCase();
      return path;
    }
    
    public String getName();
    
    public String getAbsolutePath();
    
    public String getNameOnly();
    
    public String getExtension();
    
    public String getRelativePath();
    
    public String getRelativePathNoExtension();
    
    public Track getPairedTrack();
  }
}
