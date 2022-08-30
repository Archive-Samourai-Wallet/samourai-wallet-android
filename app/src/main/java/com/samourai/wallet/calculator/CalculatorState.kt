package com.samourai.wallet.calculator

import com.plcoding.calculatorprep.CalculatorOperation

data class CalculatorState(
    val number1: String = "",
    val number2: String = "",
    val operator: CalculatorOperation? = null,
    val result: String = ""
)