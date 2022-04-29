# BitTorrent-style file sharing application
## Overview

In this project, I will extend Project 1 to support a BitTorrent-style file sharing application. BitTorrent is a communication protocol for P2P(peer to peer) sharing, which allows users to distribute data over the Internet in a decentralized way. 

The download process of BitTorrent can be roughly divided into 4 steps: **1.** Parse .torrent file. **2.** Request Tracker to get a list of Peers. **3.** Request Peers, download Piece, and check the validity of Piece according to the pieces field. **4.** Assemble Piece, get complete file. 


**1.Parse .torrent file:** Torrent file is a bencoded dictionary, containing a bunch of information about files and folders to be distributed. In this project, I am going to assume all the torrent files are in **Single File Mode**, and don't consider the cases of Multiple File Mode.  
**2.Request Tracker for Peers:**  In this project, I will use configuration to replace this step, which means I am not going to ask the tracker for peers' information, instead I will read the peers' information from a configuration. (TBD, waiting for Sami's confirmation)   
**3.Request Peers, download Piece:**  This step also can be divided into 4 parts, including **handshake**; receive **Bitfield** message from peer; wait for peer's **Unchoke** message; download Pieces.   
**4.Assemble Piece, get complete file:** Once each piece is correct, we can assemble the pieces to a complete file.

In addition, in the BitTorrent protocol, each host acts as both server and client, so except for the download process, a host should be able to handle the communication with other peers. For example, Handshake, Request, Chock, Unchoke,Piece, ect. 

(TBD) Unlike a real BitTorrent client, where upload and download happen at the same time. For now, they will be separated into 2 applications. If an application is doing download, it will only download. If it is doing upload, it will only upload.
## Milestones

- **May 6 - Parse .torrent file** Use Bencode to parse the .torrent file to get information such as "announce", "info", "length", and "pieces".  

- **May 9 - handshake and bitfield** This includes "client" sending handshake messages, "server" handling handshake messages and sending bitfield, unchock, chock message, ect. 

- **May 14 - request and pieces** This includes "client" sending a request to the peers for downloading a certain piece and storing it into the file system. The "server" peers will be able to send the "piece" reponse.

- **May 17 - Integration Test** Test files can be correctly shared between VMs by using the BitTorrent protocol.
