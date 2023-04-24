package at.petrak.hexcasting.common.casting.operators.stack

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.getPositiveInt
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.nbt.CompoundTag

// "lehmer code"
object OpAlwinfyHasAscendedToABeingOfPureMath : Action {
    override fun operate(
        env: CastingEnvironment,
        stack: MutableList<Iota>,
        userData: CompoundTag,
        continuation: SpellContinuation
    ): OperationResult {
        if (stack.isEmpty())
            throw MishapNotEnoughArgs(1, 0)

        val code = stack.getPositiveInt(stack.lastIndex)
        stack.removeLast()

        val strides = IntArrayList()
        for (f in FactorialIter()) {
            if (f <= code)
                strides.add(f)
            else
                break
        }

        if (strides.size > stack.size)
            throw MishapNotEnoughArgs(strides.size + 1, stack.size + 1)
        var editTarget = stack.subList(stack.size - strides.size, stack.size)
        val swap = editTarget.toMutableList()
        var radix = code
        for (divisor in strides.asReversed()) {
            val index = radix / divisor
            radix %= divisor
            editTarget[0] = swap.removeAt(index)
            // i hope this isn't O(n)
            editTarget = editTarget.subList(1, editTarget.size)
        }

        return OperationResult(
            stack,
            userData,
            listOf(),
            continuation,
            1
        )
    }

    private class FactorialIter : Iterator<Int> {
        var acc = 1
        var n = 1
        override fun hasNext(): Boolean = true

        override fun next(): Int {
            val out = this.acc
            this.acc *= this.n
            this.n++
            return out
        }
    }
}
