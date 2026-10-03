package com.kamino.howanyalife.model

/**
 * Everything the cat can do.
 *
 * [assetDir] is the folder under `assets/cat/` holding that action's PNG frames.
 * [speed] is how far the cat moves per second, as a fraction of the walkable floor width.
 */
enum class CatAction(
    val assetDir: String,
    val fps: Int,
    val loop: Boolean,
    val minSeconds: Float,
    val maxSeconds: Float,
    val speed: Float = 0f,
) {
    IDLE("idle", fps = 4, loop = true, minSeconds = 3f, maxSeconds = 6f),
    WALK("walk", fps = 8, loop = true, minSeconds = 4f, maxSeconds = 8f, speed = 0.12f),
    RUN("run", fps = 12, loop = true, minSeconds = 2f, maxSeconds = 4f, speed = 0.4f),
    SLEEP("sleep", fps = 2, loop = true, minSeconds = 15f, maxSeconds = 30f),
    STRETCH("stretch", fps = 6, loop = false, minSeconds = 2.5f, maxSeconds = 2.5f),
    LAUGH("laugh", fps = 8, loop = true, minSeconds = 2f, maxSeconds = 3f),
    CRY("cry", fps = 6, loop = true, minSeconds = 3f, maxSeconds = 5f),
    EAT("eat", fps = 6, loop = true, minSeconds = 4f, maxSeconds = 4f),
    ;

    val moves: Boolean get() = speed > 0f
}
