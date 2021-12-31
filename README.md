# wal-cli
CLI Identity Wallet Implementation

## Requirements:

- Install Mongodb: https://www.mongodb.com/try/download/community

- Set the following environment variables:

  - IOG Repository credentials:
  
    `PRISM_SDK_USER = atala-dev`

    `PRISM_SDK_PASSWORD = (Request to IOG)`

  - RootsId Repository credentials, generate a personal access token 
  [(PAT)](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token).
  Since the repo is private it should only work with the team members.
  
    `ROOTS-ID_USER = <github user>`
    
    `ROOTS-ID_PASSWORD = <PAT Token>`


### Notes
- On Windows restart IDE after adding/changing environment variables.
- Windows WSL + 2 setup:
  - Mongodb Windows WSL setup [here](https://docs.microsoft.com/en-us/windows/wsl/tutorials/wsl-database#install-mongodb)
  - To add environment variable:  
    `nano ~/.bashrc`
  - Add the environment variables to the file:  
    `export <variable name>= <value>`
  - To display QR on screen, Install [vcxsrv](https://sourceforge.net/projects/vcxsrv/) and add the following to 
  ~/.bashrc file:  
    `export DISPLAY=<X11 Host>:0.0`
  - To start mongodb:  
  `sudo mongod --dbpath ~/data/db`

