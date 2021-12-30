# wal-cli
CLI Identity Wallet Implementation

Requirements:

Mongodb is required
https://www.mongodb.com/try/download/community

Mongodb Windows WSL setup
https://docs.microsoft.com/en-us/windows/wsl/tutorials/wsl-database#install-mongodb


Windows + WSL 2 add environment variable:
nano ~/.bashrc
add to file:
export PRISM_SDK_PASSWORD=<value>
export DISPLAY=<X11 Host>:0.0

Windows + WSL 2:
Install: https://sourceforge.net/projects/vcxsrv/
start mongodb:
sudo mongod --dbpath ~/data/db

Windows Environment variables
Restart IDE after adding/changing environment variables