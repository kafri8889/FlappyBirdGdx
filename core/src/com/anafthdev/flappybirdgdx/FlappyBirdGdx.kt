package com.anafthdev.flappybirdgdx

import com.anafthdev.flappybirdgdx.screen.GameScreen
import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.utils.ScreenUtils

class FlappyBirdGdx : Game() {

    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont().apply {
            region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            data.setScale(0.4f)
        }

        Box2D.init()

        setScreen(GameScreen(this))
    }

    override fun dispose() {
        batch.dispose()
    }
}
