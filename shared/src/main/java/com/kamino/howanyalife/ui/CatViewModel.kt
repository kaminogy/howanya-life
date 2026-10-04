package com.kamino.howanyalife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamino.howanyalife.data.CatRepository
import com.kamino.howanyalife.data.catTime
import com.kamino.howanyalife.model.BehaviorEngine
import com.kamino.howanyalife.model.CatAction
import com.kamino.howanyalife.model.CatState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

data class CatUiState(
    val loaded: Boolean = false,
    val stats: CatState = CatState(),
    val action: CatAction = CatAction.IDLE,
    /** Horizontal position on the floor, -1 (left edge) .. 1 (right edge). */
    val x: Float = 0f,
    val facingRight: Boolean = true,
    /** Position of the food bowl on the floor, or null when no bowl is out. */
    val bowlX: Float? = null,
    val bowlFull: Boolean = false,
    val night: Boolean = false,
)

class CatViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CatRepository(app)
    private val spriteStore = SpriteStore(app)
    private val engine = BehaviorEngine()

    private val _ui = MutableStateFlow(CatUiState())
    val ui: StateFlow<CatUiState> = _ui.asStateFlow()

    private val _sprites = MutableStateFlow(CatSprites())
    val sprites: StateFlow<CatSprites> = _sprites.asStateFlow()

    private var session: Job? = null
    private var behaviorJob: Job? = null
    private var lastTickMillis = 0L
    private var pendingSave: Job? = null

    init {
        viewModelScope.launch {
            _sprites.value = withContext(Dispatchers.IO) { spriteStore.loadAll() }
        }
    }

    /** Starts the simulation; call when the screen becomes visible. */
    fun onResume() {
        if (session?.isActive == true) return
        session = viewModelScope.launch {
            restore()
            launch { needsLoop() }
            startBehavior { behaviorLoop() }
        }
    }

    /** Stops animation work and saves; call when the screen is hidden. */
    fun onPause() {
        session?.cancel()
        behaviorJob?.cancel()
        session = null
        if (!_ui.value.loaded) return
        applyElapsedTime()
        val stats = _ui.value.stats
        pendingSave = viewModelScope.launch { repository.save(stats, System.currentTimeMillis()) }
    }

    /** Waits for the save started by [onPause], e.g. before refreshing the home-screen widget. */
    suspend fun awaitSaved() {
        pendingSave?.join()
    }

    fun feed() {
        val ui = _ui.value
        if (!ui.loaded || session?.isActive != true || ui.bowlX != null) return
        startBehavior {
            feedingScript()
            behaviorLoop()
        }
    }

    fun pet() {
        val ui = _ui.value
        if (!ui.loaded || session?.isActive != true) return
        if (ui.bowlX != null || ui.action == CatAction.SLEEP || ui.action == CatAction.LAUGH) return
        startBehavior {
            _ui.update { it.copy(stats = it.stats.petted()) }
            perform(CatAction.LAUGH, engine.durationMillis(CatAction.LAUGH))
            behaviorLoop()
        }
    }

    private fun startBehavior(block: suspend () -> Unit) {
        behaviorJob?.cancel()
        behaviorJob = viewModelScope.launch { block() }
    }

    /**
     * Reloads the saved state every time the screen comes back, since something else
     * (the phone's home-screen widget) may have fed the cat in the meantime.
     */
    private suspend fun restore() {
        pendingSave?.join()
        val now = System.currentTimeMillis()
        val stats = repository.loadCurrent(now)
            ?: _ui.value.stats.takeIf { _ui.value.loaded }
            ?: CatState()
        _ui.update {
            it.copy(
                loaded = true,
                stats = stats,
                action = when {
                    stats.asleep -> CatAction.SLEEP
                    it.action == CatAction.SLEEP -> CatAction.IDLE
                    else -> it.action
                },
                // A feeding interrupted by leaving the app is abandoned.
                bowlX = null,
                bowlFull = false,
                night = BehaviorEngine.isNight(hourOfDay()),
            )
        }
        lastTickMillis = now
    }

    private suspend fun needsLoop() {
        var ticks = 0
        while (true) {
            delay(NEEDS_TICK_MILLIS)
            applyElapsedTime()
            if (++ticks % AUTOSAVE_EVERY_TICKS == 0) {
                repository.save(_ui.value.stats, System.currentTimeMillis())
            }
        }
    }

    private fun applyElapsedTime() {
        val now = System.currentTimeMillis()
        val elapsed = catTime(now - lastTickMillis)
        lastTickMillis = now
        _ui.update {
            it.copy(stats = it.stats.advance(elapsed), night = BehaviorEngine.isNight(hourOfDay()))
        }
    }

    private suspend fun behaviorLoop() {
        while (true) {
            val ui = _ui.value
            val next = engine.next(ui.stats, ui.action, hourOfDay())
            perform(next, engine.durationMillis(next))
        }
    }

    private suspend fun feedingScript() {
        val catX = _ui.value.x
        val bowlX = if (catX < 0f) BOWL_POSITION else -BOWL_POSITION
        _ui.update { it.copy(bowlX = bowlX, bowlFull = true) }

        if (_ui.value.action == CatAction.SLEEP) {
            perform(CatAction.STRETCH, engine.durationMillis(CatAction.STRETCH))
        }
        // Stop just beside the bowl, on the side the cat is coming from.
        val side = sign(_ui.value.x - bowlX).takeIf { it != 0f } ?: -1f
        walkTo(bowlX + side * EATING_DISTANCE)
        _ui.update { it.copy(facingRight = bowlX > it.x) }

        perform(CatAction.EAT, engine.durationMillis(CatAction.EAT))
        _ui.update { it.copy(stats = it.stats.fed(), bowlFull = false) }
        perform(CatAction.LAUGH, engine.durationMillis(CatAction.LAUGH))
        _ui.update { it.copy(bowlX = null) }
    }

    private suspend fun walkTo(target: Float) {
        setAction(CatAction.WALK)
        while (abs(_ui.value.x - target) > 0.01f) {
            delay(FRAME_MILLIS)
            moveToward(target, CatAction.WALK.speed)
        }
    }

    /** Plays [action] for [durationMillis], wandering around the floor if it is a moving action. */
    private suspend fun perform(action: CatAction, durationMillis: Long) {
        setAction(action)
        if (!action.moves) {
            delay(durationMillis)
            return
        }
        var target = randomTarget()
        var remaining = durationMillis
        while (remaining > 0) {
            delay(FRAME_MILLIS)
            remaining -= FRAME_MILLIS
            if (moveToward(target, action.speed)) target = randomTarget()
        }
    }

    private fun setAction(action: CatAction) {
        _ui.update { it.copy(action = action, stats = it.stats.copy(asleep = action == CatAction.SLEEP)) }
    }

    /** Moves one frame toward [target]; returns true once it is reached. */
    private fun moveToward(target: Float, speed: Float): Boolean {
        // The floor spans -1..1, i.e. a width of 2.
        val step = speed * 2f * FRAME_MILLIS / 1000f
        var reached = false
        _ui.update {
            val dx = target - it.x
            reached = abs(dx) <= step
            it.copy(
                x = if (reached) target else it.x + step * sign(dx),
                facingRight = if (dx != 0f) dx > 0f else it.facingRight,
            )
        }
        return reached
    }

    private fun randomTarget(): Float {
        val current = _ui.value.x
        // Pick a spot at least a little way off so the cat visibly travels.
        var target: Float
        do {
            target = Random.nextFloat() * 2f - 1f
        } while (abs(target - current) < 0.3f)
        return target
    }


    private fun hourOfDay(): Int = LocalTime.now().hour

    private companion object {
        const val FRAME_MILLIS = 33L
        const val NEEDS_TICK_MILLIS = 1_000L
        const val AUTOSAVE_EVERY_TICKS = 15
        const val BOWL_POSITION = 0.65f
        const val EATING_DISTANCE = 0.35f
    }
}
