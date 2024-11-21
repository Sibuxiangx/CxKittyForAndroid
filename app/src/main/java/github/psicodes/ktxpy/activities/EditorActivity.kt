/*
Copyright (C) 2022-2023  PsiCodes

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package github.psicodes.ktxpy.activities

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Bundle
import android.system.ErrnoException
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.UriUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import github.psicodes.ktxpy.R
import github.psicodes.ktxpy.dataStore.SettingsDataStore
import github.psicodes.ktxpy.databinding.ActivityEditorBinding
import github.psicodes.ktxpy.ui.theme.EditorTheme
import github.psicodes.ktxpy.utils.Keys
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.event.KeyBindingEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.style.builtin.ScaleCursorAnimator
import io.github.rosemoe.sora.widget.subscribeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStreamReader


class EditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditorBinding
    private var undo: MenuItem? = null
    private var redo: MenuItem? = null
    private lateinit var currentFile: File
    private lateinit var dataStore: SettingsDataStore
    private val currentFiles = arrayListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        github.psicodes.ktxpy.utils.CrashHandler.INSTANCE.init(this)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.symbolInput.bindEditor(binding.editor)
        setCurrentFile()
        dataStore = SettingsDataStore(applicationContext)
        binding.materialToolbar.setTitleTextAppearance(this, R.style.RobotoBoldTextAppearance)
        binding.runCode.setImageIcon(Icon.createWithResource(this, R.drawable.code_run_icon))
        setSupportActionBar(binding.materialToolbar)
        binding.symbolInput.addSymbols(
            arrayOf("->", "{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/", "!", "&", "#", "=", "-"),
            arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/", "!", "&", "#", "=", "-")
        )
        val typeface = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")

        binding.symbolInput.forEachButton {
            it.typeface = typeface
        }
        binding.editor.apply {
            typefaceText = typeface
            setLineSpacing(2f, 1.1f)
            cursorAnimator = ScaleCursorAnimator(this)
            nonPrintablePaintingFlags =
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or CodeEditor.FLAG_DRAW_LINE_SEPARATOR or CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION
            // Update display dynamically
            subscribeEvent<SelectionChangeEvent> { _, _ -> updatePositionText() }
            subscribeEvent<ContentChangeEvent> { _, _ ->
                postDelayed(
                    ::updateBtnState,
                    50
                )
            }
            subscribeEvent<KeyBindingEvent> { event, _ ->
                if (event.eventType != EditorKeyEvent.Type.DOWN) {
                    return@subscribeEvent
                }
            }
        }

        if (savedInstanceState != null) {
            binding.editor.setText(savedInstanceState.getString(TEXT_KEY))
        } else {
            binding.editor.setText(currentFile.readText())
        }
        updatePositionText()
        updateBtnState()
        CoroutineScope(Dispatchers.Default).launch {
            setTheme(dataStore.mThemeString.first() ?: EditorTheme.DarculaTheme)
        }
        binding.runCode.visibility = View.GONE
        binding.runCode.setOnClickListener {
//            saveFile()
//            val mIntent = Intent()
//            mIntent.setClass(this, TermActivity::class.java)
//            mIntent.putExtra(Keys.KEY_FILE_PATH, currentFile.absolutePath)
//            startActivity(mIntent)
            throw NotImplementedError()
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            saveFile()
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setCurrentFile() {
        if (intent == null) {
            throw RuntimeException("Intent is null")
        }
        intent.getStringExtra(Keys.KEY_FILE_PATH)?.let {
            runCatching {
                currentFile = File(intent.getStringExtra(Keys.KEY_FILE_PATH)!!)
                if (!currentFile.canRead()) {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
                binding.editor.setText(currentFile.readText())
                if (!currentFiles.contains(currentFile)) {
                    currentFiles.add(currentFile)
                }
            }.onFailure {
                it.printStackTrace()
                Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show()
                finish()
            }.onSuccess {
                return
            }
        }
    }

    private fun updateBtnState() {
        undo?.isEnabled = binding.editor.canUndo()
        redo?.isEnabled = binding.editor.canRedo()
    }

    private fun updatePositionText() {
        val cursor = binding.editor.cursor
        var text = ""
        text += if (cursor.isSelected) {
            "(" + (cursor.right - cursor.left) + " chars)"
        } else {
            val content = binding.editor.text
            if (content.getColumnCount(cursor.leftLine) == cursor.leftColumn) {
                "(<" + content.getLine(cursor.leftLine).lineSeparator.let {
                    if (it == LineSeparator.NONE) {
                        "EOF"
                    } else {
                        it.name
                    }
                } + ">)"
            } else {
                val char = binding.editor.text.charAt(
                    cursor.leftLine,
                    cursor.leftColumn
                )
                if (char.isLowSurrogate() && cursor.leftColumn > 0) {
                    "(" + String(
                        charArrayOf(
                            binding.editor.text.charAt(
                                cursor.leftLine,
                                cursor.leftColumn - 1
                            ), char
                        )
                    ) + ")"
                } else if (char.isHighSurrogate() && cursor.leftColumn + 1 < binding.editor.text.getColumnCount(
                        cursor.leftLine
                    )
                ) {
                    "(" + String(
                        charArrayOf(
                            char, binding.editor.text.charAt(
                                cursor.leftLine,
                                cursor.leftColumn + 1
                            )
                        )
                    ) + ")"
                } else {
                    "(" + escapeIfNecessary(
                        binding.editor.text.charAt(
                            cursor.leftLine,
                            cursor.leftColumn
                        )
                    ) + ")"
                }
            }
        }
    }

    private fun escapeIfNecessary(c: Char): String {
        return when (c) {
            '\n' -> "\\n"
            '\t' -> "\\t"
            '\r' -> "\\r"
            ' ' -> "<ws>"
            else -> c.toString()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        undo = menu.findItem(R.id.text_undo)
        redo = menu.findItem(R.id.text_redo)

        menu.findItem(R.id.current_files).isVisible = false
        menu.findItem(R.id.text_file_add).isVisible = false

        return super.onCreateOptionsMenu(menu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(TEXT_KEY, binding.editor.text.toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.editor.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val editor = binding.editor
        when (id) {
            R.id.text_undo -> editor.undo()
            R.id.current_files -> throw NotImplementedError()
            R.id.text_file_add -> throw NotImplementedError()
            R.id.text_redo -> editor.redo()
            R.id.text_save -> saveFile()
            R.id.text_theme -> {
                showThemeDialog(arrayOf("Quiet Light", "Darcula", "Abyss Color"))
            }
            R.id.reset_config -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val f = File(filesDir, "cxkitty/config.yml")
                    if (f.exists()) f.deleteRecursively()

                    val of = File(filesDir, "cxkitty/config.yml.bak")

                    if (of.exists()) {
                        of.copyTo(f)
                        lifecycleScope.launch {
                            Toast.makeText(this@EditorActivity, "重置成功", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    }
                    else throw FileNotFoundException("Backup file not found")
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveFile() {
        val fos = FileOutputStream(currentFile)
        CoroutineScope(Dispatchers.IO).run {
            fos.write(binding.editor.text.toString().toByteArray())
            fos.close()
        }
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
    }

    private fun showThemeDialog(themes: Array<String>) {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("选择主题")
        builder.setItems(themes) { _: DialogInterface?, which: Int ->
            when (which) {
                0 -> {
                    setTheme(EditorTheme.QuietLight)
                    CoroutineScope(Dispatchers.IO).launch {
                        dataStore.updateTheme(EditorTheme.QuietLight)
                    }
                }

                1 -> {
                    setTheme(EditorTheme.DarculaTheme)
                    CoroutineScope(Dispatchers.IO).launch {
                        dataStore.updateTheme(EditorTheme.DarculaTheme)
                    }
                }

                2 -> {
                    setTheme(EditorTheme.AbyssColor)
                    CoroutineScope(Dispatchers.IO).launch {
                        dataStore.updateTheme(EditorTheme.AbyssColor)
                    }
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun setTheme(mTheme: String) {
        val editor = binding.editor
        when (mTheme) {
            EditorTheme.QuietLight -> try {
                val themeSource = IThemeSource.fromInputStream(
                    assets.open("textmate/QuietLight.tmTheme"),
                    "QuietLight.tmTheme",
                    null
                )
                val colorScheme = TextMateColorScheme.create(themeSource)
                editor.colorScheme = colorScheme
                val language = TextMateLanguage.create(
                    IGrammarSource.fromInputStream(
                        assets.open("textmate/python/syntax/python.tmLanguage.json"),
                        "Python.tmLanguage.json",
                        null
                    ),
                    InputStreamReader(assets.open("textmate/python/language-configuration.json")),
                    (editor.colorScheme as TextMateColorScheme).themeSource
                )
                editor.setEditorLanguage(language)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            EditorTheme.DarculaTheme -> try {
                val themeSource = IThemeSource.fromInputStream(
                    assets.open("textmate/darcula.json"),
                    "darcula.json",
                    null
                )

                val colorScheme = TextMateColorScheme.create(themeSource)
                editor.colorScheme = colorScheme
                val language = TextMateLanguage.create(
                    IGrammarSource.fromInputStream(
                        assets.open("textmate/python/syntax/python.tmLanguage.json"),
                        "Python.tmLanguage.json",
                        null
                    ),
                    InputStreamReader(assets.open("textmate/python/language-configuration.json")),
                    (editor.colorScheme as TextMateColorScheme).themeSource
                )
                editor.setEditorLanguage(language)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            EditorTheme.AbyssColor -> try {
                val themeSource = IThemeSource.fromInputStream(
                    assets.open("textmate/abyss-color-theme.json"),
                    "abyss-color-theme.json",
                    null
                )
                val colorScheme = TextMateColorScheme.create(themeSource)
                editor.colorScheme = colorScheme
                val language = TextMateLanguage.create(
                    IGrammarSource.fromInputStream(
                        assets.open("textmate/python/syntax/python.tmLanguage.json"),
                        "Python.tmLanguage.json",
                        null
                    ),
                    InputStreamReader(assets.open("textmate/python/language-configuration.json")),
                    (editor.colorScheme as TextMateColorScheme).themeSource
                )
                editor.setEditorLanguage(language)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val TEXT_KEY = "SavedText"
    }

}