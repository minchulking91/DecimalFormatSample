package io.github.minchulking91.decimalformatsample


import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.NumberKeyListener
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * copy from [android.text.method.DigitsKeyListener]
 */
class DecimalKeyListener private constructor(val locale: Locale, val decimal: Boolean) :
    NumberKeyListener() {

    private val accepted: CharArray

    private val decimalPointChars: Char
    private val groupPointChars: Char

    private val needsAdvancedInput: Boolean

    override fun getAcceptedChars(): CharArray {
        return accepted
    }

    private fun isDecimalPointChar(c: Char): Boolean {
        return decimalPointChars == c
    }


    /**
     * Returns the input type for the listener.
     * copy from [android.text.method.DigitsKeyListener.getInputType]
     */
    override fun getInputType(): Int {
        return if (needsAdvancedInput) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        } else {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
    }

    /**
     * copy from [android.text.method.DigitsKeyListener.filter]
     */
    override fun filter(
        source: CharSequence, start: Int, end: Int,
        dest: Spanned, dstart: Int, dend: Int
    ): CharSequence? {
        var tempSource = source
        var tempStart = start
        var tempEnd = end
        val out = super.filter(tempSource, tempStart, tempEnd, dest, dstart, dend)

        if (!decimal) {
            return out
        }

        if (out != null) {
            tempSource = out
            tempStart = 0
            tempEnd = out.length
        }

        var decimal = -1
        val dLen = dest.length

        /*
         * Find out if the existing text has a sign or decimal point characters.
         */

        for (i in 0 until dstart) {
            val c = dest[i]

            if (isDecimalPointChar(c)) {
                decimal = i
            }
        }
        for (i in dend until dLen) {
            val c = dest[i]

            if (isDecimalPointChar(c)) {
                decimal = i
            }
        }

        /*
         * If it does, we must strip them out from the source.
         * In addition, a sign character must be the very first character,
         * and nothing can be inserted before an existing sign character.
         * Go in reverse order so the offsets are stable.
         */

        var stripped: SpannableStringBuilder? = null

        for (i in tempEnd - 1 downTo tempStart) {
            val c = tempSource[i]
            var strip = false

            if (isDecimalPointChar(c)) {
                if (decimal >= 0) {
                    strip = true
                } else {
                    decimal = i
                }
            }

            if (strip) {
                if (tempEnd == tempStart + 1) {
                    return ""  // Only one character, and it was stripped.
                }

                if (stripped == null) {
                    stripped = SpannableStringBuilder(tempSource, tempStart, tempEnd)
                }

                stripped.delete(i - tempStart, i + 1 - tempStart)
            }
        }

        return stripped ?: out
    }

    init {
        val symbols = DecimalFormatSymbols(locale)
        decimalPointChars = symbols.decimalSeparator
        groupPointChars = symbols.groupingSeparator
        needsAdvancedInput = decimalPointChars != DEFAULT_DECIMAL_SEPARATOR
        accepted = if (decimal) {
            val separatorChar = symbols.decimalSeparator
            charArrayOf(*DIGITS, separatorChar, groupPointChars)
        } else {
            charArrayOf(*DIGITS, groupPointChars)
        }
    }

    companion object {
        private val DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        private const val DEFAULT_DECIMAL_SEPARATOR = '.'

        fun getInstance(locale: Locale = Locale.getDefault(), decimal: Boolean = true): DecimalKeyListener {
            return DecimalKeyListener(locale, decimal)
        }
    }

}
