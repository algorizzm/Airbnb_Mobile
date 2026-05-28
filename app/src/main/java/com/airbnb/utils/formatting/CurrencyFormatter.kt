package com.airbnb.utils.formatting

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * Centralized currency formatting utility for consistent price display across the app.
 * 
 * Responsibilities:
 * - Standardize Philippine Peso (₱) formatting
 * - Handle decimal cleanup (remove unnecessary decimals)
 * - Format totals, nightly prices, and subtotals
 * - Format prices with thousands separators
 * 
 * All methods are stateless and thread-safe.
 */
object CurrencyFormatter {
    
    // Currency symbol
    private const val PESO_SYMBOL = "₱"
    
    // Number formatter with thousands separator
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    
    // Decimal formatter for prices with cents
    private val decimalFormat = DecimalFormat("#,##0.00")
    
    /**
     * Formats a price amount with peso symbol.
     * 
     * Examples:
     * - formatPrice(1200.0) -> "₱1,200"
     * - formatPrice(4500.5) -> "₱4,500"
     * - formatPrice(12450.0) -> "₱12,450"
     * 
     * @param amount Price amount
     * @return Formatted price string with peso symbol
     */
    fun formatPrice(amount: Double): String {
        return "$PESO_SYMBOL${numberFormat.format(amount.toInt())}"
    }
    
    /**
     * Formats a price amount with peso symbol (from Int).
     * 
     * @param amount Price amount
     * @return Formatted price string with peso symbol
     */
    fun formatPrice(amount: Int): String {
        return "$PESO_SYMBOL${numberFormat.format(amount)}"
    }
    
    /**
     * Formats a nightly price with "/ night" suffix.
     * 
     * Examples:
     * - formatNightlyPrice(1200.0) -> "₱1,200 / night"
     * - formatNightlyPrice(4500.0) -> "₱4,500 / night"
     * 
     * @param amount Nightly price amount
     * @return Formatted nightly price string
     */
    fun formatNightlyPrice(amount: Double): String {
        return "${formatPrice(amount)} / night"
    }
    
    /**
     * Formats a nightly price with "/ night" suffix (from Int).
     * 
     * @param amount Nightly price amount
     * @return Formatted nightly price string
     */
    fun formatNightlyPrice(amount: Int): String {
        return "${formatPrice(amount)} / night"
    }
    
    /**
     * Formats a total price with "total" suffix.
     * 
     * Examples:
     * - formatTotal(12450.0) -> "₱12,450 total"
     * - formatTotal(6000.0) -> "₱6,000 total"
     * 
     * @param amount Total price amount
     * @return Formatted total price string
     */
    fun formatTotal(amount: Double): String {
        return "${formatPrice(amount)} total"
    }
    
    /**
     * Formats a total price with "total" suffix (from Int).
     * 
     * @param amount Total price amount
     * @return Formatted total price string
     */
    fun formatTotal(amount: Int): String {
        return "${formatPrice(amount)} total"
    }
    
    /**
     * Formats a price with decimals (for precise calculations).
     * 
     * Examples:
     * - formatPriceWithDecimals(1200.50) -> "₱1,200.50"
     * - formatPriceWithDecimals(4500.00) -> "₱4,500.00"
     * 
     * @param amount Price amount with decimals
     * @return Formatted price string with decimals
     */
    fun formatPriceWithDecimals(amount: Double): String {
        return "$PESO_SYMBOL${decimalFormat.format(amount)}"
    }
    
    /**
     * Formats a price breakdown line item.
     * 
     * Examples:
     * - formatBreakdownItem("Subtotal", 12000.0) -> "Subtotal: ₱12,000"
     * - formatBreakdownItem("Service Fee", 450.0) -> "Service Fee: ₱450"
     * 
     * @param label Line item label
     * @param amount Line item amount
     * @return Formatted breakdown line
     */
    fun formatBreakdownItem(label: String, amount: Double): String {
        return "$label: ${formatPrice(amount)}"
    }
    
    /**
     * Formats a price per unit (e.g., per night, per guest).
     * 
     * Examples:
     * - formatPricePerUnit(1200.0, "night") -> "₱1,200 / night"
     * - formatPricePerUnit(500.0, "guest") -> "₱500 / guest"
     * 
     * @param amount Price amount
     * @param unit Unit label (e.g., "night", "guest", "hour")
     * @return Formatted price per unit string
     */
    fun formatPricePerUnit(amount: Double, unit: String): String {
        return "${formatPrice(amount)} / $unit"
    }
    
    /**
     * Formats a price per unit (from Int).
     * 
     * @param amount Price amount
     * @param unit Unit label
     * @return Formatted price per unit string
     */
    fun formatPricePerUnit(amount: Int, unit: String): String {
        return "${formatPrice(amount)} / $unit"
    }
    
    /**
     * Formats a price range.
     * 
     * Examples:
     * - formatPriceRange(1000.0, 5000.0) -> "₱1,000 - ₱5,000"
     * - formatPriceRange(500.0, 1500.0) -> "₱500 - ₱1,500"
     * 
     * @param minAmount Minimum price
     * @param maxAmount Maximum price
     * @return Formatted price range string
     */
    fun formatPriceRange(minAmount: Double, maxAmount: Double): String {
        return "${formatPrice(minAmount)} - ${formatPrice(maxAmount)}"
    }
    
    /**
     * Formats a price range (from Int).
     * 
     * @param minAmount Minimum price
     * @param maxAmount Maximum price
     * @return Formatted price range string
     */
    fun formatPriceRange(minAmount: Int, maxAmount: Int): String {
        return "${formatPrice(minAmount)} - ${formatPrice(maxAmount)}"
    }
    
    /**
     * Formats a discount or savings amount.
     * 
     * Examples:
     * - formatDiscount(500.0) -> "-₱500"
     * - formatDiscount(1200.0) -> "-₱1,200"
     * 
     * @param amount Discount amount
     * @return Formatted discount string with negative sign
     */
    fun formatDiscount(amount: Double): String {
        return "-${formatPrice(amount)}"
    }
    
    /**
     * Formats a discount or savings amount (from Int).
     * 
     * @param amount Discount amount
     * @return Formatted discount string with negative sign
     */
    fun formatDiscount(amount: Int): String {
        return "-${formatPrice(amount)}"
    }
    
    /**
     * Formats a percentage.
     * 
     * Examples:
     * - formatPercentage(15.0) -> "15%"
     * - formatPercentage(7.5) -> "7.5%"
     * 
     * @param percentage Percentage value
     * @return Formatted percentage string
     */
    fun formatPercentage(percentage: Double): String {
        return if (percentage % 1.0 == 0.0) {
            "${percentage.toInt()}%"
        } else {
            "$percentage%"
        }
    }
    
    /**
     * Formats a price with a percentage discount.
     * 
     * Examples:
     * - formatPriceWithDiscount(1200.0, 10.0) -> "₱1,200 (10% off)"
     * - formatPriceWithDiscount(5000.0, 15.0) -> "₱5,000 (15% off)"
     * 
     * @param amount Original price
     * @param discountPercentage Discount percentage
     * @return Formatted price with discount
     */
    fun formatPriceWithDiscount(amount: Double, discountPercentage: Double): String {
        return "${formatPrice(amount)} (${formatPercentage(discountPercentage)} off)"
    }
}
