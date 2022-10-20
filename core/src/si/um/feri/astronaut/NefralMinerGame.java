package si.um.feri.astronaut;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Iterator;

public class NefralMinerGame extends ApplicationAdapter {
    private Texture backgroundImage;
    private Texture crystalImage;
    private Texture playerImage;
    private Texture oreImage;
    private Sound pickupSound;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Rectangle player;
    private Array<Rectangle> crystals;    // special LibGDX Array
    private Array<Rectangle> ores;
    private long lastCrystalTime;
    private long lastOreTime;
    private int crystalPickUpScore;
    private int playerHP;    // starts with 100

    public BitmapFont font;

    // all values are set experimental
    private static final int SPEED = 600;    // pixels per second
    private static final int SPEED_CRYSTAL = 200; // pixels per second
    private static int SPEED_ORE = 100;    // pixels per second
    private static final long CREATE_CRYSTAL_TIME = 1000000000;    // ns
    private static final long CREATE_ORE_TIME = 2000000000;    // ns

    @Override
    public void create() {
        font = new BitmapFont();
        font.getData().setScale(2);
        crystalPickUpScore = 0;
        playerHP = 100;

        // default way to load a texture
        backgroundImage = new Texture(Gdx.files.internal("background-mine.png"));
        playerImage = new Texture(Gdx.files.internal("Nefral-Miner.png"));
        crystalImage = new Texture(Gdx.files.internal("crystal.png"));
        oreImage = new Texture(Gdx.files.internal("ore.png"));
        pickupSound = Gdx.audio.newSound(Gdx.files.internal("pick.wav"));

        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        Gdx.graphics.setWindowedMode(1000,800);
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        //camera.setToOrtho(false, 1000,800);
        batch = new SpriteBatch();

        // create a Rectangle to logically represents the rocket
        player = new Rectangle();
        player.x = Gdx.graphics.getWidth() / 2f - playerImage.getWidth() / 2f;    // center the rocket horizontally
        player.y = 20;    // bottom left corner of the rocket is 20 pixels above the bottom screen edge
        player.width = playerImage.getWidth();
        player.height = playerImage.getHeight();

        crystals = new Array<Rectangle>();
        ores = new Array<Rectangle>();
        // add first crystal and ore
        spawnCrystal();
        spawnOre();
    }

    /**
     * Runs every frame.
     */
    @Override
    public void render() {
        // clear screen
        Gdx.gl.glClearColor(0, 0, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        // process user input
        if (Gdx.input.isTouched()) commandTouched();    // mouse or touch screen
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) commandMoveLeft();
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) commandMoveRight();
        if (Gdx.input.isKeyPressed(Input.Keys.A)) commandMoveLeftCorner();
        if (Gdx.input.isKeyPressed(Input.Keys.S)) commandMoveRightCorner();
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) commandExitGame();

        // check if we need to create a new crystal/ore
        if (TimeUtils.nanoTime() - lastCrystalTime > CREATE_CRYSTAL_TIME) spawnCrystal();
        if (TimeUtils.nanoTime() - lastOreTime > CREATE_ORE_TIME) spawnOre();

        if (playerHP > 0) {    // is game end?
            // move and remove any that are beneath the bottom edge of
            // the screen or that hit the rocket
            for (Iterator<Rectangle> it = ores.iterator(); it.hasNext(); ) {
                Rectangle ore = it.next();
                ore.y -= SPEED_ORE * Gdx.graphics.getDeltaTime();
                if (ore.y + oreImage.getHeight() < 0) it.remove();
                if (ore.overlaps(player)) {
                    pickupSound.play();
                    playerHP--;
                }
            }

            for (Iterator<Rectangle> it = crystals.iterator(); it.hasNext(); ) {
                Rectangle crystal = it.next();
                crystal.y -= SPEED_CRYSTAL * Gdx.graphics.getDeltaTime();
                if (crystal.y + crystalImage.getHeight() < 0) it.remove();    // from screen
                if (crystal.overlaps(player)) {
                    pickupSound.play();
                    crystalPickUpScore++;
                    if (crystalPickUpScore % 10 == 0) SPEED_ORE += 66;    // speeds up
                    it.remove();    // smart Array enables remove from Array
                }
            }
        } else {    // health of rocket is 0 or less
            batch.begin();
            {
                font.setColor(Color.RED);
                font.draw(batch, "The END", Gdx.graphics.getHeight() / 2f, Gdx.graphics.getHeight() / 2f);
            }
            batch.end();
        }

        // tell the camera to update its matrices.
        camera.update();

        // tell the SpriteBatch to render in the
        // coordinate system specified by the camera
        batch.setProjectionMatrix(camera.combined);

        // begin a new batch and draw the rocket, crystals, ores
        batch.begin();
        {    // brackets added just for indent
            batch.draw(backgroundImage,0,0);
            batch.draw(playerImage, player.x, player.y);
            for (Rectangle ore : ores) {
                batch.draw(oreImage, ore.x, ore.y);
            }
            for (Rectangle crystal : crystals) {
                batch.draw(crystalImage, crystal.x, crystal.y);
            }
            font.setColor(Color.YELLOW);
            font.draw(batch, "" + crystalPickUpScore, Gdx.graphics.getWidth() - 50, Gdx.graphics.getHeight() - 20);
            font.setColor(Color.GREEN);
            font.draw(batch, "" + playerHP, 20, Gdx.graphics.getHeight() - 20);
        }
        batch.end();
    }

    /**
     * Release all the native resources.
     */
    @Override
    public void dispose() {
        crystalImage.dispose();
        oreImage.dispose();
        playerImage.dispose();
        pickupSound.dispose();
        batch.dispose();
        font.dispose();
    }

    private void spawnCrystal() {
        Rectangle crystal = new Rectangle();
        crystal.x = MathUtils.random(0, Gdx.graphics.getWidth() - crystalImage.getWidth());
        crystal.y = Gdx.graphics.getHeight();
        crystal.width = crystalImage.getWidth();
        crystal.height = crystalImage.getHeight();
        crystals.add(crystal);
        lastCrystalTime = TimeUtils.nanoTime();
    }

    private void spawnOre() {
        Rectangle ore = new Rectangle();
        ore.x = MathUtils.random(0, Gdx.graphics.getWidth() - crystalImage.getWidth());
        ore.y = Gdx.graphics.getHeight();
        ore.width = oreImage.getWidth();
        ore.height = oreImage.getHeight();
        ores.add(ore);
        lastOreTime = TimeUtils.nanoTime();
    }

    private void commandMoveLeft() {
        player.x -= SPEED * Gdx.graphics.getDeltaTime();
        if (player.x < 0) player.x = 0;
    }

    private void commandMoveRight() {
        player.x += SPEED * Gdx.graphics.getDeltaTime();
        if (player.x > Gdx.graphics.getWidth() - playerImage.getWidth())
            player.x = Gdx.graphics.getWidth() - playerImage.getWidth();
    }

    private void commandMoveLeftCorner() {
        player.x = 0;
    }

    private void commandMoveRightCorner() {
        player.x = Gdx.graphics.getWidth() - playerImage.getWidth();
    }

    private void commandTouched() {
        Vector3 touchPos = new Vector3();
        touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(touchPos); // transform the touch/mouse coordinates to our camera's coordinate system
        player.x = touchPos.x - playerImage.getWidth() / 2f;
    }

    private void commandExitGame() {
        Gdx.app.exit();
    }
}
