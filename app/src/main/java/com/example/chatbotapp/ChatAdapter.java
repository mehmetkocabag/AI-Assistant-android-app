package com.example.chatbotapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class ChatAdapter extends BaseAdapter {
    private MainActivity context;
    private List<ChatInfo> chatInfo;

    public ChatAdapter(MainActivity context, List<ChatInfo> chatInfo) {
        this.context = context;
        this.chatInfo = chatInfo;
    }

    @Override
    public int getCount() {
        return chatInfo.size();
    }

    @Override
    public Object getItem(int position) {
        return chatInfo.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.message_item, parent, false);
        }
        TextView textView = convertView.findViewById(R.id.messageText);
        textView.setText(chatInfo.get(position).getFirstMessage().getContent());
        return convertView;
    }

    public void updateChatInfo(List<ChatInfo> newChatInfo) {
        chatInfo.clear();
        chatInfo.addAll(newChatInfo);
        notifyDataSetChanged();
    }
}
