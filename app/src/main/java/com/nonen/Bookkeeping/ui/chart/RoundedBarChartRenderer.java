package com.nonen.Bookkeeping.ui.chart;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class RoundedBarChartRenderer extends BarChartRenderer {
    private final RectF shadowRect = new RectF();

    public RoundedBarChartRenderer(BarDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
    }

    @Override
    protected void drawDataSet(Canvas canvas, IBarDataSet dataSet, int index) {
        Transformer transformer = mChart.getTransformer(dataSet.getAxisDependency());
        mBarBorderPaint.setColor(dataSet.getBarBorderColor());
        mBarBorderPaint.setStrokeWidth(Utils.convertDpToPixel(dataSet.getBarBorderWidth()));
        boolean drawBorder = dataSet.getBarBorderWidth() > 0f;
        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();
        if (mChart.isDrawBarShadowEnabled()) {
            mShadowPaint.setColor(dataSet.getBarShadowColor());
            float barWidth = mChart.getBarData().getBarWidth() / 2f;
            int count = Math.min((int) Math.ceil(dataSet.getEntryCount() * phaseX), dataSet.getEntryCount());
            for (int i = 0; i < count; i++) {
                float x = dataSet.getEntryForIndex(i).getX();
                shadowRect.left = x - barWidth;
                shadowRect.right = x + barWidth;
                transformer.rectValueToPixel(shadowRect);
                if (mViewPortHandler.isInBoundsLeft(shadowRect.right)) {
                    if (!mViewPortHandler.isInBoundsRight(shadowRect.left)) {
                        break;
                    }
                    shadowRect.top = mViewPortHandler.contentTop();
                    shadowRect.bottom = mViewPortHandler.contentBottom();
                    canvas.drawRect(shadowRect, mShadowPaint);
                }
            }
        }
        BarBuffer buffer = mBarBuffers[index];
        buffer.setPhases(phaseX, phaseY);
        buffer.setDataSet(index);
        buffer.setInverted(mChart.isInverted(dataSet.getAxisDependency()));
        buffer.setBarWidth(0.5f);
        buffer.feed(dataSet);
        transformer.pointValuesToPixel(buffer.buffer);
        boolean singleColor = dataSet.getColors().size() == 1;
        if (singleColor) {
            mRenderPaint.setColor(dataSet.getColor());
        }
        int entryIndex = 0;
        int index2 = 0;
        while (index2 < buffer.size()) {
            int right = index2 + 2;
            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[right])) {
                index2 += 4;
                entryIndex++;
                continue;
            }
            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[index2])) {
                return;
            }
            if (!singleColor) {
                mRenderPaint.setColor(dataSet.getColor(entryIndex));
            }
            canvas.drawRoundRect(new RectF(buffer.buffer[index2], buffer.buffer[index2 + 1],
                buffer.buffer[right], buffer.buffer[index2 + 3]),
                Utils.convertDpToPixel(2f), Utils.convertDpToPixel(2f), mRenderPaint);
            if (drawBorder) {
                canvas.drawRect(buffer.buffer[index2], buffer.buffer[index2 + 1], buffer.buffer[right],
                    buffer.buffer[index2 + 3], mBarBorderPaint);
            }
            index2 += 4;
            entryIndex++;
        }
    }
}
