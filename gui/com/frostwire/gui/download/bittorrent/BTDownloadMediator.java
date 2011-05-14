package com.frostwire.gui.download.bittorrent;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.frostwire.CoreFrostWireUtils;
import com.frostwire.GuiFrostWireUtils;
import com.limegroup.bittorrent.gui.TorrentDownloadFactory;
import com.limegroup.bittorrent.gui.TorrentFileFetcher;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.DialogOption;
import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.FileDetailsProvider;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.PaddedPanel;
import com.limegroup.gnutella.gui.actions.BitziLookupAction;
import com.limegroup.gnutella.gui.actions.CopyMagnetLinkToClipboardAction;
import com.limegroup.gnutella.gui.actions.LimeAction;
import com.limegroup.gnutella.gui.actions.SearchAction;
import com.limegroup.gnutella.gui.dnd.FileTransfer;
import com.limegroup.gnutella.gui.dock.DockIcon;
import com.limegroup.gnutella.gui.dock.DockIconFactoryImpl;
import com.limegroup.gnutella.gui.options.OptionsMediator;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.ColumnPreferenceHandler;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.SimpleColumnListener;
import com.limegroup.gnutella.gui.tables.TableSettings;
import com.limegroup.gnutella.gui.themes.SkinMenu;
import com.limegroup.gnutella.gui.themes.SkinMenuItem;
import com.limegroup.gnutella.gui.themes.SkinPopupMenu;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeSettings;
import com.limegroup.gnutella.gui.util.CoreExceptionHandler;
import com.limegroup.gnutella.gui.util.GUILauncher;
import com.limegroup.gnutella.gui.util.GUILauncher.LaunchableProvider;
import com.limegroup.gnutella.settings.QuestionsHandler;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * This class acts as a mediator between all of the components of the
 * download window.  It also constructs all of the download window
 * components.
 */
