/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
 * Copyright (C) 2022 Xilin Jia https://github.com/XilinJia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gnucash.android.model

import android.util.Log
import com.crashlytics.android.Crashlytics
import org.gnucash.android.model.Commodity.Companion.getInstance
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * Money represents a money amount and a corresponding currency.
 * Money internally uses [BigDecimal] to represent the amounts, which enables it
 * to maintain high precision afforded by BigDecimal. Money objects are immutable and
 * most operations return new Money objects.
 * Money String constructors should not be passed any locale-formatted numbers. Only
 * [Locale.US] is supported e.g. "2.45" will be parsed as 2.45 meanwhile
 * "2,45" will be parsed to 245 although that could be a decimal in [Locale.GERMAN]
 *
 * @author Ngewi Fet<ngewif></ngewif>@gmail.com>
 * @author Xilin Jia <https://github.com/XilinJia> []Kotlin code created (Copyright (C) 2022)]
 */
class Money : Comparable<Money> {
    /**
     * Returns the commodity used by the Money
     * @return Instance of commodity
     */
    /**
     * Currency of the account
     */
    var mCommodity: Commodity? = null
        private set

    /**
     * Amount value held by this object
     */
    private var mAmount: BigDecimal? = null

    /**
     * Rounding mode to be applied when performing operations
     * Defaults to [RoundingMode.HALF_EVEN]
     */
    private var ROUNDING_MODE = RoundingMode.HALF_EVEN

    /**
     * Creates a new money amount
     * @param amount Value of the amount
     * @param commodity Commodity of the money
     */
    constructor(amount: BigDecimal?, commodity: Commodity?) {
        mCommodity = commodity
        setMAmount(amount!!) //commodity has to be set first. Because we use it's scale
    }

    /**
     * Overloaded constructor.
     * Accepts strings as arguments and parses them to create the Money object
     * @param amount Numerical value of the Money
     * @param currencyCode Currency code as specified by ISO 4217
     */
    constructor(amount: String?, currencyCode: String?) {
        //commodity has to be set first
        mCommodity = getInstance(currencyCode)
        setMAmount(BigDecimal(amount))
    }

    /**
     * Constructs a new money amount given the numerator and denominator of the amount.
     * The rounding mode used for the division is [BigDecimal.ROUND_HALF_EVEN]
     * @param numerator Numerator as integer
     * @param denominator Denominator as integer
     * @param currencyCode 3-character currency code string
     */
    constructor(numerator: Long, denominator: Long, currencyCode: String) {
        mAmount = getBigDecimal(numerator, denominator)
        setMCommodity(currencyCode)
    }

    /**
     * Copy constructor.
     * Creates a new Money object which is a clone of `money`
     * @param money Money instance to be cloned
     */
    constructor(money: Money) {
        setMCommodity(money.mCommodity!!)
        setMAmount(money.asBigDecimal())
    }

    /**
     * Returns a new `Money` object the currency specified by `currency`
     * and the same value as this one. No value exchange between the currencies is performed.
     * @param commodity [Commodity] to assign to new `Money` object
     * @return [Money] object with same value as current object, but with new `currency`
     */
    fun withCurrency(commodity: Commodity): Money {
        return Money(mAmount, commodity)
    }

    /**
     * Sets the commodity for the Money
     *
     * No currency conversion is performed
     * @param commodity Commodity instance
     */
    private fun setMCommodity(commodity: Commodity) {
        mCommodity = commodity
    }

    /**
     * Sets the commodity for the Money
     * @param currencyCode ISO 4217 currency code
     */
    private fun setMCommodity(currencyCode: String) {
        mCommodity = getInstance(currencyCode)
    }

