function Game() {
    var Q = Quintus({development: true}).include("Sprites, Scenes, Input, 2D, Touch, UI").setup("game").controls().touch();
    Q.Sprite.extend("Player1")


    this.lala = 2 + trrr() + new Nested().getPrrr();
    function trrr() {
        return 1;
    }

    function Nested() {
        this.getPrrr = function() {
            return 42;
        }
    }
}
new Game();
