package com.example.plog.ui.diary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import com.example.plog.R;

public class LinedEditText extends AppCompatEditText {
    private final Paint linePaint = new Paint();

    public LinedEditText(Context context) {
        super(context);
        init();
    }

    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LinedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.diary_line));
        linePaint.setStrokeWidth(1f);
        setLineSpacing(8f, 1.0f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int lineHeight = getLineHeight();
        int startY = getPaddingTop() + lineHeight;
        int endX = getWidth() - getPaddingEnd();

        for (int y = startY; y < getHeight() - getPaddingBottom(); y += lineHeight) {
            canvas.drawLine(getPaddingStart(), y, endX, y, linePaint);
        }

        super.onDraw(canvas);
    }
}
