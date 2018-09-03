// License: https://github.com/kubode/diff-util-kotlin

package com.jemshit.sensorlogger.helper

import androidx.recyclerview.widget.DiffUtil

interface Diffable {
    fun isTheSame(other: Diffable): Boolean = equals(other)
    fun isContentsTheSame(other: Diffable): Boolean = equals(other)
}

private class Callback(val old: List<Diffable>, val new: List<Diffable>) : DiffUtil.Callback() {

    override fun getOldListSize() = old.size
    override fun getNewListSize() = new.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old[oldItemPosition].isTheSame(new[newItemPosition])
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old[oldItemPosition].isContentsTheSame(new[newItemPosition])
    }
}

/**
 * @see DiffUtil.calculateDiff
 */
fun calculateDiff(old: List<Diffable>, new: List<Diffable>, detectMoves: Boolean = false): DiffUtil.DiffResult {
    return DiffUtil.calculateDiff(Callback(old, new), detectMoves)
}