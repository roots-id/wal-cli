@Echo off
java -jar -agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y .\build\libs\wal-cli-1.0.1-SNAPSHOT-all.jar %*