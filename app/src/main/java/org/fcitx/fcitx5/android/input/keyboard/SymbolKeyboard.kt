/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.picker.PickerWindow
import splitties.views.imageResource

/**
 * The symbol page, reached from the number pad's "!?#" key.
 *
 * This fork keeps symbols off the main keyboard (no swipe alt labels), so this
 * page is the one place where common symbols such as "@" have to live. The
 * exhaustive symbol list stays in the picker behind the "!?#" key.
 */
@SuppressLint("ViewConstructor")
class SymbolKeyboard(
    context: Context,
    theme: Theme,
) : BaseKeyboard(context, theme, Layout) {

    companion object {
        const val Name = "Symbol"

        val Layout: List<List<KeyDef>> = listOf(
            listOf(
                SymbolKey("1"),
                SymbolKey("2"),
                SymbolKey("3"),
                SymbolKey("4"),
                SymbolKey("5"),
                SymbolKey("6"),
                SymbolKey("7"),
                SymbolKey("8"),
                SymbolKey("9"),
                SymbolKey("0")
            ),
            listOf(
                SymbolKey("@"),
                SymbolKey("#"),
                SymbolKey("$"),
                SymbolKey("%"),
                SymbolKey("&"),
                SymbolKey("-"),
                SymbolKey("+"),
                SymbolKey("("),
                SymbolKey(")"),
                SymbolKey("/")
            ),
            listOf(
                SymbolKey("*"),
                SymbolKey("\""),
                SymbolKey("'"),
                SymbolKey(":"),
                SymbolKey(";"),
                SymbolKey("!"),
                SymbolKey("?"),
                SymbolKey("="),
                SymbolKey("_"),
                SymbolKey("\\")
            ),
            listOf(
                SymbolKey(","),
                SymbolKey("."),
                SymbolKey("~"),
                SymbolKey("^"),
                SymbolKey("|"),
                SymbolKey("["),
                SymbolKey("]"),
                SymbolKey("{"),
                SymbolKey("}"),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("ABC", TextKeyboard.Name),
                LayoutSwitchKey("?123", NumberKeyboard.Name),
                TextPickerSwitchKey("!?#", PickerWindow.Key.Symbol),
                SpaceKey(),
                ReturnKey()
            )
        )
    }

    val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
    val space: TextKeyView by lazy { findViewById(R.id.button_space) }
    val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

    override fun onReturnDrawableUpdate(returnDrawable: Int) {
        `return`.img.imageResource = returnDrawable
    }

    override fun onInputMethodUpdate(ime: InputMethodEntry) {
        space.mainText.text = buildString {
            append(ime.displayName)
            ime.subMode.run { label.ifEmpty { name.ifEmpty { null } } }?.let { append(" ($it)") }
        }
    }

}
