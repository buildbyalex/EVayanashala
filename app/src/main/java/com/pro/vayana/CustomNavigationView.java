package com.pro.vayana;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.navigation.NavigationView;

public class CustomNavigationView extends NavigationView {
    private boolean showNotificationDot = false;
    private int dotColor = Color.RED;
    private float dotRadius = 10f;
    private int itemId = R.id.pending_approvals; // ID of the menu item to show the dot on

    public CustomNavigationView(Context context) {
        super(context);
    }

    public CustomNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (showNotificationDot) {
            View itemView = findViewById(itemId);
            if (itemView != null) {
                float x = itemView.getRight() - dotRadius * 2;
                float y = itemView.getTop() + itemView.getHeight() / 2f;

                Paint paint = new Paint();
                paint.setColor(dotColor);
                paint.setStyle(Paint.Style.FILL);

                canvas.drawCircle(x, y, dotRadius, paint);
            }
        }
    }

    public void setShowNotificationDot(boolean show) {
        this.showNotificationDot = show;
        invalidate();
    }

    public void setDotColor(int color) {
        this.dotColor = color;
    }

    public void setDotRadius(float radius) {
        this.dotRadius = radius;
    }

    public void setNotificationItemId(int id) {
        this.itemId = id;
    }
}