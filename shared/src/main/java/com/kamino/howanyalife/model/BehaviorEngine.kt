package com.kamino.howanyalife.model

import kotlin.random.Random

/** Picks the cat's next action from its needs and the time of day. */
class BehaviorEngine(private val random: Random = Random.Default) {

    fun next(state: CatState, current: CatAction, hourOfDay: Int): CatAction {
        val night = isNight(hourOfDay)

        if (current == CatAction.SLEEP) {
            val wantsToWake = state.energy >= 95f || (!night && state.energy >= 60f && random.nextFloat() < 0.4f)
            return if (wantsToWake) CatAction.STRETCH else CatAction.SLEEP
        }

        if (state.isTired || (night && state.energy < 70f && random.nextFloat() < 0.5f)) {
            return CatAction.SLEEP
        }

        val weights = if (state.isHungry) {
            mapOf(CatAction.CRY to 5f, CatAction.IDLE to 2f, CatAction.WALK to 1f)
        } else {
            buildMap {
                put(CatAction.WALK, 4f)
                put(CatAction.IDLE, 3f)
                put(CatAction.STRETCH, 0.6f)
                put(CatAction.RUN, 0.5f + state.mood / 40f)
                put(CatAction.LAUGH, state.mood / 50f)
                if (state.mood < 30f) put(CatAction.CRY, 2f)
            }
        }
        return weightedPick(weights)
    }

    /** Random duration for [action], in milliseconds. */
    fun durationMillis(action: CatAction): Long {
        val seconds = action.minSeconds + random.nextFloat() * (action.maxSeconds - action.minSeconds)
        return (seconds * 1000).toLong()
    }

    private fun weightedPick(weights: Map<CatAction, Float>): CatAction {
        var roll = random.nextFloat() * weights.values.sum()
        for ((action, weight) in weights) {
            roll -= weight
            if (roll <= 0f) return action
        }
        return weights.keys.last()
    }

    companion object {
        fun isNight(hourOfDay: Int): Boolean = hourOfDay >= 22 || hourOfDay < 6
    }
}
