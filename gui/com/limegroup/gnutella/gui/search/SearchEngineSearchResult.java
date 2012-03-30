/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.limegroup.gnutella.gui.search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JPopupMenu;

import com.frostwire.bittorrent.websearch.WebSearchResult;
import com.frostwire.gui.GuiFrostWireUtils;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.util.PopupUtils;
import com.limegroup.gnutella.settings.BittorrentSettings;

public class SearchEngineSearchResult extends AbstractSearchResult {

    private WebSearchResult _item;
    private SearchEngine _searchEngine;
    private SearchInformation _info;

    public SearchEngineSearchResult(WebSearchResult item, SearchEngine searchEngine, SearchInformation searchInfo) {
        _item = item;
        _searchEngine = searchEngine;
        _info = searchInfo;
    }

    @Override
    public long getCreationTime() {
        return _item.getCreationTime();
    }

    @Override
    public String getExtension() {
        return "torrent";
    }

    @Override
    public String getFileName() {
        return _item.getFileName();
    }

    @Override
    public String getFilenameNoExtension() {
        return _item.getFilenameNoExtension();
    }

    @Override
    public int getQuality() {
        return 0;
    }

    public String getHash() {
        return _item.getHash();
    }

    public String getTorrentURI() {
        return _item.getTorrentURI();
    }

    @Override
    public long getSize() {
        return _item.getSize();
    }

    @Override
    public float getSpamRating() {
        return 0;
    }

    @Override
    public int getSpeed() {
        return Integer.MAX_VALUE - 2;
    }

    @Override
    public String getVendor() {
        return _item.getVendor();
    }

    @Override
    public void initialize(SearchResultDataLine line) {
        line.setAddedOn(getCreationTime());

        //hack this to show the icon for mininova or for isohunt.
    }

    @Override
    public boolean isMeasuredSpeed() {
        return false;
    }

    public boolean allowDeepSearch() {
        return true;
    }

    @Override
    public void takeAction(SearchResultDataLine line, GUID guid, File saveDir, String fileName, boolean saveAs, SearchInformation searchInfo) {
        GUIMediator.instance().openTorrentSearchResult(_item, false);
        showTorrentDetails(BittorrentSettings.SHOW_TORRENT_DETAILS_DELAY);
    }

    public void showTorrentDetails(long delay) {
        GuiFrostWireUtils.showTorrentDetails(delay, _searchEngine.redirectUrl, _info.getQuery(), _item.getTorrentDetailsURL(), getFileName());
    }

    @Override
    public JPopupMenu createMenu(JPopupMenu popupMenu, SearchResultDataLine[] lines, SearchResultMediator resultPanel) {

        PopupUtils.addMenuItem(SearchMediator.BUY_NOW_STRING, resultPanel.BUY_LISTENER, popupMenu, lines.length == 1, 0);
        PopupUtils.addMenuItem(SearchMediator.DOWNLOAD_STRING, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                takeAction(null, null, null, null, false, null);
            }
        }, popupMenu, lines.length > 0, 1);
        PopupUtils.addMenuItem(SearchMediator.DOWNLOAD_PARTIAL_FILES_STRING, resultPanel.DOWNLOAD_PARTIAL_FILES_LISTENER, popupMenu, lines.length == 1, 2);
        PopupUtils.addMenuItem(SearchMediator.TORRENT_DETAILS_STRING, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showTorrentDetails(-1);
            }
        }, popupMenu, lines.length == 1, 3);

        return popupMenu;
    }

    public int getSeeds() {
        return _item.getSeeds();
    }

    public SearchEngine getSearchEngine() {
        return _searchEngine;
    }

    public WebSearchResult getWebSearchResult() {
        return _item;
    }
}