    /**
     * Returns the GnuCash format numerator for this amount.
     *
     * Example: Given an amount 32.50$, the numerator will be 3250
     * @return GnuCash numerator for this amount
     */
    fun numerator(): Long {
        return try {
            mAmount!!.scaleByPowerOfTen(scale).longValueExact()
        } catch (e: ArithmeticException) {
            val msg = "Currency " + mCommodity!!.mMnemonic +
                    " with scale " + scale +
                    " has amount " + mAmount.toString()
            Crashlytics.log(msg)
            Log.e(javaClass.name, msg)
            throw e
        }
    }

    /**
     * Returns the GnuCash amount format denominator for this amount
     *
     * The denominator is 10 raised to the power of number of fractional digits in the currency
     * @return GnuCash format denominator
     */
    fun denominator(): Long {
        val scale = scale
        return BigDecimal.ONE.scaleByPowerOfTen(scale).longValueExact()
    }

    /**
     * Returns the scale (precision) used for the decimal places of this amount.
     *
     * The scale used depends on the commodity
     * @return Scale of amount as integer
     */
    private val scale: Int
        get() {
            var scale = mCommodity!!.smallestFractionDigits()
            if (scale < 0) {
                scale = mAmount!!.scale()
            }
            if (scale < 0) {
                scale = 0
            }
            return scale
        }

    /**
     * Returns the amount represented by this Money object
     *
     * The scale and rounding mode of the returned value are set to that of this Money object
     * @return [BigDecimal] valure of amount in object
     */
    fun asBigDecimal(): BigDecimal {
        return mAmount!!.setScale(mCommodity!!.smallestFractionDigits(), RoundingMode.HALF_EVEN)
    }

    /**
     * Returns the amount this object
     * @return Double value of the amount in the object
     */
    fun asDouble(): Double {
        return mAmount!!.toDouble()
    }

