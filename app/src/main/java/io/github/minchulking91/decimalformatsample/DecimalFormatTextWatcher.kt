package io.github.minchulking91.decimalformatsample

import android.text.Editable
import android.text.TextWatcher
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * [maxFractionDigits] -1 is no limit
 */
class DecimalFormatTextWatcher private constructor(locale: Locale) : TextWatcher {
    private val groupingSeparator: Char
    private val decimalSeparator: Char
    private val decimalSeparatorChars: CharArray
    private val groupSize: Int

    var maxFractionDigits: Int = -1
    var onDecimalChanged: ((BigDecimal) -> Unit)? = null

    private var maxIntPart: String? = null
    private var maxFractionPart: String? = null

    private var busy: Boolean = false


    override fun afterTextChanged(s: Editable?) {
        if (busy) return

        if (s.isNullOrBlank()) {
            onDecimalChanged?.invoke(BigDecimal.ZERO)
            return
        }

        busy = true

        val (groupedDecimal, decimal) = stringToDecimal(
            s.toString(),
            maxIntPart,
            maxFractionPart,
            maxFractionDigits,
            onDecimalChanged != null
        )
        s.replace(0, s.length, groupedDecimal)
        decimal?.let {
            onDecimalChanged?.invoke(it)
        }
        busy = false
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        //no-op
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        //no-op
    }

    private fun stringToDecimal(
        decimalString: String,
        maxIntPart: CharSequence?,
        maxFractionPart: CharSequence?,
        maxFractionDigits: Int,
        parseBigDecimal: Boolean
    ): Pair<CharSequence, BigDecimal?> {
        var intPart: CharSequence
        var fractionPart: CharSequence
        var hasDecimalSeparator: Boolean
        if (decimalString.contains(decimalSeparator)) {
            decimalString.split(decimalSeparator).take(2).let { array ->
                intPart = array[0]
                fractionPart = array[1]
            }
            hasDecimalSeparator = true
        } else {
            intPart = decimalString
            fractionPart = ""
            hasDecimalSeparator = false
        }
        //정수부는 group separator 이 있기 때문에 제거함.
        intPart = intPart.replace(numberFilter, "").let {
            naturalNumberRegex.find(it)?.value ?: "0"
        }
        fractionPart = fractionNumberRegex.find(fractionPart)?.value ?: ""

        if (maxIntPart != null && maxFractionPart != null && isBiggerThenMax(
                intPart,
                fractionPart,
                maxIntPart,
                maxFractionPart
            )
        ) {
            intPart = maxIntPart
            fractionPart = maxFractionPart
            hasDecimalSeparator = maxFractionPart.isNotEmpty()
        }

        if (maxFractionDigits == 0) {
            //정수만 허용
            hasDecimalSeparator = false
            fractionPart = ""
        } else if (maxFractionDigits != -1 && fractionPart.isNotBlank() && fractionPart.length > maxFractionDigits) {
            //소수부 제한이 있을 경우 소수부 절삭.
            fractionPart = fractionPart.subSequence(0, fractionPart.length)
        }

        val formattedDecimalString = getFormattedDecimalString(intPart, fractionPart, hasDecimalSeparator)
        val bigDecimal = if (parseBigDecimal) getBigDecimal(intPart, fractionPart) else null
        return formattedDecimalString to bigDecimal

    }

    private fun getBigDecimal(intPart: CharSequence, fractionPart: CharSequence): BigDecimal {
        val plainString = StringBuilder(intPart).append(fractionPart).toString()
        return BigDecimal(plainString).movePointLeft(fractionPart.length)
    }

