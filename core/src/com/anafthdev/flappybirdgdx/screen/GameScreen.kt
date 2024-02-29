package com.anafthdev.flappybirdgdx.screen

import com.anafthdev.flappybirdgdx.Constant
import com.anafthdev.flappybirdgdx.FlappyBirdGdx
import com.anafthdev.flappybirdgdx.data.Trunk
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Timer
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameScreen(private val game: FlappyBirdGdx): Screen {

    private val MIN_TRUNK_OFFSET = -35 // -35px from center (center to up)
    private val MAX_TRUNK_OFFSET = 35 // 35px from center (center to bottom)
    private val TRUNK_INTERVAL = 2f // Show trunk every n second
    private val BACKGROUND_MOVE_SPEED_LAYER_4 = 60 // 60 px/sec
    private val BACKGROUND_MOVE_SPEED_LAYER_3 = 40 // 40 px/sec
    private val BACKGROUND_MOVE_SPEED_LAYER_2 = 20 // 20 px/sec

    private val bgLayer1: Texture = Texture("demon_woods_l1.png")

    private val bgLayer2: Texture = Texture("demon_woods_l2.png")
    private var xCoordBgLayer2_1: Float = 0f
    private var xCoordBgLayer2_2: Float = Constant.SCREEN_RESOLUTION_WIDTH

    private val bgLayer3: Texture = Texture("demon_woods_l3.png")
    private var xCoordBgLayer3_1: Float = 0f
    private var xCoordBgLayer3_2: Float = Constant.SCREEN_RESOLUTION_WIDTH

    private val bgLayer4: Texture = Texture("demon_woods_l4.png")
    private var xCoordBgLayer4_1: Float = 0f
    private var xCoordBgLayer4_2: Float = Constant.SCREEN_RESOLUTION_WIDTH

    private val birdUpImg: Texture = Texture("bird_up.png")
    private val birdDownImg: Texture = Texture("bird_down.png")

    private val trunkImg = Texture("trunk.png")

    private val camera: OrthographicCamera = OrthographicCamera().apply {
        setToOrtho(false, Constant.SCREEN_RESOLUTION_WIDTH, Constant.SCREEN_RESOLUTION_HEIGHT)
    }

    private val tapToStartGameGlyphLayout: GlyphLayout = GlyphLayout(
        game.font,
        "Tap  to start game!"
    )

    private val gravityForce = Vector2(0f, -Constant.GRAVITY_FORCE)

    private val box2dDebugRenderer = Box2DDebugRenderer()
    private val world: World = World(
        gravityForce,
        true
    ).apply {
        setContactFilter { fixtureA, fixtureB ->
            // Check collision

            if (fixtureA?.userData == null || fixtureB?.userData == null) return@setContactFilter false

            when {
                fixtureA.userData == "bird" && fixtureB.userData == "trunk" -> {
                    gameOver()
                    true
                }
                fixtureA.userData == "bird" && fixtureB.userData == "scoreHitBox" -> {
                    score++
                    false
                }
                else -> true
            }
        }
    }

    private val birdSize = 40f / 4f
    private val birdBody = createBirdBody()

    private val trunks = Array<Trunk>()

    private val timer = Timer.instance()
    private val timerTask = object : Timer.Task() {
        override fun run() {
            trunks.add(Trunk(world, trunkImg, Random.nextInt(MIN_TRUNK_OFFSET, MAX_TRUNK_OFFSET).toFloat()))
            // Show trunk every 1 second
            timer.scheduleTask(this, TRUNK_INTERVAL)
        }
    }

    private var accumulator = 0f
    private var highScore = 0
    private var score = 0

    /**
     * Last y coordinate of [birdBody]
     *
     * if last y greater than new y, use [birdDownImg] otherwise use [birdUpImg]
     */
    private var lastYCoord = 0f

    private var isGameStarted: Boolean = false

    override fun show() {

    }

    override fun render(delta: Float) {

        if (isGameStarted) doPhysicsStep(delta)

        with(game) {
            batch.projectionMatrix = camera.combined

            batch.begin()
            batch.renderBackground()
            for (trunk in trunks) trunk.draw(batch)
            batch.renderBird()

            if (!isGameStarted) {
                font.draw(
                    batch,
                    tapToStartGameGlyphLayout,
                    Constant.SCREEN_RESOLUTION_WIDTH / 2 - tapToStartGameGlyphLayout.width / 2,
                    Constant.SCREEN_RESOLUTION_HEIGHT / 2
                )
            }

            // Draw score text
            font.draw(
                batch,
                if (isGameStarted) score.toString() else highScore.toString(),
                Constant.SCREEN_RESOLUTION_WIDTH / 2,
                Constant.SCREEN_RESOLUTION_HEIGHT * 0.92f
            )

            batch.end()

//            box2dDebugRenderer.render(world, batch.projectionMatrix)
        }

        if (Gdx.input.justTouched() && isGameStarted) {
            // Jump
            birdBody.setLinearVelocity(0f, 50f)
        }

        if (Gdx.input.justTouched() && !isGameStarted) {
            startGame()
        }

        // Check if bird is out of bounds
        if (birdBody.position.y < 0 + birdSize / 2 || birdBody.position.y > Constant.SCREEN_RESOLUTION_HEIGHT - birdSize / 2) {
            gameOver()
        }

        // Move trunks and destroy if out of bounds
        val iter: MutableIterator<Trunk> = trunks.iterator()
        while (iter.hasNext()) {
            val trunk = iter.next()
            trunk.moveX(trunk.x - 20 * delta)

            if (trunk.x + trunk.trunkSize.width < 0) {
                trunk.destroy()
                iter.remove()
            }
        }

        lastYCoord = birdBody.position.y
    }

    override fun resize(width: Int, height: Int) {

    }

    override fun pause() {

    }

    override fun resume() {

    }

    override fun hide() {

    }

    override fun dispose() {
        bgLayer1.dispose()
        bgLayer2.dispose()
        bgLayer3.dispose()
        bgLayer4.dispose()
        world.dispose()
        box2dDebugRenderer.dispose()
    }

    // Render scrolling background
    private fun SpriteBatch.renderBackground() {

        // Misal screen width = 500 px
        // Berarti koordinat x yg ke-1 = 0, ke-2 = 500
        // Ketika koordinat ke-1 pindah, misal geser 10 px, berarti 0 - 10 = -10
        // Maka koordinat ke-2 juga geser sebanyak 10 px, 500 - 10 = 490

        xCoordBgLayer4_1 -= BACKGROUND_MOVE_SPEED_LAYER_4 * Gdx.graphics.deltaTime
        xCoordBgLayer4_2 = xCoordBgLayer4_1 + Constant.SCREEN_RESOLUTION_WIDTH

        xCoordBgLayer3_1 -= BACKGROUND_MOVE_SPEED_LAYER_3 * Gdx.graphics.deltaTime
        xCoordBgLayer3_2 = xCoordBgLayer3_1 + Constant.SCREEN_RESOLUTION_WIDTH

        xCoordBgLayer2_1 -= BACKGROUND_MOVE_SPEED_LAYER_2 * Gdx.graphics.deltaTime
        xCoordBgLayer2_2 = xCoordBgLayer2_1 + Constant.SCREEN_RESOLUTION_WIDTH

        // Jika background 4_1 sudah tidak terlihat di layar
        // reset ke koordinat semula
        if (xCoordBgLayer4_1 <= -Constant.SCREEN_RESOLUTION_WIDTH) {
            xCoordBgLayer4_1 = 0f
            xCoordBgLayer4_2 = Constant.SCREEN_RESOLUTION_WIDTH
        }

        // Jika background 3_1 sudah tidak terlihat di layar
        // reset ke koordinat semula
        if (xCoordBgLayer3_1 <= -Constant.SCREEN_RESOLUTION_WIDTH) {
            xCoordBgLayer3_1 = 0f
            xCoordBgLayer3_2 = Constant.SCREEN_RESOLUTION_WIDTH
        }

        // Jika background 2_1 sudah tidak terlihat di layar
        // reset ke koordinat semula
        if (xCoordBgLayer2_1 <= -Constant.SCREEN_RESOLUTION_WIDTH) {
            xCoordBgLayer2_1 = 0f
            xCoordBgLayer2_2 = Constant.SCREEN_RESOLUTION_WIDTH
        }

        draw(bgLayer1, 0f, 0f, Constant.SCREEN_RESOLUTION_WIDTH, Constant.SCREEN_RESOLUTION_HEIGHT)
        draw(bgLayer2, xCoordBgLayer2_1, 0f, Constant.SCREEN_RESOLUTION_WIDTH, Constant.SCREEN_RESOLUTION_HEIGHT)
        draw(bgLayer2, xCoordBgLayer2_2, 0f, Constant.SCREEN_RESOLUTION_WIDTH, Constant.SCREEN_RESOLUTION_HEIGHT)
        draw(bgLayer3, xCoordBgLayer3_1, 0f, Constant.SCREEN_RESOLUTION_WIDTH, Constant.SCREEN_RESOLUTION_HEIGHT)
        draw(bgLayer3, xCoordBgLayer3_2, 0f, Constant.SCREEN_RESOLUTION_WIDTH, Constant.SCREEN_RESOLUTION_HEIGHT)
        draw(bgLayer4, xCoordBgLayer4_1, 0f, Constant.SCREEN_RESOLUTION_WIDTH, Constant.SCREEN_RESOLUTION_HEIGHT)
        draw(bgLayer4, xCoordBgLayer4_2, 0f, Constant.SCREEN_RESOLUTION_WIDTH, Constant.SCREEN_RESOLUTION_HEIGHT)
    }

    private fun SpriteBatch.renderBird() {
        draw(
            if (lastYCoord >= birdBody.position.y) birdDownImg else birdUpImg,
            birdBody.position.x - birdSize / 2, // Center to body
            birdBody.position.y - birdSize / 2, // Center to body
            birdSize,
            birdSize
        )
    }

    private fun doPhysicsStep(deltaTime: Float) {
        // fixed time step
        // max frame time to avoid spiral of death (on slow devices)
        val frameTime = min(deltaTime.toDouble(), 0.25).toFloat()
        accumulator += frameTime
        while (accumulator >= 1f/60f) {
            world.step(
                1f/60f,
                8,
                3
            )
            accumulator -= 1f/60f
        }
    }

    private fun createBirdBody(): Body {
        return world.createBody(
            BodyDef().apply {
                type = BodyDef.BodyType.DynamicBody
                position.set(
                    Constant.SCREEN_RESOLUTION_WIDTH * 0.3f,
                    Constant.SCREEN_RESOLUTION_HEIGHT / 2f - birdSize / 2, // Center vertically
                )

                gravityScale = 14f
            }
        ).apply {
            val shape = PolygonShape().apply {
                setAsBox(birdSize / 2, birdSize / 2)
            }

            createFixture(
                shape,
                1.0f
            ).apply {
                userData = "bird"
            }

            shape.dispose()
        }
    }

    private fun startGame() {
        isGameStarted = true
        trunks.add(Trunk(world, trunkImg, Random.nextInt(MIN_TRUNK_OFFSET, MAX_TRUNK_OFFSET).toFloat()))
        timer.scheduleTask(timerTask, TRUNK_INTERVAL)

        birdBody.setTransform(
            Constant.SCREEN_RESOLUTION_WIDTH * 0.3f,
            Constant.SCREEN_RESOLUTION_HEIGHT / 2f - birdSize / 2, // Center vertically
            0f
        )
    }

    private fun gameOver() {
        // Lose

        highScore = max(highScore, score)
        score = 0

        timer.clear()
        // jump a bit
        birdBody.setLinearVelocity(0f, 20f)
        // Clear all trunks
        trunks.iterator().let {
            for (trunk in it) {
                trunk.destroy()
                it.remove()
            }
        }

        isGameStarted = false
    }

}