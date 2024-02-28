package com.anafthdev.flappybirdgdx.data

import com.anafthdev.flappybirdgdx.Constant
import com.anafthdev.flappybirdgdx.data.model.Size
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World

class Trunk(
    val world: World,
    val texture: Texture,
    offsetY: Float = 0f,
    x: Float = Constant.SCREEN_RESOLUTION_WIDTH,
    y: Float = Constant.SCREEN_RESOLUTION_HEIGHT,
) {

    val size = Size(5f, 45f)

    var mx = x
    val x: Float
        get() = mx

//    private val trunkUp = Vector2(x, y - size.height + (size.height / 4))
//    private val trunkBottom = Vector2(x, 0f - size.height - (size.height / 4))

    // Center the drawing x by subtracting with "size.width / 2"
    // Don't do this in body. Add x value with "size.width / 2"
    private val trunkUp = Vector2(x - size.width / 2, y - size.height + (size.height / 4) + offsetY)
    private val trunkBottom = Vector2(x - size.width / 2, 0f - size.height - (size.height / 4) + offsetY)

    private val trunkUpBody = createTrunkBody(
        trunkUp.cpy().apply {
            this.x += size.width / 2
        }
    )
    private val trunkBottomBody = createTrunkBody(
        trunkBottom.cpy().apply {
            this.x += size.width / 2
        }
    )

    private fun createTrunkBody(pos: Vector2): Body {
        return world.createBody(
            BodyDef().apply {
                type = BodyDef.BodyType.StaticBody
                awake = false
                position.set(pos)
            }
        ).apply {
            val shape = PolygonShape().apply {
                setAsBox(
                    size.width / 2,
                    size.height,
                    Vector2(0f, size.height), // Center this hit box
                    0f
                )
            }

            createFixture(
                shape,
                1.0f
            ).apply {
                userData = "trunk"
            }

            shape.dispose()
        }
    }

    fun draw(batch: SpriteBatch) {
        // Up
        batch.draw(
            texture,
            trunkUp.x,
            trunkUp.y,
            size.width,
            size.height * 2
        )

        // bottom
        batch.draw(
            texture,
            trunkBottom.x,
            trunkBottom.y,
            size.width,
            size.height * 2
        )
    }

    fun moveX(x: Float) {
        mx = x
        trunkUp.x = x - size.width / 2  // Need this to center draw to body
        trunkBottom.x = x - size.width / 2  // Need this to center draw to body
        trunkUpBody.setTransform(x, trunkUpBody.position.y, trunkUpBody.angle)
        trunkBottomBody.setTransform(x, trunkBottomBody.position.y, trunkBottomBody.angle)
    }

    fun destroy() {
        world.destroyBody(trunkUpBody)
        world.destroyBody(trunkBottomBody)
    }

}