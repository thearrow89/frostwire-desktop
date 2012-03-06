package com.aelitis.azureus.plugins.magnet.metadata;

import java.util.Map;

import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequestListener;
import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManagerAdapter;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;

import com.aelitis.azureus.core.peermanager.PeerManagerRegistration;

public class MetadataPeerManagerAdapter implements PEPeerManagerAdapter {

    @Override
    public void statsRequest(PEPeer originator, Map request, Map reply) {
    }

    @Override
    public void setTrackerRefreshDelayOverrides(int percent) {
    }

    @Override
    public void setStateSeeding(boolean never_downloaded) {
    }

    @Override
    public void setStateFinishing() {
    }

    @Override
    public void restartDownload(boolean forceRecheck) {
    }

    @Override
    public void removePiece(PEPiece piece) {
    }

    @Override
    public void removePeer(PEPeer peer) {
    }

    @Override
    public void protocolBytesSent(PEPeer peer, int bytes) {
    }

    @Override
    public void protocolBytesReceived(PEPeer peer, int bytes) {
    }

    @Override
    public void priorityConnectionChanged(boolean added) {
    }

    @Override
    public void permittedSendBytesUsed(int bytes) {
    }

    @Override
    public void permittedReceiveBytesUsed(int bytes) {
    }

    @Override
    public boolean isPeriodicRescanEnabled() {
        return false;
    }

    @Override
    public boolean isPeerSourceEnabled(String peer_source) {
        return false;
    }

    @Override
    public boolean isPeerExchangeEnabled() {
        return true;
    }

    @Override
    public boolean isNATHealthy() {
        return false;
    }

    @Override
    public boolean isExtendedMessagingEnabled() {
        return true;
    }

    @Override
    public boolean hasPriorityConnection() {
        return false;
    }

    @Override
    public int getUploadRateLimitBytesPerSecond() {
        return 0;
    }

    @Override
    public TRTrackerScraperResponse getTrackerScrapeResponse() {
        return null;
    }

    @Override
    public String getTrackerClientExtensions() {
        return null;
    }

    @Override
    public byte[][] getSecrets(int crypto_level) {
        return null;
    }

    @Override
    public long getRandomSeed() {
        return 0;
    }

    @Override
    public int getPosition() {
        return 0;
    }

    @Override
    public int getPermittedBytesToSend() {
        return 0;
    }

    @Override
    public int getPermittedBytesToReceive() {
        return 0;
    }

    @Override
    public PeerManagerRegistration getPeerManagerRegistration() {
        return new PeerManagerRegistration() {

            @Override
            public void unregister() {
            }

            @Override
            public void removeLink(String link) {
            }

            @Override
            public TOTorrentFile getLink(String link) {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void deactivate() {
            }

            @Override
            public void addLink(String link, TOTorrentFile target) throws Exception {
            }

            @Override
            public void activate(PEPeerControl peer_control) {
            }
        };
    }

    @Override
    public int getMaxUploads() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxSeedConnections() {
        return 0;
    }

    @Override
    public int getMaxConnections() {
        return 0;
    }

    @Override
    public LogRelation getLogRelation() {
        return null;
    }

    @Override
    public int getDownloadRateLimitBytesPerSecond() {
        return 0;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public int getCryptoLevel() {
        return 0;
    }

    @Override
    public void enqueueReadRequest(PEPeer peer, DiskManagerReadRequest request, DiskManagerReadRequestListener listener) {
    }

    @Override
    public void discarded(PEPeer peer, int bytes) {
    }

    @Override
    public void dataBytesSent(PEPeer peer, int bytes) {
    }

    @Override
    public void dataBytesReceived(PEPeer peer, int bytes) {
    }

    @Override
    public void addPiece(PEPiece piece) {
    }

    @Override
    public void addPeer(PEPeer peer) {
        peer.addListener(new MetadataPeerListener());
    }

    @Override
    public void addHTTPSeed(String address, int port) {
    }
}