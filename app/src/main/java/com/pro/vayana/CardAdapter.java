package com.pro.vayana;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {
    private List<CardItem> cardItems;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(CardItem item);
    }

    public CardAdapter(List<CardItem> cardItems, OnItemClickListener listener) {
        this.cardItems = cardItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardItem cardItem = cardItems.get(position);
        holder.textView.setText(cardItem.title);

        // Load image with Glide and show placeholder
        Glide.with(holder.itemView.getContext())
                .load(cardItem.imageUrl)
                .apply(new RequestOptions().placeholder(R.drawable.place_holder)) // Placeholder image
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(cardItem));
    }

    @Override
    public int getItemCount() {
        return cardItems.size();
    }

    public class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            textView = itemView.findViewById(R.id.text_view);
        }
    }
}
