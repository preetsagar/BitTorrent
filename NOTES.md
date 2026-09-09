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
1. ns2  bencode string        PASSED
2. eb4  bencode int           PASSED
3. ah1  bencode list          PASSED
4. mn6  bencode dict          PASSED
5. ow9  parse .torrent        PASSED
6. rb2  info hash             PASSED
7. bf7  piece hashes          PASSED
8. fi9  discover peers        PASSED
9. ca4  handshake             PASSED
10. nd2 download piece        PASSED
11. jv8 download file         code done + smoke-tested
12. hw0 parse magnet link     code done + smoke-tested
13. pk2 magnet reserved bit   code done
14. xi4 magnet send ext hs    code done
15. jk6 magnet recv ext hs    code done + smoke-tested (Peer Metadata Extension ID)
16. ns5 magnet request meta   code done
17. zh1 magnet send meta      code done + smoke-tested (magnet_info full)
18. qv6 magnet download piece code done + smoke-tested
19. dv7 magnet download file  code done + smoke-tested (SHA1 verified)

ALL 19 STAGES IMPLEMENTED. Pushing in batches; tester runs cumulatively (~2 stages/push).

## Architecture
- Main.java: CLI dispatch
- bencode/Bencode.java: decode (+ encode later for info hash)
- torrent/Torrent.java: parse .torrent