    /**
     * @param intPart 정수부
     * @param fractionPart 소수부 (fractionDigits 적용되어 있음)
     * @param hasDecimalSeparator 소수점 여부 (10.) 입력을 허용하기 위함.
     * @return decimalFormat 이 적용된 CharSequence
     * ex)  intPart: 1234567890
     *      fractionPart: 1234567890
     *      return: 1,234,567,890.1234567890
     */
    private fun getFormattedDecimalString(
        intPart: CharSequence,
        fractionPart: CharSequence,
        hasDecimalSeparator: Boolean
    ): CharSequence {
        val formattedDecimalBuilder = StringBuilder()
        /**
         * int part
         *
         * 정수 부를 뒤집고 뒤에서부터 3자리 마다 ,를 찍은 후 다시 뒤집음.
         * index    012 345 6
         * reversed 432,112,3
         */
        intPart.reversed().forEachIndexed { index, c ->
            if (index.rem(groupSize) == 0 && index > 0) {
                formattedDecimalBuilder.append(groupingSeparator)
            }
            formattedDecimalBuilder.append(c)
        }
        formattedDecimalBuilder.reverse()

        /**
         * fraction part
         */
        if (hasDecimalSeparator) {
            formattedDecimalBuilder.append(decimalSeparator)
            formattedDecimalBuilder.append(fractionPart)
        }
        return formattedDecimalBuilder
    }

    /**
     * max 값 과 비교.
     *
     * - 정수부 비교.
     *      - 정수부 길이 비교
     *      - 정수부 각 숫자 비교.
     * - 소수부 비교.
     */
    private fun isBiggerThenMax(
        inputIntPart: CharSequence,
        inputFractionPart: CharSequence,
        maxIntPart: CharSequence,
        maxFractionPart: CharSequence
    ): Boolean {
        //정수부 길이 비교

        val intLength = inputIntPart.length
        val maxIntLength = maxIntPart.length
        if (intLength > maxIntLength) {
            return true
        }
        if (intLength < maxIntLength) {
            return false
        }
        //정수부 각 숫자 비교.
        for (i in 0 until intLength) {
            if (inputIntPart[i] > maxIntPart[i]) {
                return true
            } else if (inputIntPart[i] < maxIntPart[i]) {
                return false
            }
        }
        //소수부 비교.
        val fractionLength = inputFractionPart.length
        val maxFractionLength = maxFractionPart.length
        val compareLength = Math.min(fractionLength, maxFractionLength)
        for (i in 0 until compareLength) {
            if (inputFractionPart[i] > maxFractionPart[i]) {
                return true
            } else if (inputFractionPart[i] < maxFractionPart[i]) {
                return false
            }
        }
        return fractionLength > maxFractionLength
    }

    fun setMaxDecimal(max: BigDecimal?) {
        if (max == null) {
            this.maxIntPart = null
            this.maxFractionPart = null
        } else {
            val (intString, fractionString) = max.stripTrailingZeros().let {
                val intPart = it.toInt().toString()
                val fractionPart = it.remainder(BigDecimal.ONE).let { fractionPart ->
                    fractionPart.movePointRight(fractionPart.scale())
                }

                val fractionString = if (fractionPart == BigDecimal.ZERO) {
                    ""
                } else {
                    fractionPart.toPlainString()
                }

                intPart to fractionString
            }
            this.maxIntPart = intString
            this.maxFractionPart = fractionString
        }
    }

    init {
        val decimalFormatSymbols = DecimalFormatSymbols.getInstance(locale)
        groupingSeparator = decimalFormatSymbols.groupingSeparator
        decimalSeparator = decimalFormatSymbols.decimalSeparator
        decimalSeparatorChars = charArrayOf(decimalSeparator)
        groupSize = DEFAULT_GROUP_SIZE
    }

    companion object {
        private const val DEFAULT_GROUP_SIZE = 3
        private val naturalNumberRegex = Regex("""([1-9]\d*|0)$""")
        private val fractionNumberRegex = Regex("""\d*""")
        private val numberFilter = Regex("""[^\d]""")

        fun getInstance(
            locale: Locale = Locale.getDefault(),
            maxFractionDigits: Int = -1,
            maxValue: BigDecimal? = null,
            onDecimalChanged: ((BigDecimal) -> Unit)? = null
        ): DecimalFormatTextWatcher {
            val textWatcher = DecimalFormatTextWatcher(locale)
            textWatcher.maxFractionDigits = maxFractionDigits
            textWatcher.setMaxDecimal(maxValue)
            textWatcher.onDecimalChanged = onDecimalChanged
            return textWatcher
        }

    }
}