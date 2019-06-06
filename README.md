# DecimalFormatSample
decimal format sample

Auto insert decimal separator to EditText with locale, maxFractionDigits, maxValue, BigDecimal callback.

Create DecimalFormatTextWatcher and DecimalKeyListener and set to EditText

DecimalKeyListener is backport for DigitsKeyListener SDK26.
after SDK 26, use DecimalFormatTextWatcher with DigitsKeyListener

```kotlin
val textWatcher = DecimalFormatTextWatcher.getInstance(
                      locale = Locale.getDefault(), maxFractionDigits = 8, 
                      maxValue = BigDecimal("1000"), 
                      onDecimalChanged = { bigDecimal ->
            
                      }
                  )
editText.addTextChangedListener(textWatcher)
editText.keyListener = DecimalKeyListener.getInstance(locale = Locale.getDefault())
```
