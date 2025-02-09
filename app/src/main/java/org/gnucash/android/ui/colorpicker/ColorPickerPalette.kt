/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2022 Xilin Jia https://github.com/XilinJia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gnucash.android.ui.colorpicker

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import org.gnucash.android.R
import org.gnucash.android.ui.colorpicker.ColorPickerSwatch.OnColorSelectedListener

/**
 * A color picker custom view which creates an grid of color squares.  The number of squares per
 * row (and the padding between the squares) is determined by the user.
 * @author Xilin Jia <https://github.com/XilinJia> [Kotlin code created (Copyright (C) 2022)]
 */
class ColorPickerPalette : TableLayout {
    var mOnColorSelectedListener: OnColorSelectedListener? = null
    private var mDescription: String? = null
    private var mDescriptionSelected: String? = null
    private var mSwatchLength = 0
    private var mMarginSize = 0
    private var mNumColumns = 0

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    /**
     * Initialize the size, columns, and listener.  Size should be a pre-defined size (SIZE_LARGE
     * or SIZE_SMALL) from ColorPickerDialogFragment.
     */
    fun init(size: Int, columns: Int, listener: OnColorSelectedListener?) {
        mNumColumns = columns
        val res = resources
        if (size == ColorPickerDialog.SIZE_LARGE) {
            mSwatchLength = res.getDimensionPixelSize(R.dimen.color_swatch_large)
            mMarginSize = res.getDimensionPixelSize(R.dimen.color_swatch_margins_large)
        } else {
            mSwatchLength = res.getDimensionPixelSize(R.dimen.color_swatch_small)
            mMarginSize = res.getDimensionPixelSize(R.dimen.color_swatch_margins_small)
        }
        mOnColorSelectedListener = listener
        mDescription = res.getString(R.string.color_swatch_description)
        mDescriptionSelected = res.getString(R.string.color_swatch_description_selected)
    }

    private fun createTableRow(): TableRow {
        val row = TableRow(context)
        val params = ViewGroup.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        row.layoutParams = params
        return row
    }

    /**
     * Adds swatches to table in a serpentine format.
     */
    fun drawPalette(colors: IntArray?, selectedColor: Int) {
        if (colors == null) {
            return
        }
        removeAllViews()
        var tableElements = 0
        var rowElements = 0
        var rowNumber = 0

        // Fills the table with swatches based on the array of colors.
        var row = createTableRow()
        for (color in colors) {
            tableElements++
            val colorSwatch: View = createColorSwatch(color, selectedColor)
            setSwatchDescription(
                rowNumber, tableElements, rowElements, color == selectedColor,
                colorSwatch
            )
            addSwatchToRow(row, colorSwatch, rowNumber)
            rowElements++
            if (rowElements == mNumColumns) {
                addView(row)
                row = createTableRow()
                rowElements = 0
                rowNumber++
            }
        }

        // Create blank views to fill the row if the last row has not been filled.
        if (rowElements > 0) {
            while (rowElements != mNumColumns) {
                addSwatchToRow(row, createBlankSpace(), rowNumber)
                rowElements++
            }
            addView(row)
        }
    }

    /**
     * Appends a swatch to the end of the row for even-numbered rows (starting with row 0),
     * to the beginning of a row for odd-numbered rows.
     */
    private fun addSwatchToRow(row: TableRow, swatch: View, rowNumber: Int) {
        if (rowNumber % 2 == 0) {
            row.addView(swatch)
        } else {
            row.addView(swatch, 0)
        }
    }

    /**
     * Add a content description to the specified swatch view. Because the colors get added in a
     * snaking form, every other row will need to compensate for the fact that the colors are added
     * in an opposite direction from their left->right/top->bottom order, which is how the system
     * will arrange them for accessibility purposes.
     */
    private fun setSwatchDescription(
        rowNumber: Int, index: Int, rowElements: Int, selected: Boolean,
        swatch: View
    ) {
        val accessibilityIndex: Int = if (rowNumber % 2 == 0) {
            // We're in a regular-ordered row
            index
        } else {
            // We're in a backwards-ordered row.
            val rowMax = (rowNumber + 1) * mNumColumns
            rowMax - rowElements
        }
        val description: String = if (selected) {
            String.format(mDescriptionSelected!!, accessibilityIndex)
        } else {
            String.format(mDescription!!, accessibilityIndex)
        }
        swatch.contentDescription = description
    }

    /**
     * Creates a blank space to fill the row.
     */
    private fun createBlankSpace(): ImageView {
        val view = ImageView(context)
        val params = TableRow.LayoutParams(mSwatchLength, mSwatchLength)
        params.setMargins(mMarginSize, mMarginSize, mMarginSize, mMarginSize)
        view.layoutParams = params
        return view
    }

    /**
     * Creates a color swatch.
     */
    private fun createColorSwatch(color: Int, selectedColor: Int): ColorPickerSwatch {
        val view = ColorPickerSwatch(
            context, color,
            color == selectedColor, mOnColorSelectedListener
        )
        val params = TableRow.LayoutParams(mSwatchLength, mSwatchLength)
        params.setMargins(mMarginSize, mMarginSize, mMarginSize, mMarginSize)
        view.layoutParams = params
        return view
    }
}