package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.ui.screens.AmountKeyboardState
import com.nonen.Bookkeeping.ui.screens.AmountOperator

class YimuNumberKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.yimu_number_keyboard, this, true)
    }

    private val keyOne = findViewById<TextView>(R.id.one)
    private val keyTwo = findViewById<TextView>(R.id.two)
    private val keyThree = findViewById<TextView>(R.id.three)
    private val keyFour = findViewById<TextView>(R.id.four)
    private val keyFive = findViewById<TextView>(R.id.five)
    private val keySix = findViewById<TextView>(R.id.six)
    private val keySeven = findViewById<TextView>(R.id.seven)
    private val keyEight = findViewById<TextView>(R.id.eight)
    private val keyNine = findViewById<TextView>(R.id.nine)
    private val keyZero = findViewById<TextView>(R.id.zero)
    private val keyPoint = findViewById<TextView>(R.id.point)
    private val keyAdd = findViewById<TextView>(R.id.add)
    private val keyReduce = findViewById<TextView>(R.id.reduce)
    private val keyRide = findViewById<TextView>(R.id.ride)
    private val keyExcept = findViewById<TextView>(R.id.except)
    private val keyDelete = findViewById<ImageView>(R.id.delete_one)
    private val keyAgain = findViewById<TextView>(R.id.add_bill_again)
    private val keyCancel = findViewById<TextView>(R.id.cancel)

    fun bind(
        state: AmountKeyboardState,
        repeatEnabled: Boolean,
        onChange: (AmountKeyboardState) -> Unit,
        onSave: (Boolean) -> Unit,
        onCancel: () -> Unit,
        onInvalidDivision: () -> Unit,
    ) {
        val digitKeys = mapOf(
            keyOne to '1', keyTwo to '2', keyThree to '3',
            keyFour to '4', keyFive to '5', keySix to '6',
            keySeven to '7', keyEight to '8', keyNine to '9', keyZero to '0',
        )
        digitKeys.forEach { (view, digit) ->
            view.setOnClickListener { onChange(state.digit(digit)) }
        }
        keyPoint.setOnClickListener { onChange(state.digit('.')) }
        keyDelete.setOnClickListener { onChange(state.backspace()) }
        keyDelete.setOnLongClickListener {
            onChange(AmountKeyboardState())
            true
        }
        bindOperator(keyAdd, AmountOperator.ADD, state, onChange, onInvalidDivision)
        bindOperator(keyReduce, AmountOperator.SUBTRACT, state, onChange, onInvalidDivision)
        bindOperator(keyRide, AmountOperator.MULTIPLY, state, onChange, onInvalidDivision)
        bindOperator(keyExcept, AmountOperator.DIVIDE, state, onChange, onInvalidDivision)

        keyAgain.text = if (repeatEnabled) {
            context.getString(R.string.yimu_again)
        } else {
            context.getString(R.string.yimu_save)
        }
        keyAgain.setOnClickListener {
            when {
                state.canSave -> onSave(repeatEnabled)
                state.hasZeroDivisor -> onInvalidDivision()
                state.canEvaluate -> onChange(state.evaluate())
            }
        }
        keyAgain.setOnLongClickListener {
            if (state.canSave) {
                onSave(false)
                true
            } else {
                false
            }
        }

        val active = state.operator != null || state.canSave
        keyCancel.text = state.actionLabel
        keyCancel.setBackgroundResource(if (active) R.drawable.shape_bg_asset_header else R.drawable.bg_grey)
        keyCancel.setTextColor(context.getColor(if (active) R.color.white else R.color.black))
        keyCancel.setOnClickListener {
            when {
                state.hasZeroDivisor -> onInvalidDivision()
                state.canEvaluate -> onChange(state.evaluate())
                state.canSave -> onSave(false)
                state.operator == null -> onCancel()
            }
        }
    }

    private fun bindOperator(
        view: TextView,
        operator: AmountOperator,
        state: AmountKeyboardState,
        onChange: (AmountKeyboardState) -> Unit,
        onInvalidDivision: () -> Unit,
    ) {
        view.setOnClickListener {
            if (state.hasZeroDivisor) onInvalidDivision() else onChange(state.operate(operator))
        }
    }
}
