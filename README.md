# wal-cli
CLI Identity Wallet Implementation

## Requirements:

- Gradle version 7, or newer  

- Install Mongodb: https://www.mongodb.com/try/download/community  
  **note:** WAL will connect to mongodb on localhost:27017

- Set the following environment variables:

  - IOG Repository credentials:    
    `PRISM_SDK_USER = (Request to IOG)`  
    `PRISM_SDK_PASSWORD = (Request to IOG)`

  - Atala PRISM Node host and port:  
    `PRISM_NODE_HOST = (Request to IOG)`  
    `PRISM_NODE_PORT = (Request to IOG, default 50053)`
  - Atala PRISM protocol and token:
    `PRISM_NODE_PROTOCOL = http`
    `PRISM_NODE_TOKEN = null`

  - Github credentials, generate a personal access token 
  [(PAT)](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) with **read:packages** option enabled.    
    `GITHUB_USER = <github user>`      
    `GITHUB_TOKEN = <PAT Token>`  
## Installation

- Clone this repository: `git clone https://github.com/roots-id/wal-cli.git`
- Checkout the main branch: `git checkout main` ⚠
- Using a terminal run the following command on the repository root folder: `gradle run shadowJar`
- On linux and mac run: `chmod u+x wal.sh` 
- Use the command `./wal.sh -h` (linux, mac) or `wal -h` (windows) to see WAL options:

```
Usage: wal options_list
Subcommands:
    new-wallet - Create a wallet
    list-wallets - List wallets
    show-mnemonic - Show wallet mnemonic phrase and passphrase
    export-wallet - Export a wallet
    import-wallet - Import a wallet
    new-did - Create a DID
    list-dids - List wallet DIDs
    show-did - Show a DID document
    publish-did - Publish a DID
    resolve-prism-did - Resolve PRISM did and show DID document
    issue-cred - Issue a credential
    verify-cred - Verify a credential
    export-cred - Export an issued credential
    import-cred - Import a credential
    revoke-cred - Revoke a credential
    add-key - Add a key to a DID
    revoke-key - Revoke DID key
    create-peer-did - Creates a new Peer DID and corresponding secrets
    resolve-peer-did - Resolve a Peer DID to DID Doc JSON
    pack - Packs the message
    unpack - Unpacks the message

Options:
    --help, -h -> Usage info
```
# Important  

The tool isn't intended for production use. Its design doesn't contemplate security. Data is stored in the database in plain text to expose the semantics and facilitate inspection. Eventually, an encryption layer will be added but right now is not a priority.

# See also

- [Tutorial](https://github.com/roots-id/wal-cli/wiki/Usage-examples)  
- [Technical discussions regarding WAL-CLI implementation](https://github.com/roots-id/wal-cli/discussions/2)
- [Bug Report and feature request](https://github.com/roots-id/wal-cli/issues/new/choose)
- [Original proposal](https://cardano.ideascale.com/c/idea/381281)

### Notes
- Windows WSL2 setup:
  - Mongodb:
    - Mongodb Windows WSL setup [here](https://docs.microsoft.com/en-us/windows/wsl/tutorials/wsl-database#install-mongodb)
    - To start mongodb: `sudo mongod --dbpath ~/data/db`
    - Check if the mongo instance is running `ps -e | grep 'mongod'`
    - mongosh usage [instructions](https://www.mongodb.com/docs/mongodb-shell/run-commands/)
  - Environment variables:
    - To add environment variables: `nano ~/.bashrc`
    - Add the environment variables to the file: `export <variable name>= <value>`
    - load the new envvars: `source ~/.bashrc`
    - Check envvars: `printenv`