public final class BTDownloadMediator extends AbstractTableMediator<BTDownloadModel, BTDownloadDataLine, BTDownloader>
	implements FileDetailsProvider {

	private static final Log LOG = LogFactory.getLog(BTDownloadMediator.class);
	
	/**
	 * Count the number of resume clicks
	 */
	@InspectablePrimitive("resume button clicks")
    private static volatile int resumeClicks;
	
    /**
     * Variable for the total number of downloads that have been added in this
     * session.
     */
    private static int _totalDownloads = 0;

    /**
     * instance, for singleton access
     */
    private static BTDownloadMediator INSTANCE;


    public static BTDownloadMediator instance() {
        if (INSTANCE == null) {
            INSTANCE = new BTDownloadMediator();
        }
        
        return INSTANCE;
    }

    /**
     * Variables so only one ActionListener needs to be created for both
     * the buttons & popup menu.
     */
	private Action removeAction;
    private Action clearAction;
    private Action browseAction;
    private Action launchAction;
    private Action resumeAction;
    private Action pauseAction;
	private Action magnetAction;
	private Action bitziAction;
	private Action exploreAction; 

    /** The actual download buttons instance.
     */
    private BTDownloadButtons _downloadButtons;
    
    private final DockIcon dockIcon;
    
    /**
     * Overriden to have different default values for tooltips.
     */
    protected void buildSettings() {
        SETTINGS = new TableSettings(ID) {
            public boolean getDefaultTooltips() {
                return false;
            }
        };
    }

    /**
     * Sets up drag & drop for the table.
     */
    protected void setupDragAndDrop() {
    	TABLE.setDragEnabled(true);
    	TABLE.setTransferHandler(new BTDownloadTransferHandler());
    }

    /**
     * Build some extra listeners
     */
    protected void buildListeners() {
        super.buildListeners();

		removeAction = new RemoveAction();
		clearAction = new ClearAction();
		browseAction = new BrowseAction();
		launchAction = new LaunchAction();
		resumeAction = new ResumeAction();
		pauseAction = new PauseAction();
		magnetAction = new CopyMagnetLinkToClipboardAction(this);
		exploreAction = new ExploreAction(); 
		bitziAction = new BitziLookupAction(this);       
    }

	/**
	 * Returns the most prominent actions that operate on the download table.
	 * @return
	 */
	public Action[] getActions() {
		Action[] actions;
		if(OSUtils.isWindows()||OSUtils.isMacOSX())
            actions = new Action[] { 
                resumeAction, pauseAction, launchAction,
                exploreAction,clearAction,removeAction};
		else 
			actions = new Action[] { 
		        resumeAction, pauseAction, launchAction,clearAction, removeAction};
		
		return actions;
	}
	
    /**
     * Set up the necessary constants.
     */
    protected void setupConstants() {
        MAIN_PANEL =
            new PaddedPanel(I18n.tr("Downloads"));
        DATA_MODEL = new BTDownloadModel();
        TABLE = new LimeJTable(DATA_MODEL);
        _downloadButtons = new BTDownloadButtons(this);
        BUTTON_ROW = _downloadButtons.getComponent();
    }
    
    /**
     * Sets up the table headers.
     */
    protected void setupTableHeaders() {
        super.setupTableHeaders();
        
    }

    /**
     * Update the splash screen.
     */
    protected void updateSplashScreen() {
        GUIMediator.setSplashScreenString(
            I18n.tr("Loading Download Window..."));
    }

    /**
     * Constructs all of the elements of the download window, including
     * the table, the buttons, etc.
     */
    private BTDownloadMediator() {
        super("DOWNLOAD_TABLE");
        GUIMediator.addRefreshListener(this);
        ThemeMediator.addThemeObserver(this);
        
        this.dockIcon = new DockIconFactoryImpl().createDockIcon();
    }

    /**
     * Override the default refreshing so that we can
     * set the clear button appropriately.
     */
    public void doRefresh() {
        boolean inactivePresent =
            ((Boolean)DATA_MODEL.refresh()).booleanValue();
        
		clearAction.setEnabled(inactivePresent);
      
		int[] selRows = TABLE.getSelectedRows();
        
		if (selRows.length > 0) {
//            DownloadDataLine dataLine = DATA_MODEL.get(selRows[0]);
//            
//			if (dataLine.getState() == DownloadStatus.WAITING_FOR_USER) {
//				resumeAction.putValue(Action.NAME,
//						I18n.tr("Find More Sources for Download"));
//				resumeAction.putValue(LimeAction.SHORT_NAME,
//						I18n.tr("Find Sources"));
//				resumeAction.putValue(Action.SHORT_DESCRIPTION,
//						I18n.tr("Try to Find Additional Sources for Downloads"));
//			}
//            else {
//				resumeAction.putValue(Action.NAME,
//						I18n.tr("Resume Download"));
//				resumeAction.putValue(LimeAction.SHORT_NAME, 
//						 I18n.tr("Resume"));
//				resumeAction.putValue(Action.SHORT_DESCRIPTION,
//						 I18n.tr("Reattempt Selected Downloads"));
//            }
//			
//            Downloader dl = dataLine.getDownloader();
//            boolean inactive = dataLine.isDownloaderInactive();
//            boolean resumable = dl.isResumable();
//            boolean pausable = dl.isPausable(); 
//            boolean completed = dl.isCompleted();
//            
//			resumeAction.setEnabled(resumable);
//			pauseAction.setEnabled(pausable);
//			priorityUpAction.setEnabled(inactive && pausable);
//			priorityDownAction.setEnabled(inactive && pausable);
//			exploreAction.setEnabled(completed || inactive);
		}
		
        dockIcon.draw(getCompleteDownloads());
	}

    /**
     * Returns the number of completed Downloads.
     * 
     * @return The number of completed Downloads
     */
    public int getCompleteDownloads() {
        int complete = 0;
//        for (int row = 0; row < DATA_MODEL.getRowCount(); row++) {
//            BTDownloadDataLine dataLine = DATA_MODEL.get(row);
//            if (dataLine.getState() == DownloadStatus.COMPLETE) {
//                complete++;
//            }
//        }
        return complete;
    }
    
    /**
     * Returns the total number of Downloads that have occurred in this session.
     *
     * @return the total number of Downloads that have occurred in this session
     */
    public int getTotalDownloads() {
        return _totalDownloads;
    }

    /**
     * Returns the total number of current Downloads.
     *
     * @return the total number of current Downloads
     */
    public int getCurrentDownloads() {
        return DATA_MODEL.getCurrentDownloads();
    }

    /**
     * Returns the total number of active Downloads.
     * This includes anything that is still viewable in the Downloads view.
     *
     * @return the total number of active Downloads
     */
    public int getActiveDownloads() {
        return DATA_MODEL.getRowCount();
    }
    
    /**
     * Returns the set of filenames of all downloads
     * This includes anything that is still viewable in the Downloads view.
     *
     * @return Set of filenames (String) of all downloads
     */
    
//    public Set<String> getFileNames() {
//    	Set<String> names = new HashSet<String>();
//    	for(int c = 0;c < DATA_MODEL.getRowCount(); c++) {
//    	    names.add(DATA_MODEL.get(c).getFileName());
//        }
//    	return names;
//    }
    
    /**
     * Returns the aggregate amount of bandwidth being consumed by active downloads.
     *  
     * @return the total amount of bandwidth being consumed by active downloads.
     */
    public double getActiveDownloadsBandwidth() {
        return DATA_MODEL.getActiveDownloadsBandwidth();
    }

    /**
     * Overrides the default add.
     *
     * Adds a new Downloads to the list of Downloads, obtaining the necessary
     * information from the supplied <tt>Downloader</tt>.
     *
     * If the download is not already in the list, then it is added.
     *  <p>
     */
    public void add(BTDownloader downloader) {
    	
//    	//don't show system update on upload tab
//        if (downloader != null && downloader.getFile() != null && downloader.getFile().getName().startsWith("hostiles.txt.")) {
//            //System.out.println("UploadMediator.add() - Skipping "+uploader.getFileName()+" - no need to show");
//            return;
//        }
//        
//        if ( !DATA_MODEL.contains(downloader) ) {
//            _totalDownloads++;
//            super.add(downloader);
//        }
    }

    /**
     * Overrides the default remove.
     *
     * Takes action upon downloaded theme files, asking if the user wants to
     * apply the theme.
     *
     * Removes a download from the list if the user has configured their system
     * to automatically clear completed download and if the download is
     * complete.
     *
     * @param downloader the <tt>Downloader</tt> to remove from the list if it is
     *  complete.
     */
    public void remove(BTDownloader dloader) {
//        DownloadStatus state = dloader.getState();
//        
//        if (state == DownloadStatus.COMPLETE 
//        		&& isThemeFile(dloader.getSaveFile().getName())) {
//        	File themeFile = dloader.getDownloadFragment();
//        	themeFile = copyToThemeDir(themeFile);
//        	// don't allow changing of theme while options are visible,
//        	// but notify the user how to change the theme
//        	if (OptionsMediator.instance().isOptionsVisible()) {
//        		GUIMediator.showMessage(I18n.tr("You have downloaded a skin titled \"{0}\", you can activate the new skin by clicking \"{1}\" in the \"{2}\"->\"{3}\" menu and then selecting it from the list of available skins.",
//        		        ThemeSettings.formatName(dloader.getSaveFile().getName()),
//        				I18n.tr("&Refresh Skins"),
//        				I18n.tr("&View"),
//        				I18n.tr("&Apply Skins")));
//        	}
//        	else {
//        	    DialogOption response = GUIMediator.showYesNoMessage(
//        				I18n.tr("You have downloaded a new skin titled {0}. Would you like to use this new skin?",
//        				        ThemeSettings.formatName(dloader.getSaveFile().getName())),
//        				QuestionsHandler.THEME_DOWNLOADED, DialogOption.YES
//        				);
//        		if( response == DialogOption.YES ) {
//        			//ThemeMediator.changeTheme(themeFile);
//        		}
//        	}
//        }
//        
//        if (state == DownloadStatus.COMPLETE &&
//        		BittorrentSettings.TORRENT_AUTO_START.getValue() &&
//        		isTorrentFile(dloader.getSaveFile().getName())) 
//        	GUIMediator.instance().openTorrent(dloader.getSaveFile());
//        
//        if(SharingSettings.CLEAR_DOWNLOAD.getValue()
//           && ( state == DownloadStatus.COMPLETE ||
//                state == DownloadStatus.ABORTED ) ) {
//        	super.remove(dloader);
//        } else {
//            DownloadDataLine ddl = DATA_MODEL.get(dloader);
//            if (ddl != null) ddl.setEndTime(System.currentTimeMillis());
//        }
    }

    public void openTorrent(File file) {
//    	try {
//            TorrentDownloadFactory factory = new TorrentDownloadFactory(file);
//            DownloaderUtils.createDownloader(factory);
//  		
//            if(SharingSettings.SHARE_TORRENT_META_FILES.getValue()) {
//            	GuiFrostWireUtils.shareTorrent(file);
//            	
//            	// begin of FTA validations (FTA: Added support to handle files one by one)
//            	com.limegroup.bittorrent.BTMetaInfo torrinfo = factory.getBTMetaInfo();
//
//            	if (torrinfo == null) {
//            		//GUIMediator.showMessage("Canceled by user"); //FTA: debug
//            		return;
//            	}
//            	final File tFile = GuiCoreMediator.getTorrentManager()
//            	.getSharedTorrentMetaDataFile(torrinfo);
//            	//System.out.println("DownloadMediator() - getBTMetaInfo SUCCESS!");
//            	// end of FTA validations
//
//            	// Old code was:
//            	//final File tFile = GuiCoreMediator.getTorrentManager()
//            	//                .getSharedTorrentMetaDataFile(factory.getBTMetaInfo());
//
//            	File backup = null;
//            	if(tFile.exists()) {
//            		//could be same file if we are re-launching 
//            		//an existing torrent from library
//            		if(tFile.equals(file)) {
//            			return;
//            		}
//
//            		//don't get this one... aren't we supposed to share the file?
//            		GuiCoreMediator.getFileManager().stopSharingFile(tFile);
//
//            		backup = new File(tFile.getParent(), tFile.getName().concat(".bak"));
//            		FileUtils.forceRename(tFile, backup);
//            	}
//            	if(!FileUtils.copy(file, tFile) && (backup != null)) {
//            		//try restoring backup
//            		if(FileUtils.forceRename(backup, tFile)) {
//            			GuiCoreMediator.getFileManager().addFileIfShared(tFile);
//            		}
//            	} 
//            	//com.limegroup.gnutella.gui.GUIMediator.showMessage("The torrent has been read!"); //FTA: debug
//            }
//    	} catch (IOException ioe) {
//    		ioe.printStackTrace();
//    		if (!ioe.toString().contains("No files selected by user")) {
//    			// could not read torrent file or bad torrent file.
//    			GUIMediator.showError(I18n.tr("FrostWire was unable to load the torrent file \"{0}\", - it may be malformed or FrostWire does not have permission to access this file.", 
//    					file.getName()),
//    					QuestionsHandler.TORRENT_OPEN_FAILURE);
//    			//System.out.println("***Error happened from Download Mediator: " +  ioe);
//    			//GUIMediator.showMessage("Error was: " + ioe); //FTA: debug
//    		}
//    	}
    }

    /**
     * Gubatron to FTA: You'll need to apply your updates on this method as well.
     * @param uri
     */
    public void openTorrentURI(URI uri) {
//    	TorrentFileFetcher fetcher = new TorrentFileFetcher(uri, GuiCoreMediator.getDownloadManager());
//    	add(fetcher);
//    	fetcher.fetch();
    }
    
    private File copyToThemeDir(File themeFile) {
        File themeDir = ThemeSettings.THEME_DIR_FILE;
        File realLoc = new File(themeDir, themeFile.getName());
        // if they're the same, just use it.
        if( realLoc.equals(themeFile) )
            return themeFile;

        // otherwise, if the file already exists in the theme dir, remove it.
        realLoc.delete();
        
        // copy from shared to theme dir.
        FileUtils.copy(themeFile, realLoc);
        return realLoc;
    }
    
    private boolean isThemeFile(String name) {
        return name.toLowerCase().endsWith(ThemeSettings.EXTENSION);
    }
    
    private boolean isTorrentFile(String name) {
    	return name.toLowerCase().endsWith(".torrent");
    }
    

    /**
     * Launches the selected files in the <tt>Launcher</tt> or in the built-in
     * media player.
     */
    void launchSelectedDownloads() {
//        int[] sel = TABLE.getSelectedRows();
//        if (sel.length == 0) {
//        	return;
//        }
//        LaunchableProvider[] providers = new LaunchableProvider[sel.length];
//        for (int i = 0; i < sel.length; i++) {
//        	providers[i] = new DownloaderProvider(DATA_MODEL.get(sel[i]).getDownloader());
//        }
//        GUILauncher.launch(providers);
    }
    
    /**
     * Pauses all selected downloads.
     */
    void pauseSelectedDownloads() {
//        int[] sel = TABLE.getSelectedRows();
//        for(int i = 0; i < sel.length; i++)
//            DATA_MODEL.get(sel[i]).getInitializeObject().pause();
    }
    
    /**  
     * Launches explorer
     */ 
    void launchExplorer() { 
//        int[] sel = TABLE.getSelectedRows();
//        Downloader dl = DATA_MODEL.get(sel[sel.length-1]).getInitializeObject(); 
//        File toExplore = dl.getFile(); 
//        
//        if (toExplore == null) {
//            return;
//        }
//        
//        GUIMediator.launchExplorer(toExplore);
    } 
    
    FileTransfer[] getSelectedFileTransfers() {
    	int[] sel = TABLE.getSelectedRows();
    	ArrayList<FileTransfer> transfers = new ArrayList<FileTransfer>(sel.length);
//    	for (int i = 0; i < sel.length; i++) {
//    		DownloadDataLine line = DATA_MODEL.get(sel[i]);
//    		Downloader downloader = line.getDownloader();
//    		// ignore if save file of complete downloader has already been moved
//    		if (downloader.getState() == DownloadStatus.COMPLETE
//    				&& !downloader.getSaveFile().exists()) {
//    			continue;
//    		}
//        	if (downloader.isLaunchable()) {
//        		transfers.add(line.getFileTransfer());
//        	}
//    	}
    	return transfers.toArray(new FileTransfer[transfers.size()]);
    }

    /**
     * Forces the selected downloads in the download window to resume.
     */
    void resumeSelectedDownloads() {
//        int[] sel = TABLE.getSelectedRows();
//        for(int i = 0; i < sel.length; i++) {
//            DownloadDataLine dd = DATA_MODEL.get(sel[i]);
//            Downloader downloader = dd.getDownloader();
//                if(!dd.isCleaned())
//                    downloader.resume();
//        }
//        
//        resumeClicks++;
    }

	

    /**
     * Opens up a browse session with the selected hosts in the download
     * window.
     */
    void browseSelectedDownloads() {
//        int[] sel = TABLE.getSelectedRows();
//        for(int i = 0; i < sel.length; i++) {
//            DownloadDataLine dd = DATA_MODEL.get(sel[i]);
//            Downloader downloader = dd.getInitializeObject();
//            RemoteFileDesc end = downloader.getBrowseEnabledHost();
//            if (end != null)
//                SearchMediator.doBrowseHost(end);
//        }
    }

    /**
     * Handles a double-click event in the table.
     */
    public void handleActionKey() {
    	if (launchAction.isEnabled())
    		launchSelectedDownloads();
    } 

    /**
     * Clears the downloads in the download window that have completed.
     */
    void clearCompletedDownloads() {
        DATA_MODEL.clearCompleted();
        clearSelection();
        clearAction.setEnabled(false);
        
        dockIcon.draw(0);
    }

	/**
	 * Returns the selected {@link FileDetails}.
	 */
	public FileDetails[] getFileDetails() {
        int[] sel = TABLE.getSelectedRows();
		FileManager fmanager = GuiCoreMediator.getFileManager();
		List<FileDetails> list = new ArrayList<FileDetails>(sel.length);
//        for(int i = 0; i < sel.length; i++) {
//            URN urn = DATA_MODEL.get(sel[i]).getDownloader().getSha1Urn();
//			if (urn != null) {
//				FileDesc fd = fmanager.getFileDescForUrn(urn);
//				if (fd != null) {
//				    // DPINJ:  Use passed in LocalFileDetailsFactory
//					list.add(GuiCoreMediator.getLocalFileDetailsFactory().create(fd));
//				}
//				else if (LOG.isDebugEnabled()) {
//					LOG.debug("not filedesc for urn " + urn);
//				}
//			}
//			else if (LOG.isDebugEnabled()) {
//				LOG.debug("no urn");
//			}
//		}
		return list.toArray(new FileDetails[0]);
	}

    // inherit doc comment
    protected JPopupMenu createPopupMenu() {
		
		JPopupMenu menu = new SkinPopupMenu();
		menu.add(new SkinMenuItem(removeAction));
		menu.add(new SkinMenuItem(resumeAction));
		menu.add(new SkinMenuItem(pauseAction));
		menu.add(new SkinMenuItem(launchAction));
		if(OSUtils.isWindows()||OSUtils.isMacOSX())
			menu.add(new SkinMenuItem(exploreAction)); 
		menu.addSeparator();
		menu.add(new SkinMenuItem(clearAction));
		menu.addSeparator();
        //menu.add(createSearchMenu());
		menu.add(new SkinMenuItem(browseAction));
//		menu.addSeparator();
//		menu.add(createAdvancedSubMenu());
				
		return menu;
    }
	
    
    /**
     * Handles the selection of the specified row in the download window,
     * enabling or disabling buttons and chat menu items depending on
     * the values in the row.
     *
     * @param row the selected row
     */
    public void handleSelection(int row) {

        BTDownloadDataLine dataLine = DATA_MODEL.get(row);

//        chatAction.setEnabled(dataLine.getChatEnabled());
//        browseAction.setEnabled(dataLine.getBrowseEnabled());
//        
//		boolean inactive = dataLine.isDownloaderInactive();
//        boolean pausable = dataLine.getDownloader().isPausable();
//
//		
//		if (dataLine.getState() == DownloadStatus.WAITING_FOR_USER) {
//			resumeAction.putValue(Action.NAME,
//								  I18n.tr("Find More Sources for Download"));
//			resumeAction.putValue(LimeAction.SHORT_NAME,
//								  I18n.tr("Find Sources"));
//			resumeAction.putValue(Action.SHORT_DESCRIPTION,
//								  I18n.tr("Try to Find Additional Sources for Downloads"));
//		} else {
//			resumeAction.putValue(Action.NAME,
//								  I18n.tr("Resume Download"));
//			resumeAction.putValue(LimeAction.SHORT_NAME, 
//								  I18n.tr("Resume"));
//			resumeAction.putValue(Action.SHORT_DESCRIPTION,
//								  I18n.tr("Reattempt Selected Downloads"));
//		}
//		
//		if (dataLine.isCompleted()) {
//			removeAction.putValue(Action.NAME,
//					  I18n.tr("Clear Download"));
//			removeAction.putValue(LimeAction.SHORT_NAME,
//					  I18n.tr("Clear"));
//			removeAction.putValue(Action.SHORT_DESCRIPTION,
//					  I18n.tr("Clear Selected Downloads"));
//			launchAction.putValue(Action.NAME,
//					  I18n.tr("Launch Download"));
//			launchAction.putValue(LimeAction.SHORT_NAME,
//					  I18n.tr("Launch"));
//			launchAction.putValue(Action.SHORT_DESCRIPTION,
//					  I18n.tr("Launch Selected Downloads"));
//			exploreAction.setEnabled(TABLE.getSelectedRowCount() == 1); 
//		} else {
//			removeAction.putValue(Action.NAME, I18n.tr
//					("Cancel Download"));
//			removeAction.putValue(LimeAction.SHORT_NAME,
//					 I18n.tr("Cancel"));
//			removeAction.putValue(Action.SHORT_DESCRIPTION,
//					 I18n.tr("Cancel Selected Downloads"));
//			launchAction.putValue(Action.NAME,
//					  I18n.tr("Preview Download"));
//			launchAction.putValue(LimeAction.SHORT_NAME,
//					  I18n.tr("Preview"));
//			launchAction.putValue(Action.SHORT_DESCRIPTION,
//					  I18n.tr("Preview Selected Downloads"));
//			exploreAction.setEnabled(false); 
//		}
//		
//		removeAction.setEnabled(true);
//        resumeAction.setEnabled(inactive);
//		pauseAction.setEnabled(pausable);
//        priorityDownAction.setEnabled(inactive && pausable);
//        priorityUpAction.setEnabled(inactive && pausable);
//		
//		Downloader dl = dataLine.getInitializeObject();
//		editLocationAction.setEnabled(TABLE.getSelectedRowCount() == 1 
//									  && dl.isRelocatable());
//		
//		magnetAction.setEnabled(dl.getSha1Urn() != null);
//		bitziAction.setEnabled(dl.getSha1Urn() != null);
//		launchAction.setEnabled(dl.isLaunchable());
    }

    /**
     * Handles the deselection of all rows in the download table,
     * disabling all necessary buttons and menu items.
     */
    public void handleNoSelection() {
        removeAction.setEnabled(false);
		resumeAction.setEnabled(false);
		launchAction.setEnabled(false);
		pauseAction.setEnabled(false);
		browseAction.setEnabled(false);
		magnetAction.setEnabled(false);
		bitziAction.setEnabled(false);
		exploreAction.setEnabled(false); 
    }

    private abstract class RefreshingAction extends AbstractAction {
    	/**
         * 
         */
        private static final long serialVersionUID = -937688457597255711L;

        public final void actionPerformed(ActionEvent e) {
    		performAction(e);
    		doRefresh();
    	}
    	
    	protected abstract void performAction(ActionEvent e);
    }
    
	private class RemoveAction extends RefreshingAction {
		
		/**
         * 
         */
        private static final long serialVersionUID = -1742554445891016991L;

        public RemoveAction() {
			putValue(Action.NAME, I18n.tr
					("Cancel Download"));
			putValue(LimeAction.SHORT_NAME,
					 I18n.tr("Cancel"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Cancel Selected Downloads"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_KILL");
		}
		
		public void performAction(ActionEvent e) {
			removeSelection();
		}
	}
	
	private class ClearAction extends RefreshingAction {
		
		/**
         * 
         */
        private static final long serialVersionUID = -5015913950467760897L;

        public ClearAction() {
			putValue(Action.NAME,
					 I18n.tr("Clear All Inactive Downloads"));
			putValue(LimeAction.SHORT_NAME,
					 I18n.tr("Clear Inactive"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Remove Inactive Downloads"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_CLEAR");
		}
		
	    public void performAction(ActionEvent e) {
            clearCompletedDownloads();
        }
	}

	private class BrowseAction extends RefreshingAction {

		/**
         * 
         */
        private static final long serialVersionUID = 874818705792848110L;

        public BrowseAction() {
    	    putValue(Action.NAME,
					I18n.tr("Browse Host"));
		}
		
		public void performAction(ActionEvent e) {
			browseSelectedDownloads();
		}
	}

	private class LaunchAction extends RefreshingAction {
		
		/**
         * 
         */
        private static final long serialVersionUID = -567893064454697074L;

        public LaunchAction() {
			putValue(Action.NAME,
					 I18n.tr("Preview Download"));
			putValue(LimeAction.SHORT_NAME,
					 I18n.tr("Preview"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Preview Selected Downloads"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_LAUNCH");
		}

		public void performAction(ActionEvent e) {
			launchSelectedDownloads();
		}
	}

	
	private class ResumeAction extends RefreshingAction {

		/**
         * 
         */
        private static final long serialVersionUID = -4449981369424872994L;

        public ResumeAction() {
    	    putValue(Action.NAME,
					 I18n.tr("Resume Download"));
			putValue(LimeAction.SHORT_NAME, 
					 I18n.tr("Resume"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Reattempt Selected Downloads"));
 			putValue(LimeAction.ICON_NAME, "DOWNLOAD_FILE_MORE_SOURCES");
		}
		
		public void performAction(ActionEvent e) {
			resumeSelectedDownloads();
		}
	}

	private class PauseAction extends RefreshingAction {

		/**
         * 
         */
        private static final long serialVersionUID = 4682149704934484393L;

        public PauseAction() {
			putValue(Action.NAME,
					 I18n.tr("Pause Download"));
			putValue(LimeAction.SHORT_NAME,
					 I18n.tr("Pause"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Pause Selected Downloads"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_PAUSE");
		}
		
		public void performAction(ActionEvent e) {
			pauseSelectedDownloads();
		}
	}

	private class ExploreAction extends RefreshingAction { 
		/**
         * 
         */
        private static final long serialVersionUID = -4648558721588938475L;

        public ExploreAction() { 
	        putValue(Action.NAME, 
	                 I18n.tr("Explore")); 
	        putValue(LimeAction.SHORT_NAME, 
	                 I18n.tr("Explore")); 
	        putValue(Action.SHORT_DESCRIPTION, 
	                 I18n.tr("Open Folder Containing the File")); 
	        putValue(LimeAction.ICON_NAME, "LIBRARY_EXPLORE"); 
	    } 
	     
	    public void performAction(ActionEvent e) { 
	        launchExplorer(); 
	    } 
	}
}