package de.niendo.ImapNotes3.Miscs;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.PopupMenu;

import java.util.List;

import de.niendo.ImapNotes3.R;

public class AttachmentAdapter extends ArrayAdapter<String> {

    Context context;
    int layoutResourceId;


// https://stackoverflow.com/questions/34565481/add-a-drop-down-menu-for-each-item-of-a-custom-listview
    ViewHolder holder = null;

    public AttachmentAdapter(Context context, int layoutResourceId, List<String> items) {
        super(context, layoutResourceId, items);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        //  this.listener=callback;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final String lists = getItem(position);
        final int pos = position;


        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.attachment_element, null);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.attachmentTitle);
            holder.txtDetail = (TextView) convertView.findViewById(R.id.attachmentInfo);
            holder.imageview = (ImageView) convertView.findViewById(R.id.attachmentBtn);

            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        //holder.txtTitle.setText(lists.getTitle());
        //holder.txtDetail.setText(lists.getDetail());

        holder.txtTitle.setText(lists);
        holder.txtDetail.setText(lists);

        try {
            holder.imageview.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {


                    switch (v.getId()) {
                        case R.id.attachmentBtn:


                            PopupMenu popup = new PopupMenu(context.getApplicationContext(), v);

                            popup.getMenuInflater().inflate(R.menu.popup_attachment,
                                    popup.getMenu());

                            popup.show();
                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {

                                    switch (item.getItemId()) {
                                        case R.id.shareAttachment:

                                            //Or Some other code you want to put here.. This is just an example.
                                            Toast.makeText(context.getApplicationContext(), " Install Clicked at position " + " : " + position, Toast.LENGTH_LONG).show();

                                            break;
                                        case R.id.deleteAttachment:

                                            Toast.makeText(context.getApplicationContext(), "Add to Wish List Clicked at position " + " : " + position, Toast.LENGTH_LONG).show();

                                            break;

                                        default:
                                            break;
                                    }

                                    return true;
                                }
                            });

                            break;

                        default:
                            break;
                    }


                }
            });

        } catch (Exception e) {

            e.printStackTrace();
        }

        return convertView;
    }

    /*private view holder class*/
    private class ViewHolder {

        TextView txtTitle;
        TextView txtDetail;
        ImageView imageview;

    }

}