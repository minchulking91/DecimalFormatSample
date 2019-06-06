package io.github.minchulking91.decimalformatsample

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        localeButton.setOnClickListener {
            showLocaleSelectDialog()
        }
        updateLocale(Locale.getDefault())

    }

    private fun showLocaleSelectDialog() {
        val locales = Locale.getAvailableLocales()
        val titles = locales.map {
            it.displayName
        }.toTypedArray()
        AlertDialog.Builder(this).setItems(titles) { _, which ->
            updateLocale(locales[which])
        }.show()
    }

    private var oldDecimalValue = BigDecimal("1234567890.1234567890")
    private var oldTextWatcher: DecimalFormatTextWatcher? = null
    private fun updateLocale(locale: Locale) {
        localeButton.text = locale.displayName
        val decimalFormatSymbols = DecimalFormatSymbols.getInstance(locale)
        localeInfoTextView.text =
            StringBuilder("GroupingSeparator  ").append(decimalFormatSymbols.groupingSeparator).append('\n')
                .append("DecimalSeparator  ").append(decimalFormatSymbols.decimalSeparator).append('\n')
        //remove old textWatcher
        oldTextWatcher?.let {
            editText.removeTextChangedListener(it)
        }
        editText.text.clear()

        //new text watcher
        val decimalFormatter = DecimalFormat.getNumberInstance(locale).apply { maximumFractionDigits = 340 }
        oldTextWatcher = DecimalFormatTextWatcher.getInstance(locale = locale, onDecimalChanged = {
            textView.text = decimalFormatter.format(it)
            oldDecimalValue = it
        })
        editText.addTextChangedListener(oldTextWatcher)
        editText.keyListener = DecimalKeyListener.getInstance(locale = locale)
        editText.setText(decimalFormatter.format(oldDecimalValue))
    }
}
