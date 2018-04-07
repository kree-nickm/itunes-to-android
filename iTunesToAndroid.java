import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.beans.*;
import java.util.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.table.*;
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
	protected iTunesLibrary library;
	protected DeviceLibrary deviceFiles;
	protected Set<iTunesLibrary.Track> filesNotOnDevice;
	protected Set<DeviceLibrary.Track> filesNotInPlaylists;
	
	public iTunesToAndroid()
	{
		library = new iTunesLibrary("D:\\Libraries\\Music\\iTunes\\iTunes Music Library.xml");
		deviceFiles = new DeviceLibrary("/sdcard/Music/");
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				window = new GuiWindow("iTunes to Android", iTunesToAndroid.this);
				constructGUIs();
				setCanvas("FrontEnd");
			}
		});
	}
	
	public void buildDifferences()
	{
		filesNotOnDevice = (Set<iTunesLibrary.Track>)((HashSet)library.getPlaylistFiles()).clone();
		Iterator<iTunesLibrary.Track> iter1a = filesNotOnDevice.iterator();
		while(iter1a.hasNext())
		{
			iTunesLibrary.Track next = iter1a.next();
			Iterator<DeviceLibrary.Track> iter1b = deviceFiles.deviceFiles.iterator();
			while(iter1b.hasNext())
			{
				if(next.equals(iter1b.next()))
				{
					iter1a.remove();
					break;
				}
			}
		}
		filesNotInPlaylists = (Set<DeviceLibrary.Track>)((HashSet)deviceFiles.deviceFiles).clone();
		Iterator<DeviceLibrary.Track> iter2a = filesNotInPlaylists.iterator();
		while(iter2a.hasNext())
		{
			DeviceLibrary.Track next = iter2a.next();
			Iterator<iTunesLibrary.Track> iter2b = library.getPlaylistFiles().iterator();
			while(iter2b.hasNext())
			{
				if(next.equals(iter2b.next()))
				{
					iter2a.remove();
					break;
				}
			}
		}
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
			protected JTable libraryTable;
			protected JTable playlistTable;
			protected JTable playlistFilesTable;
			protected JTable deviceFilesTable;
			protected JTable notOnDeviceTable;
			protected JTable notInPlaylistsTable;
			protected JButton collectPlaylists;
			protected JButton copyToDevice;
			protected JButton deleteFromDevice;
			protected JPanel playlistPanel;
			
			protected String[] playlistNames;
			
			protected JDialog progressDialog;
			protected JProgressBar filesProgress;
			protected JProgressBar transferProgress;
			protected JTextArea progressLog;
			protected JScrollPane progressLogScroller;
			
			protected JDialog deleteDialog;
			protected JProgressBar deleteProgress;
			
			protected SwingWorker transferTask;
			protected SwingWorker deleteTask;
			
			protected void create()
			{
				tabs = new JTabbedPane();
				{
					playlistPanel = new JPanel(new GuiLayout());
					{
						playlistTable = new JTable(library.getPlaylistDisplayData(), library.getPlaylistDisplayColumns()){
							public boolean isCellEditable(int row, int column) {return false;}
						};
						playlistScroller = new JScrollPane(playlistTable);
						
						collectPlaylists = new JButton("Build File List From Playlists");
						{
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
						}
						
						playlistFilesTable = new JTable(new DefaultTableModel(new String[]{"Local Files In Playlists"}, 0)){
							public boolean isCellEditable(int row, int column) {return false;}
						};
						playlistFilesTable.setFillsViewportHeight(true);
						playlistFilesTable.setAutoCreateRowSorter(true);
						playlistFilesTable.getRowSorter().toggleSortOrder(0);
						playlistFilesScroller = new JScrollPane(playlistFilesTable);
						
						deviceFilesTable = new JTable(deviceFiles.getDisplayData(), new String[]{"Device Files"}){
							public boolean isCellEditable(int row, int column) {return false;}
						};
						deviceFilesTable.setFillsViewportHeight(true);
						deviceFilesTable.setAutoCreateRowSorter(true);
						deviceFilesTable.getRowSorter().toggleSortOrder(0);
						deviceFilesScroller = new JScrollPane(deviceFilesTable);
						
						notOnDeviceTable = new JTable(new DefaultTableModel(new String[]{"Files To Copy To Device"}, 0)){
							public boolean isCellEditable(int row, int column) {return false;}
						};
						notOnDeviceTable.setFillsViewportHeight(true);
						notOnDeviceTable.setAutoCreateRowSorter(true);
						notOnDeviceTable.getRowSorter().toggleSortOrder(0);
						notOnDeviceScroller = new JScrollPane(notOnDeviceTable);
						
						copyToDevice = new JButton("copyToDevice");
						{
							copyToDevice.addActionListener(new ActionListener(){
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
											Iterator<iTunesLibrary.Track> iter = filesNotOnDevice.iterator();
											iTunesLibrary.Track file = null;
											int k = 0;
											while(!isCancelled() && iter.hasNext())
											{
												firePropertyChange("filenumber", k, ++k);
												firePropertyChange("file", (file!=null ? file.getRelativePath() : ""), (file = iter.next()).getRelativePath());
												firePropertyChange("copied", k>1 ? 100 : 0, 0);
												try
												{
													ProcessBuilder pb = new ProcessBuilder("adb", "push", file.getAbsolutePath(), deviceFiles.path + file.getRelativePath());
													System.out.println(pb.command());
													pb.redirectErrorStream(true);
													Process p = pb.start();
													InputStream in = p.getInputStream();
													BufferedReader stdInput = new BufferedReader(new InputStreamReader(in));
													String s;
													int progress = 0;
													while((s = stdInput.readLine()) != null)
													{
														try
														{
															firePropertyChange("copied", progress, progress = Integer.parseInt(s.substring(1, s.indexOf('%')).trim()));
														}
														catch(NumberFormatException x1)
														{
															firePropertyChange("log", "", s);
														}
														catch(StringIndexOutOfBoundsException x2)
														{
															firePropertyChange("log", "", s);
														}
													}
												}
												catch(IOException x)
												{
													System.out.println(x);
													firePropertyChange("log", "", "IOException caught trying to run command for file: "+ file);
												}
											}
											return null;
										}
										
										public void done()
										{
											System.out.println("File transfer thread finished."+ (isCancelled() ? " Cancelled prematurely." : ""));
											progressLog.append("Finished.");
											//progressDialog.setVisible(false);
											deviceFiles.buildFileList();
											setupTransferTables();
										}
									};
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
						}
						
						notInPlaylistsTable = new JTable(new DefaultTableModel(new String[]{"Files To Remove From Device"}, 0)){
							public boolean isCellEditable(int row, int column) {return false;}
						};
						notInPlaylistsTable.setFillsViewportHeight(true);
						notInPlaylistsTable.setAutoCreateRowSorter(true);
						notInPlaylistsTable.getRowSorter().toggleSortOrder(0);
						notInPlaylistsScroller = new JScrollPane(notInPlaylistsTable);
						
						deleteFromDevice = new JButton("deleteFromDevice");
						{
							deleteFromDevice.addActionListener(new ActionListener(){
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
													ProcessBuilder pb = new ProcessBuilder("adb", "shell", "rm", "$'"+ file.replace("'", "\\'") +"'");
													System.out.println(pb.command());
													pb.redirectErrorStream(true);
													Process p = pb.start();
													InputStream in = p.getInputStream();
													BufferedReader stdInput = new BufferedReader(new InputStreamReader(in));
													String s;
													int progress = 0;
													while((s = stdInput.readLine()) != null)
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
											deviceFiles.buildFileList();
											setupTransferTables();
										}
									};
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
						}
					}
					playlistPanel.add(deviceFilesScroller, new GuiLayout.GuiConstraint(playlistPanel,	"0",	"0",	"33%",	"0,bottom", deviceFilesScroller));
					playlistPanel.add(playlistScroller, new GuiLayout.GuiConstraint(playlistPanel,		"33%",	"0",	"33%",	"200", playlistScroller));
					playlistPanel.add(collectPlaylists, new GuiLayout.GuiConstraint(playlistPanel,		"33%",	"200",	"33%",	"30", collectPlaylists));
					playlistPanel.add(playlistFilesScroller, new GuiLayout.GuiConstraint(playlistPanel,	"33%",	"230",	"33%",	"0,bottom", playlistFilesScroller));
					playlistPanel.add(notOnDeviceScroller, new GuiLayout.GuiConstraint(playlistPanel,	"66%",	"0",	"34%",	"45%", notOnDeviceScroller));
					playlistPanel.add(copyToDevice, new GuiLayout.GuiConstraint(playlistPanel,			"66%",	"45%",	"34%",	"5%", copyToDevice));
					playlistPanel.add(notInPlaylistsScroller, new GuiLayout.GuiConstraint(playlistPanel,"66%",	"50%",	"34%",	"45%", notInPlaylistsScroller));
					playlistPanel.add(deleteFromDevice, new GuiLayout.GuiConstraint(playlistPanel,		"66%",	"95%",	"34%",	"5%", deleteFromDevice));
					
					libraryTable = new JTable(library.getDisplayData(), library.getDisplayColumns()){
						public boolean isCellEditable(int row, int column) {return false;}
					};
					libraryTable.setFillsViewportHeight(true);
					libraryTable.setAutoCreateRowSorter(true);
					libraryScroller = new JScrollPane(libraryTable);
				}
				tabs.addTab("Sync", playlistPanel);
				tabs.addTab("iTunes Library", libraryScroller);
				
				add(tabs, new GuiLayout.GuiConstraint(this,	"0,left",	"0,top",	"0,right",	"0,bottom", tabs));
				
				progressDialog = new JDialog(window, "Copying Files");
				progressDialog.setLayout(new GuiLayout());
				{
					filesProgress = new JProgressBar(0, 0);
					filesProgress.setStringPainted(true);
					transferProgress = new JProgressBar(0, 100);
					transferProgress.setStringPainted(true);
					progressLog = new JTextArea();
					progressLog.setEditable(false);
					progressLogScroller = new JScrollPane(progressLog);
				}
				progressDialog.add(filesProgress, new GuiLayout.GuiConstraint(progressDialog,	"0,center",	"5",	"300",	"40", filesProgress));
				progressDialog.add(transferProgress, new GuiLayout.GuiConstraint(progressDialog,"0,center",	"55",	"300",	"40", transferProgress));
				progressDialog.add(progressLogScroller, new GuiLayout.GuiConstraint(progressDialog,"0",	"105",	"600",	"200", progressLogScroller));
				progressDialog.pack();
				progressDialog.setLocationRelativeTo(window);
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
				
				deleteDialog = new JDialog(window, "Deleting Files");
				deleteDialog.setLayout(new GuiLayout());
				{
					deleteProgress = new JProgressBar(0, 0);
					deleteProgress.setStringPainted(true);
				}
				deleteDialog.add(deleteProgress, new GuiLayout.GuiConstraint(deleteDialog,	"0",	"0",	"300",	"40", deleteProgress));
				deleteDialog.pack();
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
			}
			
			public void setupTransferTables()
			{
				((DefaultTableModel)playlistFilesTable.getModel()).setRowCount(0);
				library.buildPlaylistFiles(playlistNames);
				Iterator<iTunesLibrary.Track> iter = library.getPlaylistFiles().iterator();
				while(iter.hasNext())
					((DefaultTableModel)playlistFilesTable.getModel()).addRow(new String[]{iter.next().getRelativePath()});
				
				((DefaultTableModel)notOnDeviceTable.getModel()).setRowCount(0);
				((DefaultTableModel)notInPlaylistsTable.getModel()).setRowCount(0);
				buildDifferences();
				Iterator<iTunesLibrary.Track> iter2 = filesNotOnDevice.iterator();
				while(iter2.hasNext())
					((DefaultTableModel)notOnDeviceTable.getModel()).addRow(new String[]{iter2.next().getRelativePath()});
				Iterator<DeviceLibrary.Track> iter3 = filesNotInPlaylists.iterator();
				while(iter3.hasNext())
					((DefaultTableModel)notInPlaylistsTable.getModel()).addRow(new String[]{iter3.next().getRelativePath()});
			}
		});
	}
	
	public void setCanvas(String canvas)
	{
		window.setCanvas(guiMap.get(canvas));
	}
	
	public void exit()
	{
		System.exit(0);
	}
}