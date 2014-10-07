/*
 * Copyright (C) 2014 Joseph D. Glandorf
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.glandorf1.joe.wsprnetviewer.app.data;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.glandorf1.joe.wsprnetviewer.app.R;

/**
 * TODO: document your custom view class.
 */
public class WsprNetCustomView extends View {

    private int mDrawBitmapWidth = 320; //!< Default width of view
    private int mDrawBitmapHeight = 180; //!< Default height of view
    private final float mGreenwichLong = (float)0.0000; // ° W
    private final float mGreenwichLat = (float)51.4800; // ° N // JO01al (612/1224,131/612) = (0.5, 0.214)

    //Images
    private BitmapFactory.Options mBmOptions;
    private Bitmap mMaidenheadBitmapOrig; //!<
    private Bitmap mMaidenheadBitmap; //!<

    //Drawing
    private Paint mBitmapPaint; //!< Paint for drawing bitmap
    private Paint mTxPaint, mTxPaintLine; //!< Paint for transmit station
    private Paint mRxPaint; //!< Paint for receive station
    private final float mStrokeWidth = 2;
    private final float mCircleRadius = 3;

    private float mTxLatitude = 0; //!< latitude
    private float mTxLongitude = 0; //!< longitude
    private float mRxLatitude = 0; //!< latitude
    private float mRxLongitude = 0; //!< longitude
    private float mAzimuth     = -1; //!< azimuth < 0 indicates not set yet

//    /**
//     * http://stackoverflow.com/questions/17861638/implementing-accessability-on-custom-view-gives-no-verbal-feedback
//     * The accessibility manager for this context. This is used to check the
//     * accessibility enabled state, as well as to send raw accessibility events.
//     */
//    private final AccessibilityManager mA11yManager;
    /** The parent context. */
    private final Context mContext;


    public WsprNetCustomView(Context context) {
        super(context);
        mContext = context;
        init(context); // init(null, 0);
    }

