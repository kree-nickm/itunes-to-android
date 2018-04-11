import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.beans.*;
import java.util.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
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
	protected iTunesLibrary library;
	protected DeviceLibrary deviceFiles;
	protected Set<iTunesLibrary.Track> filesNotOnDevice;
	protected Set<DeviceLibrary.Track> filesNotInPlaylists;
	
	public iTunesToAndroid()
	{
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
		if(library == null || deviceFiles == null)
		{
			filesNotOnDevice = new HashSet<iTunesLibrary.Track>();
			filesNotInPlaylists = new HashSet<DeviceLibrary.Track>();
			return;
		}
		
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
			protected JButton itunesBrowseButton;
			protected JButton itunesLoadButton;
			protected JPanel playlistPanel;
			protected JPanel setupPanel;
			protected JTextField itunesInput;
			protected JFileChooser itunesChooser;
			protected JLabel deviceScanLabel;
			
			protected String[] playlistNames;
			
			protected JDialog progressDialog;
			protected JProgressBar filesProgress;
			protected JProgressBar transferProgress;
			protected JTextArea progressLog;
			protected JScrollPane progressLogScroller;
			
			protected JDialog deleteDialog;
			protected JProgressBar deleteProgress;
			
			protected SwingWorker scanForDeviceTask;
			protected SwingWorker transferTask;
			protected SwingWorker deleteTask;
			
			protected void create()
			{
				tabs = new JTabbedPane();
				{
					setupPanel = new JPanel(new GuiLayout());
					{
						itunesInput = new JTextField();
						itunesChooser = new JFileChooser(new File(System.getProperty("user.home") +"\\Music\\iTunes\\"));
						itunesChooser.setFileFilter(new FileNameExtensionFilter("XML File", "xml"));
						itunesBrowseButton = new JButton("Browse");
						{
							itunesBrowseButton.addActionListener(new ActionListener(){
								public void actionPerformed(ActionEvent e)
								{
									int returnVal = itunesChooser.showOpenDialog(window);
									if(returnVal == JFileChooser.APPROVE_OPTION)
									{
										itunesInput.setText(itunesChooser.getSelectedFile().getAbsolutePath());
									}
								}
							});
						}
						itunesLoadButton = new JButton("Load");
						{
							itunesLoadButton.addActionListener(new ActionListener(){
								public void actionPerformed(ActionEvent e)
								{
									try
									{
										library = new iTunesLibrary(itunesInput.getText());
										setupLibraryTab();
										if(deviceFiles != null)
											setupSyncTab();
										showMessagePopup("Library Loaded", "The iTunes library has been successfully loaded.");
									}
									catch(iTunesLibrary.InvalidLibraryException x)
									{
										showMessagePopup("Error Loading Library", x.getMessage());
										System.out.println(x);
									}
								}
							});
						}
						deviceScanLabel = new JLabel("");
					}
					setupPanel.add(itunesInput, new GuiLayout.GuiConstraint(setupPanel,			"0",	"0",	"400",	"25", itunesInput));
					setupPanel.add(itunesBrowseButton, new GuiLayout.GuiConstraint(setupPanel,	"400",	"0",	"100",	"25", itunesBrowseButton));
					setupPanel.add(itunesLoadButton, new GuiLayout.GuiConstraint(setupPanel,	"500",	"0",	"100",	"25", itunesLoadButton));
					setupPanel.add(deviceScanLabel, new GuiLayout.GuiConstraint(setupPanel,		"20",	"40",	"100%",	"25", deviceScanLabel));
					
					playlistPanel = new JPanel(new GuiLayout());
					{
						playlistTable = new JTable(new DefaultTableModel()){
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
						
						deviceFilesTable = new JTable(new DefaultTableModel(new String[]{"Device Files"}, 0)){
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
											try
											{
												deviceFiles.buildFileList();
											}
											catch(DeviceLibrary.DeviceMissingException x1)
											{
												showMessagePopup("Error Accessing Device", x1.getMessage());
												System.out.println(x1);
											}
											catch(DeviceLibrary.ADBException x2)
											{
												showMessagePopup("Error Running ADB", x2.getMessage());
												System.out.println(x2);
											}
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
											try
											{
												deviceFiles.buildFileList();
											}
											catch(DeviceLibrary.DeviceMissingException x1)
											{
												showMessagePopup("Error Accessing Device", x1.getMessage());
												System.out.println(x1);
											}
											catch(DeviceLibrary.ADBException x2)
											{
												showMessagePopup("Error Running ADB", x2.getMessage());
												System.out.println(x2);
											}
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
				}
				tabs.addTab("Setup", setupPanel);
				tabs.addTab("Sync", playlistPanel);
				tabs.addChangeListener(new ChangeListener(){
					public void stateChanged(ChangeEvent e)
					{
						if(tabs.getSelectedComponent() == setupPanel)
							startPeriodicDeviceScan();
					}
				});
				startPeriodicDeviceScan();
				
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
				
				setupTransferTables();
			}
			
			public void showMessagePopup(String title, String text)
			{
				JOptionPane.showMessageDialog(window, text, title, JOptionPane.WARNING_MESSAGE);
			}
			
			public void startPeriodicDeviceScan()
			{
				if(scanForDeviceTask != null && !scanForDeviceTask.isDone())
					return;
				scanForDeviceTask = new SwingWorker<Void,Void>() {
					public Void doInBackground()
					{
						short prevStatus = -2;
						String device;
						String prevStr = "";
						while(tabs.getSelectedComponent() == setupPanel)
						{
							try
							{
								if((device = DeviceLibrary.checkForDevice()) != null)
								{
									try
									{
										if(prevStatus != 1)
										{
											deviceFiles = new DeviceLibrary("/sdcard/Music/"); // need to get path based on user input
											firePropertyChange("scanresult", prevStr, prevStr = "Device found: "+ device);
											setupDeviceTab();
											if(library != null)
												setupSyncTab();
										}
										prevStatus = 1;
									}
									catch(DeviceLibrary.DeviceMissingException x1)
									{
										if(prevStatus != 0)
											firePropertyChange("scanresult", prevStr, prevStr = x1.getMessage());
										prevStatus = 0;
									}
								}
								else
								{
									if(prevStatus != 0)
										firePropertyChange("scanresult", prevStr, prevStr = "No Android device found.");
									prevStatus = 0;
								}
							}
							catch(DeviceLibrary.ADBException x2)
							{
								if(prevStatus != -1)
									firePropertyChange("scanresult", prevStr, prevStr = x2.getMessage());
								prevStatus = -1;
							}
							try
							{
								Thread.sleep(1000);
							}
							catch(InterruptedException x)
							{
								System.out.println(x);
							}
						}
						return null;
					}
					
					public void done()
					{
						System.out.println("No longer scanning for Android devices.");
					}
				};
				scanForDeviceTask.addPropertyChangeListener(new PropertyChangeListener(){
					public void propertyChange(PropertyChangeEvent e)
					{
						if("scanresult".equals(e.getPropertyName()))
						{
							System.out.println(e.getNewValue());
							deviceScanLabel.setText(e.getNewValue().toString());
						}
					}
				});
				scanForDeviceTask.execute();
				System.out.println("Scanning for Android devices.");
			}
			
			public void setupSyncTab()
			{
			}
			
			public void setupDeviceTab()
			{
				((DefaultTableModel)deviceFilesTable.getModel()).setDataVector(deviceFiles.getDisplayData(), new String[]{"Device Files"});
			}
			
			public void setupLibraryTab()
			{
				if(libraryTable == null)
					libraryTable = new JTable(new DefaultTableModel(library.getDisplayData(), library.getDisplayColumns())){
						public boolean isCellEditable(int row, int column) {return false;}
					};
				else
					((DefaultTableModel)libraryTable.getModel()).setDataVector(library.getDisplayData(), library.getDisplayColumns());
				libraryTable.setFillsViewportHeight(true);
				libraryTable.setAutoCreateRowSorter(true);
				if(libraryScroller == null)
					libraryScroller = new JScrollPane(libraryTable);
				tabs.insertTab("iTunes Library", null, libraryScroller, "Full list of your iTunes library so that you can verify that it has been properly loaded.", tabs.getTabCount());
				
				((DefaultTableModel)playlistTable.getModel()).setDataVector(library.getPlaylistDisplayData(), library.getPlaylistDisplayColumns());
			}
			
			public void setupTransferTables()
			{
				((DefaultTableModel)playlistFilesTable.getModel()).setRowCount(0);
				if(library != null)
				{
					library.buildPlaylistFiles(playlistNames);
					Iterator<iTunesLibrary.Track> iter = library.getPlaylistFiles().iterator();
					while(iter.hasNext())
						((DefaultTableModel)playlistFilesTable.getModel()).addRow(new String[]{iter.next().getRelativePath()});
				}
				
				((DefaultTableModel)notOnDeviceTable.getModel()).setRowCount(0);
				((DefaultTableModel)notInPlaylistsTable.getModel()).setRowCount(0);
				buildDifferences();
				Iterator<iTunesLibrary.Track> iter2 = filesNotOnDevice.iterator();
				while(iter2.hasNext())
					((DefaultTableModel)notOnDeviceTable.getModel()).addRow(new String[]{iter2.next().getRelativePath()});
				Iterator<DeviceLibrary.Track> iter3 = filesNotInPlaylists.iterator();
				while(iter3.hasNext())
					((DefaultTableModel)notInPlaylistsTable.getModel()).addRow(new String[]{iter3.next().getRelativePath()});
				
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
		System.exit(0);
	}
}