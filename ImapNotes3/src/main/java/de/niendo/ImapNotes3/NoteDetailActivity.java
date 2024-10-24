/*
 * Copyright (C) 2022-2024 - Peter Korf <peter@niendo.de>
 * Copyright (C)         ? - kwhitefoot
 * Copyright (C)      2016 - Martin Carpella
 * Copyright (C)      2014 - c0238
 * Copyright (C) 2014-2015 - nb
 * and Contributors.
 *
 * This file is part of ImapNotes3.
 *
 * ImapNotes3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.niendo.ImapNotes3;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.mail.Message;

import de.niendo.ImapNotes3.Data.NotesDb;
import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.Miscs.EditorMenuAdapter;
import de.niendo.ImapNotes3.Miscs.HtmlNote;
import de.niendo.ImapNotes3.Miscs.NDSpinner;
import de.niendo.ImapNotes3.Miscs.Utilities;
import de.niendo.ImapNotes3.Sync.SyncUtils;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.color.SimpleColorDialog;
import eltos.simpledialogfragment.form.Check;
import eltos.simpledialogfragment.form.Hint;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import jp.wasabeef.richeditor.RichEditor;


public class NoteDetailActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, SimpleDialog.OnDialogResultListener {

    public static final String selectedNote = "selectedNote";
    public static final String ActivityType = "ActivityType";
    public static final String ActivityTypeEdit = "ActivityTypeEdit";
    public static final String ActivityTypeAdd = "ActivityTypeAdd";
    public static final String ActivityTypeAddShare = "ActivityTypeAddShare";
    public static final String ActivityTypeProcessed = "ActivityTypeProcessed";
    public static final String DLG_TABLE_DIMENSION = "DLG_TABLE_DIMENSION";
    public static final String DLG_TABLE_DIMENSION_ROW = "DLG_TABLE_DIMENSION_ROW";
    public static final String DLG_TABLE_DIMENSION_COL = "DLG_TABLE_DIMENSION_COL";
    public static final String DLG_HTML_TXT_COLOR = "DLG_HTML_TXT_COLOR";
    public static final String DLG_HTML_BG_COLOR = "DLG_HTML_BG_COLOR";
    public static final String DLG_INSERT_LINK = "DLG_INSERT_LINK";

    public static final String DLG_INSERT_LINK_URL = "DLG_INSERT_LINK_URL";
    public static final String DLG_INSERT_LINK_TEXT = "DLG_INSERT_LINK_TEXT";
    public static final String DLG_INSERT_LINK_TITLE = "DLG_INSERT_LINK_TITLE";
    public static final String DLG_INSERT_LINK_IMAGE = "DLG_INSERT_LINK_IMAGE";
    public static final String DLG_INSERT_IMAGE = "DLG_INSERT_IMAGE";
    public static final String DLG_INSERT_IMAGE_INLINE = "DLG_INSERT_IMAGE_INLINE";
    public static final String DLG_INSERT_IMAGE_SHRINK_FACTOR = "DLG_INSERT_IMAGE_SHRINK_FACTOR";
    public static final String DLG_INSERT_LINK_IMAGE_URL = "DLG_INSERT_LINK_IMAGE_URL";
    public static final String DLG_INSERT_LINK_IMAGE_ALT = "DLG_INSERT_LINK_IMAGE_ALT";
    public static final String DLG_INSERT_LINK_IMAGE_WIDTH = "DLG_INSERT_LINK_IMAGE_WIDTH";
    public static final String DLG_INSERT_LINK_IMAGE_HEIGHT = "DLG_INSERT_LINK_IMAGE_HEIGHT";
    public static final String DLG_INSERT_LINK_IMAGE_RELATIVE = "DLG_INSERT_LINK_IMAGE_RELATIVE";
    public static final String DLG_INSERT_IMAGE_FILE_SIZE = "DLG_INSERT_IMAGE_FILE_SIZE";

    public static final String DLG_INSERT_HASHTAG_NAME = "DLG_INSERT_HASHTAG_NAME";
    public static final String DLG_INSERT_HASHTAG = "DLG_INSERT_HASHTAG";
    public static final String DLG_SELECT_ACCOUNT = "DLG_SELECT_ACCOUNT";
    public static final String DLG_SELECT_ACCOUNT_ACCOUNT = "DLG_SELECT_ACCOUNT_ACCOUNT";
    public static final String DLG_SELECT_ACCOUNT_FINISH = "DLG_SELECT_ACCOUNT_FINISH";
    public static final double MAX_INSERT_FILE_SIZE_MB = 1.0;
    private static final int EDIT_BUTTON = 6;
    private static final String TAG = "IN_NoteDetailActivity";
    private boolean textChanged = false;
    private boolean textChangedShare = false;
    @NonNull
    private String bgColor = "none";
    private String accountName = "";
    private String suid; // uid as string
    private RichEditor editText;
    private @ColorInt int lastTxtColor = 0x80e9a11d;
    private @ColorInt int lastBgColor = 0x80e9a11d;
    private String lastTag = "#";
    private List<String> tagList;
    private MenuItem itemNext;
    private MenuItem itemPrevious;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setElevation(0); // or other
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.ActionBgColor)));
        editText = findViewById(R.id.bodyView);

        Intent intent = getIntent();
        String stringres;
        Log.d(TAG, "Check_Action_Send");
        // Get intent, action and MIME type
        String action = intent.getAction();
        String ChangeNote = intent.getStringExtra(ActivityType);
        if (ChangeNote == null)
            ChangeNote = "";
        if (action == null)
            action = "";

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveChangesDialog();
            }
        });

        if (action.equals(Intent.ACTION_SEND) && !ChangeNote.equals(ActivityTypeAddShare) && !intent.getBooleanExtra(NoteDetailActivity.ActivityTypeProcessed, false)) {
            ImapNotes3.ShowAction(editText, R.string.insert_in_note, R.string.ok, 60,
                    () -> {
                        processShareIntent(intent);
                    });
        }
        if (ChangeNote.equals(ActivityTypeEdit)) {
            HashMap hm = (HashMap) intent.getSerializableExtra(selectedNote);

            if (hm == null) {
                // Entry can not opened..
                ImapNotes3.ShowMessage(R.string.Invalid_Note, null, 3);
                finish();
                return;
            }
            suid = hm.get(OneNote.UID).toString();
            accountName = hm.get(OneNote.ACCOUNT).toString();

            File rootDir = ImapNotes3.GetAccountDir(accountName);
            Message message = SyncUtils.ReadMailFromFileRootAndNew(suid, rootDir);
            Log.d(TAG, "rootDir: " + rootDir);
            if (message != null) {
                HtmlNote htmlNote = HtmlNote.GetNoteFromMessage(message);
                stringres = htmlNote.text;
                bgColor = htmlNote.color;
                SetupRichEditor();
                editText.setHtml(stringres);
            } else {
                // Entry can not opened..
                ImapNotes3.ShowMessage(R.string.Invalid_Message, null, 3);
                finish();
                return;
            }
        } else if (ChangeNote.equals(ActivityTypeAdd)) {   // new entry
            accountName = intent.getStringExtra(ListActivity.EDIT_ITEM_ACCOUNTNAME);
            SetupRichEditor();
        } else if (ChangeNote.equals(ActivityTypeAddShare)) {   // new Entry from Share
            accountName = intent.getStringExtra(ListActivity.EDIT_ITEM_ACCOUNTNAME);
            SetupRichEditor();
            processShareIntent(intent);
        }
        ResetColors();
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (which == BUTTON_NEUTRAL) {
            editText.requestFocusFromTouch();
            switch (dialogTag) {
                case DLG_HTML_TXT_COLOR:
                    editText.setTextColor("initial");
                    return true;
                case DLG_HTML_BG_COLOR:
                    editText.setTextBackgroundColor("initial");
                    return true;
            }
        }
        if (which == BUTTON_POSITIVE) {
            editText.requestFocusFromTouch();
            switch (dialogTag) {
                case DLG_HTML_TXT_COLOR:
                    lastTxtColor = extras.getInt(SimpleColorDialog.COLOR);
                    editText.setTextColor(lastTxtColor);
                    return true;
                case DLG_HTML_BG_COLOR:
                    lastBgColor = extras.getInt(SimpleColorDialog.COLOR);
                    editText.setTextBackgroundColor(lastBgColor);
                    return true;
                case DLG_TABLE_DIMENSION:
                    editText.insertTable(Integer.valueOf(extras.getString(DLG_TABLE_DIMENSION_COL)), Integer.valueOf(extras.getString(DLG_TABLE_DIMENSION_ROW)));
                    return true;
                case DLG_INSERT_LINK:
                    editText.insertLink(Utilities.CheckUrlScheme(extras.getString(DLG_INSERT_LINK_URL)), extras.getString(DLG_INSERT_LINK_TEXT), extras.getString(DLG_INSERT_LINK_TITLE));
                    return true;
                case DLG_INSERT_LINK_IMAGE: {
                    Boolean relative = extras.getBoolean(DLG_INSERT_LINK_IMAGE_RELATIVE);
                    editText.insertImage(Utilities.CheckUrlScheme(extras.getString(DLG_INSERT_LINK_IMAGE_URL)),
                            extras.getString(DLG_INSERT_LINK_IMAGE_ALT), extras.getString(DLG_INSERT_LINK_IMAGE_WIDTH),
                            extras.getString(DLG_INSERT_LINK_IMAGE_HEIGHT), relative);
                    return true;
                }
                case DLG_INSERT_HASHTAG:
                    lastTag = extras.getString(DLG_INSERT_HASHTAG_NAME);
                    editText.insertHTML(lastTag + " ");
                    if (!tagList.contains(lastTag))
                        tagList.add(0, lastTag);
                    return true;
                case DLG_INSERT_IMAGE: {
                    Boolean relative = extras.getBoolean(DLG_INSERT_LINK_IMAGE_RELATIVE);
                    Boolean inline = extras.getBoolean(DLG_INSERT_IMAGE_INLINE);
                    Integer scale = Integer.valueOf(extras.getString(DLG_INSERT_IMAGE_SHRINK_FACTOR));
                    Uri uri = extras.getParcelable(Intent.EXTRA_STREAM);
                    double fileSize = extras.getDouble(DLG_INSERT_IMAGE_FILE_SIZE);
                    editText.insertHTML(extras.getString(DLG_INSERT_LINK_IMAGE_ALT));
                    if (inline) {
                        if ((double) (Utilities.getRealSizeFromUri(this, uri) / (scale * scale)) > MAX_INSERT_FILE_SIZE_MB * 1024 * 1024) {
                            Log.d(TAG, "FileSize:" + fileSize / (scale * scale));
                            ImapNotes3.ShowMessage(String.format(getResources().getString(R.string.file_size_allowed), MAX_INSERT_FILE_SIZE_MB), editText, 2);
                        } else {
                            editText.insertImageAsBase64(uri, extras.getString(DLG_INSERT_LINK_IMAGE_ALT),
                                    extras.getString(DLG_INSERT_LINK_IMAGE_WIDTH),
                                    extras.getString(DLG_INSERT_LINK_IMAGE_HEIGHT), relative, scale);
                        }
                    } else {
                        editText.insertImage(Utilities.CheckUrlScheme(extras.getString(DLG_INSERT_LINK_IMAGE_URL)),
                                extras.getString(DLG_INSERT_LINK_IMAGE_ALT), extras.getString(DLG_INSERT_LINK_IMAGE_WIDTH),
                                extras.getString(DLG_INSERT_LINK_IMAGE_HEIGHT), relative);
                    }
                    editText.insertHTML("<br><br>");
                    return true;
                }
                case DLG_SELECT_ACCOUNT: {
                    accountName = extras.getString(DLG_SELECT_ACCOUNT_ACCOUNT);
                    saveNote(extras.getBoolean(DLG_SELECT_ACCOUNT_FINISH));
                    return true;
                }
                default: {
                    Log.d(TAG, "unknown dialog");
                }
            }
        }
        return false;
    }

    private void SetupRichEditor() {
        editText.setEditorBackgroundColor(0); // otherwise it will not work in dark mode
        editText.setPadding(10, 10, 10, 10);
        editText.setEditorFontSize(18);
        //    editText.setBackground("https://raw.githubusercontent.com/wasabeef/art/master/chip.jpg");
        editText.setPlaceholder(getString(R.string.placeholder));
        editText.LoadFont("Alita Brush", "Alita Brush.ttf");

        editText.setOnTextChangeListener(text -> {
            if (text.contains("loaded"))
                textChanged = textChangedShare;
            if (text.contains("input"))
                textChanged = true;
        });

        editText.setOnInitialLoadListener(ready -> {
            editText.setEditorHeight(editText.getHeight());
        });

        editText.setOnClickListener((RichEditor.onClickListener) text -> {
            String url = Utilities.getValueFromJSON(text, "href");
            ImapNotes3.ShowAction(editText, R.string.open_link, R.string.ok, 3,
                    () -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        try {
                            startActivity(browserIntent);
                        } catch (ActivityNotFoundException e) {
                            ImapNotes3.ShowMessage(R.string.no_program_found, editText, 3);
                        }

                    });
        });

        NDSpinner formatSpinner = findViewById(R.id.action_format);
        formatSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[11], R.id.action_format, this));
        formatSpinner.setOnItemSelectedListener(this);

        NDSpinner insertSpinner = findViewById(R.id.action_insert);
        insertSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[13], R.id.action_insert, this));
        insertSpinner.setOnItemSelectedListener(this);

        NDSpinner headingSpinner = findViewById(R.id.action_heading);
        headingSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[8], R.id.action_heading, this));
        headingSpinner.setOnItemSelectedListener(this);

        NDSpinner alignmentSpinner = findViewById(R.id.action_alignment);
        alignmentSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[9], R.id.action_alignment, this));
        alignmentSpinner.setOnItemSelectedListener(this);

        NDSpinner tableSpinner = findViewById(R.id.action_table);
        tableSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[5], R.id.action_table, this));
        tableSpinner.setOnItemSelectedListener(this);

        findViewById(R.id.action_undo).setOnClickListener(v -> editText.undo());
        findViewById(R.id.action_redo).setOnClickListener(v -> editText.redo());

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        NDSpinner spinner = (NDSpinner) parent;
        if ((view == null) || (!spinner.initIsDone)) return;
        switch (view.getId()) {
            case R.id.action_removeFormat:
                editText.removeFormat();
                break;
            case R.id.action_bold:
                editText.toggleBold();
                break;
            case R.id.action_italic:
                editText.toggleItalic();
                break;
            case R.id.action_subscript:
                editText.setSubscript();
                break;
            case R.id.action_superscript:
                editText.setSuperscript();
                break;
            case R.id.action_strikethrough:
                editText.toggleStrikeThrough();
                break;
            case R.id.action_underline:
                editText.toggleUnderline();
                break;
            case R.id.action_heading1:
                editText.setHeading(1);
                break;
            case R.id.action_heading2:
                editText.setHeading(2);
                break;
            case R.id.action_heading3:
                editText.setHeading(3);
                break;
            case R.id.action_heading4:
                editText.setHeading(4);
                break;
            case R.id.action_heading5:
                editText.setHeading(5);
                break;
            case R.id.action_heading6:
                editText.setHeading(6);
                break;
            case R.id.action_txt_color:
                SimpleColorDialog.build()
                        .colors(this, SimpleColorDialog.MATERIAL_COLOR_PALLET_DARK)
                        .title(getString(R.string.selectTextColor))
                        .colorPreset(lastTxtColor)
                        .setupColorWheelAlpha(false)
                        .allowCustom(true)
                        .neg(R.string.cancel)
                        .neut(R.string.default_color)
                        .show(this, DLG_HTML_TXT_COLOR);
                break;
            case R.id.action_bg_color:
                SimpleColorDialog.build()
                        .colors(this, SimpleColorDialog.MATERIAL_COLOR_PALLET_LIGHT)
                        .title(getString(R.string.selectBgColor))
                        .colorPreset(lastBgColor)
                        .setupColorWheelAlpha(false)
                        .allowCustom(true)
                        .neg(R.string.cancel)
                        .neut(R.string.default_color)
                        .show(this, DLG_HTML_BG_COLOR);
                break;
            case R.id.action_font_size_1:
                editText.requestFocusFromTouch();
                editText.setFontSize(1);
                break;
            case R.id.action_font_size_2:
                editText.requestFocusFromTouch();
                editText.setFontSize(2);
                break;
            case R.id.action_font_size_3:
                editText.requestFocusFromTouch();
                editText.setFontSize(3);
                break;
            case R.id.action_font_size_4:
                editText.requestFocusFromTouch();
                editText.setFontSize(4);
                break;
            case R.id.action_font_size_5:
                editText.requestFocusFromTouch();
                editText.setFontSize(5);
                break;
            case R.id.action_font_size_6:
                editText.requestFocusFromTouch();
                editText.setFontSize(6);
                break;
            case R.id.action_font_size_7:
                editText.requestFocusFromTouch();
                editText.setFontSize(7);
                break;
            case R.id.action_font_serif:
                editText.setFontFamily("serif");
                break;
            case R.id.action_font_sansserif:
                editText.setFontFamily("sans-serif");
                break;
            case R.id.action_font_monospace:
                editText.setFontFamily("monospace");
                break;
            case R.id.action_font_cursive:
                editText.setFontFamily("cursive");
                break;
            case R.id.action_font_fantasy:
                editText.setFontFamily("Alita Brush");
                break;
            case R.id.action_indent:
                editText.setIndent();
                break;
            case R.id.action_outdent:
                editText.setOutdent();
                break;
            case R.id.action_align_left:
                editText.setAlignLeft();
                break;
            case R.id.action_align_center:
                editText.setAlignCenter();
                break;
            case R.id.action_align_right:
                editText.setAlignRight();
                break;
            case R.id.action_blockquote:
                editText.setBlockquote();
                break;
            case R.id.action_insert_bullets:
                editText.setUnorderedList();
                break;
            case R.id.action_insert_numbers:
                editText.setOrderedList();
                break;
            case R.id.action_code_off_html:
                editText.setOnJSDataListener(value -> {
                    editText.insertHTML(Html.escapeHtml(value));
                });
                editText.getSelectedHtml();
                break;
            case R.id.action_code_html:
                editText.setOnJSDataListener(value -> {
                    editText.insertHTML(value);
                });
                editText.getSelectedText();
                break;
            case R.id.action_pre:
                editText.setPre();
                break;
            case R.id.action_insert_image:
                // 1. get the selected text via callback
                // 2. make the Image
                editText.setOnJSDataListener(value -> {
                    SimpleFormDialog.build()
                            .title(R.string.insert_link_image)
                            //.msg(R.string.please_fill_in_form)
                            .fields(
                                    Input.plain(DLG_INSERT_LINK_IMAGE_URL)
                                            .required()
                                            .hint(R.string.link_image_url)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(value),
                                    Check.box(DLG_INSERT_LINK_IMAGE_RELATIVE)
                                            .check(true)
                                            .label(R.string.image_size_relative),
                                    Input.plain(DLG_INSERT_LINK_IMAGE_WIDTH)
                                            .hint(R.string.link_image_width)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text("100%"),
                                    Input.plain(DLG_INSERT_LINK_IMAGE_HEIGHT)
                                            .hint(R.string.link_image_height)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(""),
                                    Input.plain(DLG_INSERT_LINK_IMAGE_ALT)
                                            .hint(R.string.link_alt_text)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text("")
                            )
                            .neg(R.string.cancel)
                            .show(this, DLG_INSERT_LINK_IMAGE);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_audio:
                // 1. get the selected text via callback
                // 2. insert audio
                editText.setOnJSDataListener(value -> {
                    if (!value.isEmpty()) {
                        editText.insertAudio(value, "");
                    } else
                        ImapNotes3.ShowMessage(R.string.select_link_audio, editText, 1);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_video:
                // 1. get the selected text via callback
                // 2. insert video
                editText.setOnJSDataListener(value -> {
                    if (!value.isEmpty()) {
                        editText.insertVideo(value, "video", "100%", "", true, "controls");
                    } else
                        ImapNotes3.ShowMessage(R.string.select_link_video, editText, 3);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_youtube:
                // 1. get the selected text via callback
                // 2. insert voutube video
                editText.setOnJSDataListener(value -> {
                    if (!value.isEmpty()) {
                        if (value.startsWith("https://www.youtube.com"))
                            value = value.replace("watch?v=", "embed/");
                        // https://www.youtube.com/watch?v=3AeYHDZ2riI
                        // https://www.youtube.com/embed/3AeYHDZ2riI
                        editText.insertYoutubeVideo(value, "100%", "", true);
                    } else
                        ImapNotes3.ShowMessage(R.string.select_link_youtube, editText, 3);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_link:
                // 1. get the selected text via callback
                // 2. make the Link
                editText.setOnJSDataListener(value -> {
                    String url = "";
                    String text = "";
                    String title = "";
                    if (!value.isEmpty()) {
                        String[] values = value.split(" ", 2);
                        if (values.length == 2) {
                            url = Utilities.CheckUrlScheme(values[0]);
                            title = values[0];
                            text = values[1];
                        } else {
                            url = Utilities.CheckUrlScheme(value);
                            text = value;
                            title = value;
                        }
                    }
                    SimpleFormDialog.build()
                            .title(R.string.insert_link)
                            //.msg(R.string.please_fill_in_form)
                            .fields(
                                    Input.plain(DLG_INSERT_LINK_URL)
                                            .required()
                                            .hint(R.string.link_url)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(url),
                                    Input.plain(DLG_INSERT_LINK_TEXT)
                                            .hint(R.string.link_text)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(text),
                                    Input.plain(DLG_INSERT_LINK_TITLE)
                                            .required()
                                            .hint(R.string.link_title)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(title)
                            )
                            .neg(R.string.cancel)
                            .show(this, DLG_INSERT_LINK);
                });
                editText.getSelectedText();
                break;
            case R.id.action_insert_checkbox:
                editText.insertCheckbox();
                break;
            case R.id.action_insert_star:
                editText.insertHTML("&#11088;");
                break;
            case R.id.action_insert_question:
                editText.insertHTML("&#10067;");
                break;
            case R.id.action_insert_datetime:
                //String date = Utilities.internalDateFormat.format(Calendar.getInstance().getTime());
                String date = Calendar.getInstance().getTime().toLocaleString();
                editText.insertHTML(date);
                break;
            case R.id.action_insert_hashtag:
                NotesDb storedNotes = NotesDb.getInstance(getApplicationContext());
                if (tagList == null)
                    tagList = storedNotes.GetTags("", "");
                String[] tagArray = new String[tagList.size()];
                tagList.toArray(tagArray);
                SimpleFormDialog.build()
                        .title(R.string.insert_hashtag)
                        //.msg(R.string.please_fill_in_form)
                        .fields(
                                Input.plain(DLG_INSERT_HASHTAG_NAME).required().hint(R.string.insert_hashtag_name)
                                        .inputType(InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_CLASS_TEXT)
                                        .suggest(tagArray)
                                        .asSpinner(false)
                                        .validatePattern(Utilities.HASHTAG_PATTERN, getString(R.string.insert_hashtag_syntax))
                                        .text(lastTag)
                        )
                        .neg(R.string.cancel)
                        .show(this, DLG_INSERT_HASHTAG);
                break;
            case R.id.action_insert_exclamation:
                editText.insertHTML("&#10071;");
                break;
            case R.id.action_insert_hline:
                editText.insertHR_Line();
                break;
            case R.id.action_insert_section:
                editText.insertCollapsibleSection(getString(R.string.section), getString(R.string.content));
                break;
            case R.id.action_insert_table:
                SimpleFormDialog.build()
                        .title(R.string.enter_table_dimension)
                        //.msg(R.string.please_fill_in_form)
                        .fields(
                                Input.plain(DLG_TABLE_DIMENSION_COL).required().hint(R.string.count_table_col)
                                        .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                                        .text("3"),
                                Input.plain(DLG_TABLE_DIMENSION_ROW).required().hint(R.string.count_table_row)
                                        .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                                        .text("5")
                        )
                        .neg(R.string.cancel)
                        .show(this, DLG_TABLE_DIMENSION);

                break;
            case R.id.action_insert_column:
                editText.addColumnToTable();
                break;
            case R.id.action_insert_row:
                editText.addRowToTable();
                break;
            case R.id.action_delete_column:
                editText.deleteColumnFromTable();
                break;
            case R.id.action_delete_row:
                editText.deleteRowFromTable();
                break;

        }

        // for color selection, it does not closes by itself
        NDSpinner formatSpinner = findViewById(R.id.action_format);
        formatSpinner.onDetachedFromWindow();

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // realColor is misnamed.  It is the ID of the radio button widget that chooses the background
    // colour.
    private void ResetColors() {
        lastTxtColor = ImapNotes3.loadPreferenceColor("EditorTxtColor", getColor(R.color.EditorTxtColor));
        editText.setEditorFontColor(lastTxtColor);
        int mybgColor = Utilities.getColorByName(bgColor, getApplicationContext());
        if (bgColor.equals("none")) {
            mybgColor = ImapNotes3.loadPreferenceColor("EditorBgColorDefault", getColor(R.color.EditorBgColorDefault));
        }
        lastBgColor = mybgColor;
        (findViewById(R.id.scrollView)).setBackgroundColor(mybgColor);
    }

    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        MenuBuilder m = (MenuBuilder) menu;
        m.setOptionalIconsVisible(true);
        itemNext = menu.findItem(R.id.itemNext);
        itemPrevious = menu.findItem(R.id.itemPrevious);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        //MenuItem item = menu.findItem(R.id.color);
        super.onPrepareOptionsMenu(menu);
        //depending on your conditions, either enable/disable
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final Intent intent = new Intent();
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.itemSearch:
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setQueryHint(getText(R.string.search_any_keyword));

                // set a listener when the start typing in the SearchView
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        searchView.clearFocus();
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        // When the query length is greater
                        // than 0 we will perform the search
                        if (query.length() > 0) {
                            // findAllAsync finds all instances
                            // on the page and
                            // highlights them,asynchronously.
                            editText.findAllAsync(query);

                            itemNext.setVisible(true);
                            itemPrevious.setVisible(true);
                        } else {
                            editText.clearMatches();
                            itemNext.setVisible(false);
                            itemPrevious.setVisible(false);
                        }

                        return true;
                    }
                });
                break;
            case R.id.itemNext:
                editText.findNext(true);
                break;
            case R.id.itemPrevious:
                editText.findNext(false);
                break;
            case R.id.delete:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.delete_note)
                        .setMessage(R.string.are_you_sure_you_wish_to_delete_the_note)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                            //Log.d(TAG,"We ask to delete Message #"+this.currentNote.get("number"));
                            intent.putExtra("DELETE_ITEM_NUM_IMAP", suid);
                            intent.putExtra(ListActivity.EDIT_ITEM_ACCOUNTNAME, accountName);
                            setResult(ListActivity.DELETE_BUTTON, intent);
                            finish();//finishing activity
                        })
                        .setNegativeButton(R.string.no, null).show();
                return true;
            case R.id.save:
                saveNote(true);
                return true;
            case R.id.share:
                Share();
                return true;
            case android.R.id.home:
                saveChangesDialog();
                return true;
            case R.id.none:
                item.setChecked(true);
                bgColor = "none";
                ResetColors();
                return true;
            case R.id.blue:
                item.setChecked(true);
                bgColor = "blue";
                ResetColors();
                return true;
            case R.id.white:
                item.setChecked(true);
                bgColor = "white";
                ResetColors();
                return true;
            case R.id.gray:
                item.setChecked(true);
                bgColor = "gray";
                ResetColors();
                return true;
            case R.id.black:
                item.setChecked(true);
                bgColor = "black";
                ResetColors();
                return true;
            case R.id.yellow:
                item.setChecked(true);
                bgColor = "yellow";
                ResetColors();
                return true;
            case R.id.pink:
                item.setChecked(true);
                bgColor = "pink";
                ResetColors();
                return true;
            case R.id.green:
                item.setChecked(true);
                bgColor = "green";
                ResetColors();
                return true;
            case R.id.brown:
                item.setChecked(true);
                bgColor = "brown";
                ResetColors();
                return true;
            case R.id.red:
                item.setChecked(true);
                bgColor = "red";
                ResetColors();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }


    /**
     * Note that this function does not save the note to permanent storage it just passes it back to
     * the calling activity to be saved in whatever fashion it that activity wishes.
     */
    private void saveNote(boolean finish) {
        Log.d(TAG, "saveNote");

        if (accountName == null || accountName.isEmpty()) {
            Bundle extra = new Bundle();
            extra.putBoolean(DLG_SELECT_ACCOUNT_FINISH, finish);

            ArrayList accounts = ListActivity.getAccountList();

            SimpleFormDialog.build()
                    .extra(extra)
                    .title(R.string.select_one_account)
                    .fields(
                            Input.spinner(DLG_SELECT_ACCOUNT_ACCOUNT, accounts)
                                    .required()
                                    .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL))

                    .neg(R.string.cancel)
                    .show(this, DLG_SELECT_ACCOUNT);
            return;
        }

        editText.setOnJSDataListener(value -> {
            Log.d(TAG, "Save setOnJSDataListener");
            Intent intent = new Intent();
            intent.putExtra(ListActivity.EDIT_ITEM_NUM_IMAP, suid);
            intent.putExtra(ListActivity.EDIT_ITEM_ACCOUNTNAME, accountName);
            ImapNotes3.AvoidLargeBundle = value;
            //intent.putExtra(ListActivity.EDIT_ITEM_TXT, value);
            intent.putExtra(ListActivity.EDIT_ITEM_COLOR, bgColor);
            setResult(NoteDetailActivity.EDIT_BUTTON, intent);
            textChanged = false;
            if (finish) finish(); //finishing activity
        });
        // data comes via callback
        editText.getHtml();
        NotesDb storedNotes = NotesDb.getInstance(getApplicationContext());
        storedNotes.SetSaveState(suid, OneNote.SAVE_STATE_SAVING, accountName);
    }

    private void Share() {
        Log.d(TAG, "Share");
        editText.setOnJSDataListener(value -> {
            Intent sendIntent = new Intent();
            Spanned html = Html.fromHtml(value, Html.FROM_HTML_MODE_COMPACT);
            String[] tok = html.toString().split("\n", 2);
            String title = tok[0];

            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("text/html");
            sendIntent.putExtra(Intent.EXTRA_TEXT, html);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.shared_note_from) + Utilities.ApplicationName + ": " + title);

            String directory = getApplicationContext().getCacheDir().toString();
            File outfile = new File(directory, title.replaceAll("[#:/]", "") + ".html");
            Log.d(TAG, "Share Note: " + outfile);
            try (OutputStream str = new FileOutputStream(outfile)) {
                str.write(value.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                ImapNotes3.ShowMessage(R.string.share_file_error + e.toString(), editText, 2);
                e.printStackTrace();
            }

            Uri logUri =
                    FileProvider.getUriForFile(
                            getApplicationContext(),
                            Utilities.PackageName, outfile);
            sendIntent.putExtra(Intent.EXTRA_STREAM, logUri);

            Intent shareIntent = Intent.createChooser(sendIntent, title);
            startActivity(shareIntent);
        });
        // data comes via callback
        editText.getHtml();

    }

    private void processShareIntent(Intent intent) {
        Log.d(TAG, "processShareIntent");
        // Share: Receive Data as new message
        String strAction = intent.getAction();
        if (!editText.hasFocus()) editText.focusEditor();
        if ((strAction != null) && strAction.equals(Intent.ACTION_SEND)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            String type = intent.getType();
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            if (subject == null) subject = "";
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                if (type.startsWith("image/")) {
                    Bundle extra = new Bundle();
                    extra.putParcelable(Intent.EXTRA_STREAM, uri);
                    double fileSize = Utilities.getRealSizeFromUri(this, uri);
                    double fileSizeMB = fileSize / 1024 / 1024;
                    int shrink = (int) Math.max(1, Math.ceil(Math.sqrt(fileSizeMB / MAX_INSERT_FILE_SIZE_MB)));
                    extra.putDouble(DLG_INSERT_IMAGE_FILE_SIZE, fileSize);
                    SimpleFormDialog.build()
                            .title(R.string.insert_shared_image)
                            //.msg(R.string.please_fill_in_form)
                            .extra(extra)
                            .fields(
                                    Input.plain(DLG_INSERT_LINK_IMAGE_URL)
                                            .required()
                                            .hint(R.string.link_image_url)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(uri.toString()),
                                    Check.box(DLG_INSERT_IMAGE_INLINE)
                                            .check(false)
                                            .label(R.string.insert_image_as_inline),
                                    Input.plain(DLG_INSERT_IMAGE_SHRINK_FACTOR).
                                            required().
                                            hint(R.string.insert_image_shrink)
                                            .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                                            .text(String.valueOf(shrink)),
                                    Hint.plain(String.format(getResources().getString(R.string.file_size_info), fileSizeMB, MAX_INSERT_FILE_SIZE_MB)),
                                    Check.box(DLG_INSERT_LINK_IMAGE_RELATIVE)
                                            .check(true)
                                            .label(R.string.image_size_relative),
                                    Input.plain(DLG_INSERT_LINK_IMAGE_WIDTH)
                                            .hint(R.string.link_image_width)
                                            .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                                            .text("100"),
                                    Input.plain(DLG_INSERT_LINK_IMAGE_HEIGHT)
                                            .hint(R.string.link_image_height)
                                            .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                                            .text(""),
                                    Input.plain(DLG_INSERT_LINK_IMAGE_ALT)
                                            .hint(R.string.link_alt_text)
                                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL)
                                            .text(uri.getLastPathSegment())
                            )
                            .neg(R.string.cancel)
                            .show(this, DLG_INSERT_IMAGE);
                    //https://stackoverflow.com/questions/17839388/creating-a-scaled-bitmap-with-createscaledbitmap-in-android

                } else if (type.equals("message/rfc822")) {
                    try {
                        editText.insertHTML(HtmlNote.GetNoteFromMessage(SyncUtils.ReadMailFromString(ImapNotes3.UriToString(uri))).text);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    editText.insertHTML(ImapNotes3.UriToString(uri));
                }
            } else {
                if (Utilities.IsUrl(sharedText)) {
                    if (subject.isEmpty()) {
                        subject = sharedText;
                    }
                    editText.insertLink(sharedText, subject, subject);
                } else {
                    if (!subject.isEmpty()) {
                        subject = "<b>" + subject + "</b><br>";
                    }
                    if (sharedText != null) {
                        if (type.equals("text/html")) {
                            editText.insertHTML(subject + sharedText);
                        } else if (type.startsWith("text/")) {
                            editText.insertHTML(subject + Html.escapeHtml(sharedText).replace("\n", "<br>"));
                        } else if (type.startsWith("image/")) {
                            editText.insertImage(sharedText, "shared image", "100", "", true);
                        }
                    }
                }
            }
            textChangedShare = true;
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    public void saveChangesDialog() {
        Log.d(TAG, "saveChangesDialog");
        if (textChanged) {
            new AlertDialog.Builder(this)
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.made_changes)
                    .setMessage(R.string.save_changes)
                    .setNegativeButton(R.string.no, (arg0, arg1) -> finish())
                    .setNeutralButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                        saveNote(true);
                    }).create().show();
        } else {
            finish();
        }
    }
}