    public WsprNetCustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    public WsprNetCustomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init(context); // init(attrs, defStyle);
    }

    /*
     * Instantiate members, avoid instantiating anything in onDraw or onMeasure methods
     */
    private void init(Context context){

        Resources res = mContext.getResources();
//        // Keep a handle to the accessibility manager.
//        mA11yManager = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        // First decode with inJustDecodeBounds=true to check dimensions
        mBmOptions = new BitmapFactory.Options();
        mBmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, R.drawable.art_maidenhead, mBmOptions);
        mDrawBitmapWidth = mBmOptions.outWidth;
        mDrawBitmapHeight = mBmOptions.outHeight;
        mBmOptions.inJustDecodeBounds = false;
        mMaidenheadBitmapOrig = BitmapFactory.decodeResource(res, R.drawable.art_maidenhead, mBmOptions);
        mMaidenheadBitmap = mMaidenheadBitmapOrig;

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setDither(true);
        mBitmapPaint.setFilterBitmap(true);
        mTxPaint = new Paint();
        mTxPaint.setAntiAlias(true);
        mTxPaint.setDither(true);
        mTxPaint.setStyle(Paint.Style.STROKE);
        mTxPaint.setStrokeWidth(mStrokeWidth);
        mTxPaint.setColor(res.getColor(R.color.wspr_orange));

        mTxPaintLine = new Paint();
        mTxPaintLine.setAntiAlias(true);
        mTxPaintLine.setDither(true);
        mTxPaintLine.setStyle(Paint.Style.STROKE);
        mTxPaintLine.setStrokeWidth(mStrokeWidth);
        mTxPaintLine.setColor(res.getColor(R.color.wspr_orange));

        mRxPaint = new Paint();
        mRxPaint.setAntiAlias(true);
        mRxPaint.setDither(true);
        mRxPaint.setStyle(Paint.Style.STROKE);
        mRxPaint.setStrokeWidth(mStrokeWidth/2);
        mRxPaint.setColor(res.getColor(R.color.wspr_red));
    }


    /*
     * Draw to canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Clear canvas before redrawing
        canvas.drawColor(Color.TRANSPARENT);
        //Don't bother drawing anything until setLatLongAzimuth() gets called.
        if (mMaidenheadBitmap != null) {
            double sw = (double)mMaidenheadBitmap.getWidth();   //.getScaledWidth(canvas)??;
            double sh = (double)mMaidenheadBitmap.getHeight();   //.getScaledHeight(canvas)??;
            if ((mAzimuth >= 0.)) { // mAzimuth is -1 until setLatLongAzimuth() gets called.
                canvas.drawBitmap(mMaidenheadBitmap, 0, 0, null);
                float x, y;
                // For the transmitter, draw a circle with "rays" emanating from it.
                x = (float) (sw * ((180. + mTxLongitude) % 360.) / 360.);
                y = (float) (sh - (sh * (( 90. + mTxLatitude) % 180.) / 180.));
                canvas.drawCircle(x, y, mCircleRadius, mTxPaint);
                float[] pts = new float[32];
                for (int i = 0, a = 45; a < 360; i+=4, a+= 90) {
                    double rad = 3.14159 * (double)a/180.;
                    pts[i+0] = (float)(x + 1.5*(float)mCircleRadius*Math.cos(rad));
                    pts[i+1] = (float)(y + 1.5*(float)mCircleRadius*Math.sin(rad));
                    pts[i+2] = (float)(x + 2.5*(float)mCircleRadius*Math.cos(rad));
                    pts[i+3] = (float)(y + 2.5*(float)mCircleRadius*Math.sin(rad));
                }
                for (int i = 16, a = 0; a < 360; i+=4, a+= 90) {
                    double rad = 3.14159 * (double)a/180.;
                    pts[i+0] = (float)(x + 2.0*(float)mCircleRadius*Math.cos(rad));
                    pts[i+1] = (float)(y + 2.0*(float)mCircleRadius*Math.sin(rad));
                    pts[i+2] = (float)(x + 3.0*(float)mCircleRadius*Math.cos(rad));
                    pts[i+3] = (float)(y + 3.0*(float)mCircleRadius*Math.sin(rad));
                }
                canvas.drawLines(pts, mTxPaintLine);
                // For the receiver, draw some concentric circles.
                x = (float) (sw * ((180. + mRxLongitude) % 360.) / 360.);
                y = (float) (sh - (sh * (( 90. + mRxLatitude) % 180.) / 180.));
                canvas.drawCircle(x, y, mCircleRadius*(float)0.75, mRxPaint);
                canvas.drawCircle(x, y, mCircleRadius*(float)1.5, mRxPaint);
                canvas.drawCircle(x, y, mCircleRadius*(float)2.5, mRxPaint);
            }
        }
    } // onDraw()

    /*
     * Determining view size based on constraints from parent views
     *
     * (non-Javadoc)
     * @see android.view.View#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //Get size requested and size mode
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width, height = 0;

        //Determine Width
        switch(widthMode){
            case MeasureSpec.EXACTLY:
                width = widthSize;
                break;
            case MeasureSpec.AT_MOST:
                width = Math.min(mDrawBitmapWidth, widthSize);
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                width = mDrawBitmapWidth;
                break;
        }

        //Determine Height
        switch(heightMode){
            case MeasureSpec.EXACTLY:
                height = heightSize;
                break;
            case MeasureSpec.AT_MOST:
                height = Math.min(mDrawBitmapHeight, heightSize);
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                height = mDrawBitmapHeight;
                break;
        }

        setMeasuredDimension(width, height);
    }

    /*
     * Called after onMeasure, returning the actual size of the view before drawing.
     *
     * (non-Javadoc)
     * @see android.view.View#onSizeChanged(int, int, int, int)
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float sw = (float)w / (float)mDrawBitmapWidth;
        float sh = (float)h / (float)mDrawBitmapHeight;
        float s = sw < sh ? sw : sh;
        Matrix matrix = new Matrix();
        matrix.postScale(s, s);
        mMaidenheadBitmap = Bitmap.createBitmap(mMaidenheadBitmapOrig, 0, 0, mMaidenheadBitmapOrig.getWidth(), mMaidenheadBitmapOrig.getHeight(), matrix, true);
    }

    /*
     * Setter for latitude/longitude/azimuth
     */
    public void setLatLongAzimuth(double txLatitude, double txLongitude, double rxLatitude, double rxLongitude, double azimuth){
        mTxLatitude = (float)txLatitude;
        mTxLongitude = (float)txLongitude;
        mRxLatitude = (float)rxLatitude;
        mRxLongitude = (float)rxLongitude;
        mAzimuth     = (float)azimuth;
        //announceForAccessibilityCompat(sSnr);
        invalidate();
    }

}
