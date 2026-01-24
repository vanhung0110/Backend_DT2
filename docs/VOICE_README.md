Voice feature (Agora) - setup & test

Overview
- Backend provides endpoints to join/leave/kick/list voice members for rooms, and additionally exposes the same voice operations for `friend-rooms` (e.g. `/friend-rooms/{id}/voice/join`). Friend-direct rooms are voice-enabled by default.
- Web client (`VoicePanel`) integrates with Agora Web SDK and uses server-provided token + appId to join channels.
- RN requires native integration (`react-native-agora`) and an EAS/native build.

Environment variables
- Set these for server (application.properties or environment):
  - voice.agora.appId
  - voice.agora.appCertificate

Backend steps (server)
1. If you have Agora credentials, set them as env or in `src/main/resources/application.properties`:
   voice.agora.appId=YOUR_APP_ID
   voice.agora.appCertificate=YOUR_CERT
2. Restart backend. The `AgoraTokenService` will return a valid token if SDK integration is added.
3. Endpoints:
   - POST /rooms/{id}/voice/join  -> { token, channel, expiresAt, appId }
   - POST /rooms/{id}/voice/leave
   - POST /rooms/{id}/voice/kick?userId=
   - GET  /rooms/{id}/voice/members

Web client steps
1. Install deps: `npm install` (already set to include `agora-rtc-sdk-ng`).
2. Start the app: `npm start`.
3. Open a room page where voice is enabled (set `voice_enabled` on the room row in DB).
4. Use the Join button on the `VoicePanel` to request token and connect.
5. Test in two browser tabs to confirm audio publish/subscription, speaking indicator and mute/volume.

RN (Expo) steps
1. Add `react-native-agora` to the project - requires native modules and EAS build. I installed `react-native-agora@^4.4.2` in the project. Note: this is a native dependency—after install you must run `expo prebuild` and use EAS to build (`eas build -p android` / `eas build -p ios`).
2. Prebuild/EAS flow recommended:
   - Add `eas.json` with env variables for Agora credentials (example `eas.json` provided in repo root).
   - `expo prebuild` then `eas build -p android` and `eas build -p ios` after adding config.
3. Implement native permission and background settings:
   - Android: foreground service for persistent audio
   - iOS: Background audio entitlement and `UIBackgroundModes` in Info.plist
4. Run on device to fully validate background behavior and per-user volume.

RT rooms (new)
- New endpoints (server):
  - POST `/rt-rooms` - create real-time room
  - GET `/rt-rooms` - list public RT rooms
  - POST `/rt-rooms/{id}/join` - join RT room (returns token, channel, appId)
  - POST `/rt-rooms/{id}/leave` - leave
  - POST `/rt-rooms/{id}/kick?userId=` - kick member (owner only)
  - GET `/rt-rooms/{id}/members` - list RT members
- DB migration: `V8__rt_rooms.sql` creates `rt_rooms` and `rt_members`.
- Frontend: `RtLobby` and `RtRoomPanel` components (web/native) to list/create/join RT rooms. UI aligns with existing theme.
- Friend chat remains unchanged and continues to use `/friend-rooms` endpoints; RT rooms are fully separated.

Testing checklist
- Backend: all unit and integration tests in `src/test/java` should pass (`mvnw test`).
- Web: connecting two clients results in audible audio streams and speaking indicator.
- RN: same as web, and verify background/foreground and mute/volume features.

Notes & next steps
- I can integrate Agora server-side token generation using Agora's Java AccessToken builder if you provide the credentials; until then server uses a stub token and web audio won't work.
- I can also prepare EAS config and a sample RN integration branch and run EAS builds if you authorize or provide credentials for testing devices.