    /**
     * An alias for [.toPlainString]
     * @return Money formatted as a string (excludes the currency)
     */
    fun asString(): String {
        return toPlainString()
    }
    /**
     * Returns a string representation of the Money object formatted according to
     * the `locale` and includes the currency symbol.
     * The output precision is limited to the number of fractional digits supported by the currency
     * @param locale Locale to use when formatting the object
     * @return String containing formatted Money representation
     */
    /**
     * Equivalent to calling formattedString(Locale.getDefault())
     * @return String formatted Money representation in default locale
     */
    @JvmOverloads
    fun formattedString(locale: Locale = Locale.getDefault()): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(locale)
        //if we want to show US Dollars for locales which also use Dollars, for example, Canada
        val symbol: String = if (mCommodity!! == Commodity.USD && locale != Locale.US) {
            "US$"
        } else {
            mCommodity!!.symbol
        }
        val decimalFormatSymbols = (currencyFormat as DecimalFormat).decimalFormatSymbols
        decimalFormatSymbols.currencySymbol = symbol
        currencyFormat.decimalFormatSymbols = decimalFormatSymbols
        currencyFormat.setMinimumFractionDigits(mCommodity!!.smallestFractionDigits())
        currencyFormat.setMaximumFractionDigits(mCommodity!!.smallestFractionDigits())
        return currencyFormat.format(asDouble())
        /*
// 	old currency formatting code
		NumberFormat formatter = NumberFormat.getInstance(locale);
		formatter.setMinimumFractionDigits(mCommodity.getSmallestFractionDigits());
		formatter.setMaximumFractionDigits(mCommodity.getSmallestFractionDigits());
		Currency currency = Currency.getInstance(mCommodity.getMMnemonic());
		return formatter.format(asDouble()) + " " + currency.getSymbol(locale);
*/
    }

    /**
     * Returns a new Money object whose amount is the negated value of this object amount.
     * The original `Money` object remains unchanged.
     * @return Negated `Money` object
     */
    fun negate(): Money {
        return Money(mAmount!!.negate(), mCommodity)
    }

    /**
     * Sets the amount value of this `Money` object
     * @param amount [BigDecimal] amount to be set
     */
    private fun setMAmount(amount: BigDecimal) {
        mAmount = amount.setScale(mCommodity!!.smallestFractionDigits(), ROUNDING_MODE)
    }

    /**
     * Returns a new `Money` object whose value is the sum of the values of
     * this object and `addend`.
     *
     * @param addend Second operand in the addition.
     * @return Money object whose value is the sum of this object and `money`
     * @throws CurrencyMismatchException if the `Money` objects to be added have different Currencies
     */
    fun add(addend: Money): Money {
        if (mCommodity!! != addend.mCommodity) throw CurrencyMismatchException()
        val bigD = mAmount!!.add(addend.mAmount)
        return Money(bigD, mCommodity)
    }

    /**
     * Returns a new `Money` object whose value is the difference of the values of
     * this object and `subtrahend`.
     * This object is the minuend and the parameter is the subtrahend
     * @param subtrahend Second operand in the subtraction.
     * @return Money object whose value is the difference of this object and `subtrahend`
     * @throws CurrencyMismatchException if the `Money` objects to be added have different Currencies
     */
    fun subtract(subtrahend: Money): Money {
        if (mCommodity!! != subtrahend.mCommodity) throw CurrencyMismatchException()
        val bigD = mAmount!!.subtract(subtrahend.mAmount)
        return Money(bigD, mCommodity)
    }

    /**
     * Returns a new `Money` object whose value is the quotient of the values of
     * this object and `divisor`.
     * This object is the dividend and `divisor` is the divisor
     *
     * This method uses the rounding mode [BigDecimal.ROUND_HALF_EVEN]
     * @param divisor Second operand in the division.
     * @return Money object whose value is the quotient of this object and `divisor`
     * @throws CurrencyMismatchException if the `Money` objects to be added have different Currencies
     */
    fun divide(divisor: Money): Money {
        if (mCommodity!! != divisor.mCommodity) throw CurrencyMismatchException()
        val bigD = mAmount!!.divide(divisor.mAmount, mCommodity!!.smallestFractionDigits(), ROUNDING_MODE)
        return Money(bigD, mCommodity)
    }

    /**
     * Returns a new `Money` object whose value is the quotient of the division of this objects
     * value by the factor `divisor`
     * @param divisor Second operand in the addition.
     * @return Money object whose value is the quotient of this object and `divisor`
     */
    fun divide(divisor: Int): Money {
        val moneyDiv = Money(BigDecimal(divisor), mCommodity)
        return divide(moneyDiv)
    }

    /**
     * Returns a new `Money` object whose value is the product of the values of
     * this object and `money`.
     *
     * @param money Second operand in the multiplication.
     * @return Money object whose value is the product of this object and `money`
     * @throws CurrencyMismatchException if the `Money` objects to be added have different Currencies
     */
    fun multiply(money: Money): Money {
        if (mCommodity!! != money.mCommodity) throw CurrencyMismatchException()
        val bigD = mAmount!!.multiply(money.mAmount)
        return Money(bigD, mCommodity)
    }

    /**
     * Returns a new `Money` object whose value is the product of this object
     * and the factor `multiplier`
     *
     * The currency of the returned object is the same as the current object
     * @param multiplier Factor to multiply the amount by.
     * @return Money object whose value is the product of this objects values and `multiplier`
     */
    fun multiply(multiplier: Int): Money {
        val moneyFactor = Money(BigDecimal(multiplier), mCommodity)
        return multiply(moneyFactor)
    }

    /**
     * Returns a new `Money` object whose value is the product of this object
     * and the factor `multiplier`
     * @param multiplier Factor to multiply the amount by.
     * @return Money object whose value is the product of this objects values and `multiplier`
     */
    fun multiply(multiplier: BigDecimal): Money {
        return Money(mAmount!!.multiply(multiplier), mCommodity)
    }

    /**
     * Returns true if the amount held by this Money object is negative
     * @return `true` if the amount is negative, `false` otherwise.
     */
    val isNegative: Boolean
        get() = mAmount!!.compareTo(BigDecimal.ZERO) == -1

    /**
     * Returns the string representation of the amount (without currency) of the Money object.
     *
     *
     * This string is not locale-formatted. The decimal operator is a period (.)
     * For a locale-formatted version, see the method overload [.toLocaleString]
     * @return String representation of the amount (without currency) of the Money object
     */
    fun toPlainString(): String {
        return mAmount!!.setScale(mCommodity!!.smallestFractionDigits(), ROUNDING_MODE).toPlainString()
    }

    /**
     * Returns a locale-specific representation of the amount of the Money object (excluding the currency)
     *
     * @return String representation of the amount (without currency) of the Money object
     */
    fun toLocaleString(): String {
        return String.format(Locale.getDefault(), "%.2f", asDouble())
    }

    /**
     * Returns the string representation of the Money object (value + currency) formatted according
     * to the default locale
     * @return String representation of the amount formatted with default locale
     */
    override fun toString(): String {
        return formattedString(Locale.getDefault())
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + mAmount.hashCode()
        result = prime * result + mCommodity.hashCode()
        return result
    }

    /** //FIXME: equality failing for money objects
     * Two Money objects are only equal if their amount (value) and currencies are equal
     * @param other Object to compare with
     * @return `true` if the objects are equal, `false` otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val other = other as Money
        if (mAmount != other.mAmount) return false
        return mCommodity!! == other.mCommodity
    }

    override fun compareTo(other: Money): Int {
        if (mCommodity!! != other.mCommodity) throw CurrencyMismatchException()
        return mAmount!!.compareTo(other.mAmount)
    }

    /**
     * Returns a new instance of [Money] object with the absolute value of the current object
     * @return Money object with absolute value of this instance
     */
    fun abs(): Money {
        return Money(mAmount!!.abs(), mCommodity)
    }

    /**
     * Checks if the value of this amount is exactly equal to zero.
     * @return `true` if this money amount is zero, `false` otherwise
     */
    val isAmountZero: Boolean
        get() = mAmount!!.compareTo(BigDecimal.ZERO) == 0

    inner class CurrencyMismatchException : IllegalArgumentException() {
        override val message: String
            get() = "Cannot perform operation on Money instances with different currencies"
    }

    companion object {
        /**
         * Default currency code (according ISO 4217)
         * This is typically initialized to the currency of the device default locale,
         * otherwise US dollars are used
         */
		@JvmField
		var DEFAULT_CURRENCY_CODE = "USD"
        /**
         * Returns a Money instance initialized to the local currency and value 0
         * @return Money instance of value 0 in locale currency
         */
        /**
         * A zero instance with the currency of the default locale.
         * This can be used anywhere where a starting amount is required without having to create a new object
         */
		@JvmStatic
		var sDefaultZero: Money? = null
            get() {
                if (field == null) {
                    field = Money(BigDecimal.ZERO, Commodity.DEFAULT_COMMODITY)
                }
                return field
            }
            private set

        /**
         * Returns the [BigDecimal] from the `numerator` and `denominator`
         * @param numerator Number of the fraction
         * @param denominator Denominator of the fraction
         * @return BigDecimal representation of the number
         */
		@JvmStatic
		fun getBigDecimal(numerator: Long, denominator: Long): BigDecimal {
            var denominator = denominator
            if (numerator == 0L && denominator == 0L) {
                denominator = 1
            }
            val scale: Int = Integer.numberOfTrailingZeros(denominator.toInt())
            return BigDecimal(BigInteger.valueOf(numerator), scale)
        }

        /**
         * Creates a new Money instance with 0 amount and the `currencyCode`
         * @param currencyCode Currency to use for this money instance
         * @return Money object with value 0 and currency `currencyCode`
         */
		@JvmStatic
		fun createZeroInstance(currencyCode: String): Money {
            val commodity = getInstance(currencyCode)
            return Money(BigDecimal.ZERO, commodity)
        }
    }
}