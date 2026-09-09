# Progress Notes (working)

## Commands
- Build + test: `mvn -q -B package -Ddir=/tmp/codecrafters-build-bittorrent-java`
- Run: `./your_program.sh <cmd> <args>`  (java --enable-preview -jar ...)
- Tests: JUnit 5 via surefire, run in `mvn test` / `package`.

## Remotes / branch
- Local branch: `main`
- `origin`       = git@github.com:preetsagar/BitTorrent.git  (GitHub mirror)
- `codecrafters` = https://git.codecrafters.io/4922ecac696e7637
- CodeCrafters ONLY runs tests on its `master` branch.
- Push sequence per stage:
    git push origin main
    git push codecrafters main:master
- "Test passed. Congrats!" + "Mark step as complete" in push output = stage passed.
- Tester runs only the current uncompleted stage per push; need a new commit each push.

## Local build note
- Local default JDK is 26; CodeCrafters uses java-25. Build locally with:
    export JAVA_HOME=/Users/preetsagar/Library/Java/JavaVirtualMachines/ms-25.0.4.1/Contents/Home

## Tester repo
- github.com/codecrafters-io/bittorrent-tester  (cloned to /tmp/bittorrent-tester)
- Stage funcs in internal/stage_*.go

## Stages (slug -> desc)
1. ns2  bencode string        DONE (passed)
2. eb4  bencode int           code done (decoder handles it)
3. ah1  bencode list          code done
4. mn6  bencode dict          code done
5. ow9  parse .torrent (info: Tracker URL, Length)
6. rb2  info hash (sha1 of bencoded info dict)
7. bf7  piece hashes (Piece Length + Piece Hashes list)
8. fi9  discover peers (tracker GET)
9. ca4  handshake
10. nd2 download piece
11. jv8 download file
12. hw0 parse magnet link
13. pk2 magnet reserved bit
14. xi4 magnet send extended handshake
15. jk6 magnet receive extended handshake
16. ns5 magnet request metadata
17. zh1 magnet send metadata
18. qv6 magnet download piece
19. dv7 magnet download file

## Architecture
- Main.java: CLI dispatch
- bencode/Bencode.java: decode (+ encode later for info hash)
- torrent/Torrent.java: parse .torrent
