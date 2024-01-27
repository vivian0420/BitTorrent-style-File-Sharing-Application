# BitTorrent-style file sharing application
## Overview

In this project, I implemented a BitTorrent-style file sharing application. BitTorrent is a communication protocol for P2P(peer to peer) sharing, which allows users to distribute data over the Internet in a decentralized way. 

The download process of BitTorrent can be roughly divided into 4 steps: **1.** Parse .torrent file. **2.** Request Tracker to get a list of Peers. **3.** Request Peers, download Piece, and check the validity of Piece according to the pieces field. **4.** Assemble Piece, get complete file. 


1. **Parse .torrent file:** Torrent file is a bencoded dictionary, containing a bunch of information about files and folders to be distributed. In this project, I assumed all the torrent files are in Single File Mode, and didn't consider the cases of Multiple File Mode.  
2. **Request Tracker for Peers:** Connect to tracker by HTTP server to ask the peers' information   
3. **Request Peers, download Piece:** In this project, I simplified the steps. Only implemented handshake, bitfield, request and piece.  
4. **Assemble Piece, get complete file:** Once each piece is correct, we can assemble the pieces to a complete file.

In addition, in the BitTorrent protocol, each host acts as both server and client, so except for the download process, a host should be able to handle the communication with other peers. For example, Handshake, Request,ect. 

Another critical piece of BitTorrent is concurrent download/upload. The idea is that, while you're downloading a file, you're also helping to upload it to others who need it. And everyone will be doing the same, so it's one of the reasons why it can be so fast. I implemented this feature in my project as well.

## Milestones

- **May 5 - Connect to the peers** First, Use Bencode to parse the .torrent file to get information such as "announce", "info", "length", and "pieces". Second, Connect to Tracker and ask the information of peers, then parse the response from Tracker and get the ip and port of each peer. Last, connect to each peer.  
- **May 8 - Handshake and bitfield** This includes "client" sending handshake messages, "server" handling handshake messages and sending bitfields.
- **May 12 - Request and pieces** This includes "client" sending a request to the peers for downloading a certain piece and storing it into the file system. The "server" peers will be able to send the "piece" reponse.
- **May 15 - concurrent download/upload** a host should be able to download and upload concurrently.
- **May 17 - Integration Test** Test files can be correctly shared between VMs by using the BitTorrent protocol.
