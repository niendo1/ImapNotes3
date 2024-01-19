package de.niendo.ImapNotes3;

import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.Miscs.AttachmentAdapter;
import de.niendo.ImapNotes3.databinding.ActivityAttachmentBinding;

public class AttachmentActivity extends AppCompatActivity {

    public static final String ACTION = "ACTION";
    private ActivityAttachmentBinding binding;


    private ArrayList<String> attachmentList;
    private AttachmentAdapter listToView;
    private ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttachmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        attachmentList = new ArrayList<>();
        attachmentList.add("Attach 1");
        attachmentList.add("Attach 2");
        attachmentList.add("Attach 3");

        //((de.niendo.ImapNotes3) this.getApplicationContext()).SetNotesList(this.noteList);
        listToView = new AttachmentAdapter(AttachmentActivity.this, R.layout.attachment_element, attachmentList);

        listview = findViewById(R.id.attachmentList);
        listview.setAdapter(this.listToView);


        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


    }

    //   @Nullable
    //   private Actions action;
}