codingbeer-vertx
================

A real-time web minigame demonstrating the use of server-side vert.x (java 8) and client-side Javascript. Used as an example at ERNI Coding beer session.

The server side consists of more Verticles. Main verticle is ConfigVerticle which initializes other verticles with default config values

Client side is made with Javascript http://www.html5quintus.com/#demo HTML5 engine.

NOTICE: This is by no means a complete fool-proof implementation of the game. Some topics that would require more attention in production-quality software:
 - Data efficiency: every frame is calculated on the server side and sent as a real-time update (every ~20ms). This may cause unnecessary lags on slower/high ping connections.
   More efficient way would be only to send change-state messages (ball has hit a wall, player has hit/missed the ball etc...) and let client-side javascript do the simple movement interpolation
 - Security: Current security implementation relies on clients not knowing other players' or games' GUID. GUID is supposed to be unique, so we assume there are no collisions (this is ok), but
   if someone finds out GUID of some other player, he could influence his games and act on his behalf. In a real world, authentication/authorization/"session" mechanisms would be needed
 - Proper cleanup/disconnect handling: game verticles for which the game has ended are being automatically terminated. However, we don't handle player disconnects
   (when player closes the browser and breaks the WebSocket connection). Also after the game ends, player GUIDs and Player instances don't get cleaned up (I have not yet found how to handle
   WebSocket disconnects in vert.x and pair these closed connections to some state (e.g. player GUID)) -- feel free to submit a pull request for this feature :)
 - and some more I can't currently think of... :o)
