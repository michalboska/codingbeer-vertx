var UIModel = function() {
    var Q = Quintus({development: true}).include("Sprites, Scenes, Input, 2D, Touch, UI").setup("game").controls(true).touch();
    var FPS = 30;
    var thisUiModel = this;

    this.onPlayerMove = null;
    this.onStageLoaded = null;

    function playerObject(playerNum) {
        var result = {
            init: function (p) {
                this._super(p, {
                    asset: "black.png",
                    x: playerNum == 1 ? 10 : 994,
                    y: 280
                });
                this.add("2d");
            }
        }
        if (playerNum == 1) {
            result.step = function () {

                var vy = Q.inputs['up'] ? -200 : 0;
                vy =  Q.inputs['down'] ? 200 : vy;
                var vySingle = vy / 30;
                if (vy != 0 && this.p.y + vySingle >= 50 && this.p.y + vySingle <= 650 - this.p.h) {
                    this.p.y += vySingle;
                    if (thisUiModel.onPlayerMove != null) {
                        thisUiModel.onPlayerMove();
                    }
                }
            };
        }
        return result;
    }

    function wallObject(wall) {
        var y = wall == "top" ? 0 : 580;
        var result = {
            init: function(p) {
                this._super(p, {
                    x: 512,
                    y: wall == "top" ? 10 : 590,
                    w: 1024,
                    h: 20,
                    cx: 512,
                    cy: 10,
                    color: "blue"
                });
            },
            draw: function(ctx) {
                ctx.fillStyle = this.p.color;
                ctx.fillRect(-this.p.cx, -this.p.cy, this.p.w, this.p.h);
            }
        };
        return result;
    }

    Q.Sprite.extend("Player1", playerObject(1));
    Q.Sprite.extend("Player2", playerObject(2));
    Q.Sprite.extend("WallTop", wallObject("top"));
    Q.Sprite.extend("WallBottom", wallObject("bottom"));
    Q.Sprite.extend("Ball", {
        init: function(p) {
            this._super(p,  {
                color: "red",
                x: 512,
                y: 300,
                w: 20,
                h: 20,
                cx: 10,
                cy: 10
            });
            this.add("2d");
        },
        draw: function(ctx) {
            ctx.fillStyle = this.p.color;
            ctx.fillRect(-this.p.cx, -this.p.cy, this.p.w, this.p.h);
        }
    });

    Q.scene("mainScene", function (stage) {
        thisUiModel.player1 = stage.insert(new Q.Player1);
        thisUiModel.player2 = stage.insert(new Q.Player2);
        thisUiModel.ball = stage.insert(new Q.Ball);
        stage.insert(new Q.WallTop);
        stage.insert(new Q.WallBottom);
        if (thisUiModel.onStageLoaded != null) {
            thisUiModel.onStageLoaded();
        }
    });



    Q.gravityX = 0;
    Q.gravityY = 0;
    Q.input.keyboardControls();

    this.uiInit = function() {
        Q.load("black.png",
            function () {
                Q.stageScene("mainScene");
            }
        );
    }
};


var Controller = function() {
    const GAME_QUEUE_PREFIX = "Game.public-";
    const LOBBY_QUEUE = "ch.erni.beer.vertx.GameLobbyVerticle.queue";
    var inputQueue;
    var playerGUID, gameGUID;
    var player1, player2, ball;
    var myPlayerNumber;
    var eb;
    var model = new UIModel();

    model.onPlayerMove = onPlayerMove;
    model.onStageLoaded = function() {
        player1 = model.player1;
        player2 = model.player2;
        ball = model.ball;
        eb = new vertx.EventBus(document.URL + "eventbus");
        eb.onopen = function() {
            registerToGame();
        }
    };
    model.uiInit();

    function registerToGame() {
        eb.send(LOBBY_QUEUE, {type: "addPlayer", "name": "Player" + new Date().getTime()}, function(addPlayerResult){
            if (addPlayerResult.status == "ok") {
                playerGUID = addPlayerResult.guid;
                eb.send(LOBBY_QUEUE, {type: "getAvailableGame"}, function(availGameResult){
                    var onGameRegistered = function(result) {
                        var address = GAME_QUEUE_PREFIX + gameGUID;
                        inputQueue = address + "-" + playerGUID;
                        console.log("Registering on " + address);
                        eb.registerHandler(address, onGameMessageReceived);
                    };
                    var availGameGuid = availGameResult.guid;
                    if (availGameGuid != null) { //join existing game
                        gameGUID = availGameGuid;
                        myPlayerNumber = 2;
                        eb.send(LOBBY_QUEUE, {type: "joinGame", playerGuid: playerGUID, gameGuid: gameGUID}, function(joinGameResult){
                            if (joinGameResult.status == "ok") {
                                 onGameRegistered(joinGameResult);
                            }
                        });
                    } else { //create a new game
                        myPlayerNumber = 1;
                        eb.send(LOBBY_QUEUE, {type: "addGame", playerGuid: playerGUID, name: "Game" + new Date().getTime()}, function(addGameResult) {
                            if (addGameResult.status == "ok") {
                                gameGUID = addGameResult.guid;
                                onGameRegistered(addGameResult);
                            }
                        });
                    }
                });
            }
        });
    }

    function onGameMessageReceived(message) {
        console.log(message);
        if (message.type == "state") {
            ball.p.x = message.ballx + ball.p.cx;
            ball.p.y = message.bally + ball.p.cy;
        } else if (message.type == "command") {

        }


    }

    function onPlayerMove() {
        if (inputQueue == null) {
            return;
        }
        eb.send(inputQueue, {type: "move", guid: playerGUID, y: player1.p.y - player1.p.cy}, function(reply){
            var y = reply.y;
            var clientY = player1.p.y - player1.p.cy;
            //fix position from server if the difference is too high
            if (Math.abs(y - clientY) > 10) {
                console.log("fixing client " + clientY + " vs server " + y);
                player1.p.y = y + player1.p.cy;
            } else {
                console.log("ok");
            }
        });
    }
}


window.onload = function() {
    window.controller = new Controller();
}
