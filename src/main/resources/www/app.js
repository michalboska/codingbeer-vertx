window.onload = function() {
    var Q = Quintus({development: true}).include("Sprites, Scenes, Input, 2D, Touch, UI").setup("game").controls(true).touch();

    function playerObject(playerNum) {
        var result = {
            init: function (p) {
                this._super(p, {
                    asset: "black.png",
                    x: playerNum == 1 ? 10 : 570,
                    y: 250,
                    w: 20,
                    h: 100
                });

                this.add("2d");
            },
            bump: function() {
                console.log("bump");
            }
        }
        if (playerNum == 1) {
            result.step = function () {
                var vy = Q.inputs['up'] ? -200 : 0;
                vy =  Q.inputs['down'] ? 200 : vy;
                var vySingle = vy / 800;
                if (this.p.y + vySingle >= 0 && this.p.y + vySingle <= 600 - this.p.h) {
                    this.p.vy = vy;
                    console.log(this.p.y);
                } else {
                    this.p.vy = 0;
                }
            };
        }
        return result;
    }

    Q.Sprite.extend("Player1", playerObject(1));
    Q.Sprite.extend("Player2", playerObject(2));


    Q.scene("mainScene", function (stage) {
        var player1 = stage.insert(new Q.Player1);
        var player2 = stage.insert(new Q.Player2);
    });

    Q.gravityX = 0;
    Q.gravityY = 0;
    Q.input.keyboardControls();

    Q.load("black.png",
        function () {
            Q.stageScene("mainScene");
        }
    );
};


