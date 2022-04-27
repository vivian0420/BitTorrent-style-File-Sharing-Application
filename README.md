# BitTorrent-style file sharing application
## Overview

In this project, I will extend Project 1 to support a BitTorrent-style file sharing application. BitTorrent is a communication protocol for P2P(peer to peer) sharing, which allows users to distribute data over the Internet in a decentralized way.

Now I have leaned that in the BitTorrent protocol, publisher should release a .torrent file while it intends to publish some data, and there are a bunch of informations in the .torrent file, like "trackers" provide a list of files available for transfer and allow the client to find peer users; "pieces" provides a way to verify the portions that have been downloaded through checksum. 

Next, I am going to learn the BitTorrent protocol, figure out how the protocol works and apply it into my project. The goal of this project is test files can be correctly shared between VMs by using the BitTorrent protocol.
## Milestones

- **May 7**  Learned and explored BitTorrent protocol---At this time, I should be able to understand how BitTorrent protocol works.
- **May 12**  Implemented partial BitTorrent protocol---At this time , I should be able to implement at least partial BitTorrent protocol's functionalities,like data encode and decode, detect correctness by data's checksum,ect.
- **May 16**  Test files could be correctly transferred between different VMs by using BitTorrent protocol.
