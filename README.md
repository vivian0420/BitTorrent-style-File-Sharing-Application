# BitTorrent-style file sharing application
## Overview

In this project, I will extend Project 1 to support a BitTorrent-style file sharing application. BitTorrent is a communication protocol for P2P(peer to peer) sharing, which allows users to distribute data over the Internet in a decentralized way. 

The download process of BitTorrent can be roughly divided into 4 steps: **1.** Parse .torrent file. **2.** Request Tracker to get a list of Peers. **3.** Request Peers, download Piece, and check the validity of Piece according to the pieces field. **4.** Assemble Piece, get complete file. 


**1.Parse .torrent file:** Torrent file is a bencoded dictionary, containing a bunch of information about files and folders to be distributed. In this project, I am going to assume all the torrent files are in **Single File Mode**, and don't consider the cases of Multiple File Mode.  
**2.Request Tracker for Peers:**  Connect to tracker by HTTP server to ask the peers' information   
**3.Request Peers, download Piece:**  I will simplify this step. Only implement handshake, bitfield, request and piece. I am going to ignore have, choke, unchoke,ect.  
**4.Assemble Piece, get complete file:** Once each piece is correct, we can assemble the pieces to a complete file.

In addition, in the BitTorrent protocol, each host acts as both server and client, so except for the download process, a host should be able to handle the communication with other peers. For example, Handshake, Request,ect. 

Another critical piece of BitTorrent is concurrent download/upload, I will implement this feature in my project as well.

## Milestones

- **May 5 - Parse .torrent file** Use Bencode to parse the .torrent file to get information such as "announce", "info", "length", and "pieces".  
- **May 8 - handshake and bitfield** This includes "client" sending handshake messages, "server" handling handshake messages and sending bitfields.
- **May 12 - request and pieces** This includes "client" sending a request to the peers for downloading a certain piece and storing it into the file system. The "server" peers will be able to send the "piece" reponse.
- **May 15 - concurrent download/upload** a host should be able to download and upload concurrently.
- **May 17 - Integration Test** Test files can be correctly shared between VMs by using the BitTorrent protocol.
