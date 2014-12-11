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
                    y: 250
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
                x: 150,
                y: 150,
                w: 20,
                h: 20,
                cx: 10,
                cy: 10,
                baseSpeed: 100
            });
            this.add("2d");
            this.p.vx = -this.p.baseSpeed;
            this.p.vy = this.p.baseSpeed;
            this.on("hit.sprite", this, "bump");
        },
        draw: function(ctx) {
            ctx.fillStyle = this.p.color;
            ctx.fillRect(-this.p.cx, -this.p.cy, this.p.w, this.p.h);
        },
        bump: function(collision) {
            console.log(collision);
            if (collision.obj.isA("Player1")) {
                this.p.vx = this.p.baseSpeed;
            }
            if (collision.obj.isA("Player2")) {
                this.p.vx = -this.p.baseSpeed;
            }
            if (collision.obj.isA("WallTop")) {
                this.p.vy = this.p.baseSpeed;
            }
            if (collision.obj.isA("WallBottom")) {
                this.p.vy = -this.p.baseSpeed;
            }
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
    var queue;
    var playerGUID, gameGUID;
    var player1, player2, ball;
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
        eb.send(LOBBY_QUEUE, {type: "listPlayers"}, function(result) {
            console.log(result);
        });
        eb.send(LOBBY_QUEUE, {type: "addPlayer", "name": "Player" + new Date().getTime()}, function(addPlayerResult){
            if (addPlayerResult.status == "ok") {
                playerGUID = addPlayerResult.guid;
                eb.send(LOBBY_QUEUE, {type: "addGame", playerGuid: playerGUID, name: "Game" + new Date().getTime()}, function(addGameResult) {
                    if (addGameResult.status == "ok") {
                        gameGUID = addGameResult.guid;
                        var address = GAME_QUEUE_PREFIX + gameGUID;
                        console.log("Registering on " + address);
                        eb.registerHandler(address, onGameMessageReceived);
                    }
                    console.log(addGameResult);
                });
            }
            console.log(addPlayerResult);
        });
    }

    function onGameMessageReceived(message) {

        player2.p.y = message + player2.p.cy;
    }

    function onPlayerMove() {
        console.log("move");
    }
}


window.onload = function() {
    window.controller = new Controller();
}
