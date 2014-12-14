var UIModel = function() {
    var Q = Quintus({development: true}).include("Sprites, Scenes, Input, 2D, Touch, UI").setup("game").controls(true).touch();
    var thisUiModel = this;
    var lblScore1, lblScore2, lblOverlay;

    this.onPlayerMove = null;
    this.onStageLoaded = null;

    function playerObject(playerNum) {
        var result = {
            init: function (p) {
                this._super(p, {
                    asset: "black.png",
                    x: playerNum == 1 ? 10 : 1014,
                    y: 280
                });
                this.add("2d");
            }
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
        lblScore1 = stage.insert(new Q.UI.Text({x: 30, y: 50, label: "0"}));
        lblScore2 = stage.insert(new Q.UI.Text({x: 984, y: 50, label: "0"}));
        lblOverlay = stage.insert(new Q.UI.Text({x: 512, y: 300, size: 30, label: "Waiting for other player..."}));
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
        Q.load("black.png, green.png",
            function () {
                Q.stageScene("mainScene");
            }
        );
    }

    this.setControllablePlayer = function(playerNum) {
        var object = playerNum == 1 ? thisUiModel.player1 : thisUiModel.player2;
        object.step = function () {
            var vy = Q.inputs['up'] ? -200 : 0;
            vy =  Q.inputs['down'] ? 200 : vy;
            var vySingle = vy / 30;
            if (vy != 0 && this.p.y + vySingle >= 50 && this.p.y + vySingle <= 650 - this.p.h) {
                this.p.y += vySingle;
                if (thisUiModel.onPlayerMove != null) {
                    thisUiModel.onPlayerMove(object);
                }
            }
        };
        object.p.asset = "green.png";
        lblOverlay.p.hidden = true;
    };

    this.setScore = function(score1, score2) {
        lblScore1.p.label = score1.toString();
        lblScore2.p.label = score2.toString();
    };

    this.setWinningPlayer = function(playerNum) {
        lblOverlay.p.label = "Player " + playerNum + " wins!";
        lblOverlay.p.hidden = false;
    }
};


var Controller = function() {
    const GAME_PUBLIC_QUEUE_PREFIX = "Game.public-";
    const GAME_INPUT_QUEUE_PREFIX = "Game.input-";
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
                        var address = GAME_PUBLIC_QUEUE_PREFIX + gameGUID;
                        eb.registerHandler(address, onGameMessageReceived);
                        inputQueue = GAME_INPUT_QUEUE_PREFIX + gameGUID;
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
        if (message.type == "state") {
            ball.p.x = message.ballx + ball.p.cx;
            ball.p.y = message.bally + ball.p.cy;
            //always update state of the other player
            var y = 0;
            if (myPlayerNumber == 1) {
                y = message.player2pos;
                player2.p.y = y + player2.p.cy;
            } else if (myPlayerNumber == 2) {
                y = message.player1pos;
                player1.p.y = y + player1.p.cy;
            }
            model.setScore(message.player1score, message.player2score);
        } else if (message.type == "command") {
            if (message.command == "start") {
                model.setControllablePlayer(myPlayerNumber);
            } else if (message.command == "win1") {
                model.setWinningPlayer(1);
            } else if (message.command == "win2") {
                model.setWinningPlayer(2);
            }
        }
    }

    function onPlayerMove(player) {
        if (inputQueue == null) {
            return;
        }
        eb.send(inputQueue, {type: "move", guid: playerGUID, y: player.p.y - player.p.cy}, function(reply){
            var y = reply.y;
            var clientY = player.p.y - player.p.cy;
            //fix position from server if the difference is too high
            if (Math.abs(y - clientY) > 20) {
                player.p.y = y + player.p.cy;
            }
        });
    }
}


window.onload = function() {
    window.controller = new Controller();
}
