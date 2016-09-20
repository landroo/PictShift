package org.landroo.pictshift;

/*
 Picture shifter.
 */

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import org.landroo.pictshift.R;
import org.landroo.ui.UI;
import org.landroo.ui.UIInterface;

public class PictShiftMainActivity extends Activity implements UIInterface {
    private static final String TAG = "PictShift";
    private static final int SWIPE_INTERVAL = 10;
    private static final int OFF_MARGIN = 0;

    private UI ui = null;

    private int displayWidth = 0; // display width
    private int displayHeight = 0; // display height

    private PictShiftView pictShiftView; // the view

    private int sX = 0;
    private int sY = 0;
    private int mX = 0;
    private int mY = 0;

    private float xPos = 0;
    private float yPos = 0;

    public float pictureWidth;
    public float pictureHeight;
    public float origWidth;
    public float origHeight;

    private Timer swipeTimer = null;
    private float swipeDistX = 0;
    private float swipeDistY = 0;
    private float swipeVelocity = 0;
    private float swipeSpeed = 0;

    private float zoomSize = 0;

    private Timer shiftTimer;

    private Paint paint;

    private PictShiftClass shiftClass;

    private boolean start = true;
    private BitmapDrawable drawable;

    private String result;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) showDlg("Success!");
        }
    };

    private class PictShiftView extends View {
        private String sTitle;
        private float titleWidth;

        public PictShiftView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            try {
                if (start) {
                    drawable.draw(canvas);
                } else {
                    for (PictShiftClass.Tile tile : shiftClass.tiles) {
                        if (tile.visible) {
                            canvas.translate((tile.tilPosX * (pictureWidth / origWidth)) + xPos,
                                    (tile.tilPosY * (pictureHeight / origHeight)) + yPos);
                            tile.bitmapDrawable.draw(canvas);
                            canvas.translate((-tile.tilPosX * (pictureWidth / origWidth)) - xPos,
                                    (-tile.tilPosY * pictureHeight / origHeight) - yPos);
                        }
                    }
                    sTitle = "" + shiftClass.stepCnt;
                    titleWidth = paint.measureText(sTitle);
                    canvas.drawText(sTitle, (displayWidth - titleWidth) / 2, 60, paint);
                }
            } catch (Exception ex) {
                Log.i(TAG, ex.getMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Display display = getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();

        pictShiftView = new PictShiftView(this);
        setContentView(pictShiftView);

        ui = new UI(this);

        initApp();

        paint = new Paint();
        paint.setTextSize(48);
        paint.setColor(0xfffce98c);
        paint.setStyle(Style.FILL);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
        paint.setShadowLayer(3, 0, 0, Color.BLACK);

        swipeTimer = new Timer();
        swipeTimer.scheduleAtFixedRate(new SwipeTask(), 0, SWIPE_INTERVAL);

        shiftTimer = new Timer();
        shiftTimer.scheduleAtFixedRate(new shiftTask(), 0, 1);
    }

    private void initApp() {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.picture);
            drawable = new BitmapDrawable(bitmap);
            drawable.setBounds(0, 0, displayWidth, displayHeight);
            shiftClass = new PictShiftClass(displayWidth, displayHeight, bitmap);

            pictureWidth = displayWidth;
            pictureHeight = displayHeight;
            origWidth = pictureWidth;
            origHeight = pictureHeight;

            pictShiftView.invalidate();
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory error in new page!");
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pict_shift_main, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return ui.tapEvent(event);
    }

    @Override
    public void onDown(float x, float y) {
        sX = (int) x;
        sY = (int) y;

        swipeVelocity = 0;

        pictShiftView.postInvalidate();
    }

    @Override
    public void onUp(float x, float y) {
        pictShiftView.postInvalidate();
    }

    @Override
    public void onTap(float x, float y) {
        float bx = (x - xPos) * (origWidth / pictureWidth);
        float by = (y - yPos) * (origHeight / pictureHeight);

        //Log.i(TAG, "" + bx + "  " + x + " " + (origWidth / pictureWidth) + " " + xPos + " ");

        start = false;

        if (shiftClass.bEnd == false) shiftClass.changeTile(bx, by);
//		else this.openOptionsMenu();
    }

    @Override
    public void onHold(float x, float y) {
//		this.openOptionsMenu();
    }

    @Override
    public void onMove(float x, float y) {
        mX = (int) x;
        mY = (int) y;

        float dx = mX - sX;
        float dy = mY - sY;

        // picture bigger than display
        if (pictureWidth >= displayWidth) {
            if (xPos + dx < displayWidth - (pictureWidth - OFF_MARGIN) || xPos + dx > -OFF_MARGIN)
                dx = 0;
            if (yPos + dy < displayHeight - (pictureHeight - OFF_MARGIN) || yPos + dy > -OFF_MARGIN)
                dy = 0;
        } else {
            if (xPos + dx > displayWidth - (pictureWidth - OFF_MARGIN) || xPos + dx < -OFF_MARGIN)
                dx = 0;
            if (yPos + dy > displayHeight - (pictureHeight - OFF_MARGIN) || yPos + dy < -OFF_MARGIN)
                dy = 0;
        }

        xPos += dx;
        yPos += dy;

        sX = (int) mX;
        sY = (int) mY;

        pictShiftView.postInvalidate();

        return;
    }

    @Override
    public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2) {
        boolean bOK = false;

        float bx = (x2 - xPos) * (origWidth / pictureWidth);
        float by = (y2 - yPos) * (origHeight / pictureHeight);
        if (shiftClass.bEnd == false) bOK = shiftClass.changeTile(bx, by);

        if (bOK == false) {
            swipeDistX = x2 - x1;
            swipeDistY = y2 - y1;
            swipeSpeed = 1;
            swipeVelocity = velocity;

            pictShiftView.postInvalidate();
        }

        return;
    }

    @Override
    public void onDoubleTap(float x, float y) {
        pictureWidth = origWidth;
        pictureHeight = origHeight;

        xPos = (displayWidth - pictureWidth) / 2;
        yPos = (displayHeight - pictureHeight) / 2;

        for (PictShiftClass.Tile tile : shiftClass.tiles)
            tile.bitmapDrawable.setBounds(0, 0, (int) pictureWidth / shiftClass.tileXNum, (int) pictureHeight / shiftClass.tileYNum);

        pictShiftView.postInvalidate();

        return;
    }

    @Override
    public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff) {
        int dist = (int) distance * 5;
        switch (mode) {
            case 1:
                zoomSize = dist;
                break;
            case 2:
                int diff = (int) (dist - zoomSize);
                float sizeNew = (float)Math.sqrt(pictureWidth * pictureWidth + pictureHeight * pictureHeight);
                float sizeDiff = 100 / (sizeNew / (sizeNew + diff));
                float newSizeX = pictureWidth * sizeDiff / 100;
                float newSizeY = pictureHeight * sizeDiff / 100;

                // zoom between min and max value
                if (newSizeX > origWidth / 4 && newSizeX < origWidth * 10) {
                    for (PictShiftClass.Tile tile : shiftClass.tiles)
                        tile.bitmapDrawable.setBounds(0, 0, (int) (newSizeX / shiftClass.tileXNum), (int) (newSizeY / shiftClass.tileYNum));

                    zoomSize = dist;

                    float diffX = newSizeX - pictureWidth;
                    float diffY = newSizeY - pictureHeight;
                    float xPer = 100 / (pictureWidth / (Math.abs(xPos) + mX)) / 100;
                    float yPer = 100 / (pictureHeight / (Math.abs(yPos) + mY)) / 100;

                    xPos -= diffX * xPer;
                    yPos -= diffY * yPer;

                    pictureWidth = newSizeX;
                    pictureHeight = newSizeY;

                    if (pictureWidth > displayWidth || pictureHeight > displayHeight) {
                        if (xPos > 0) xPos = 0;
                        if (yPos > 0) yPos = 0;

                        if (xPos + pictureWidth < displayWidth) xPos = displayWidth - pictureWidth;
                        if (yPos + pictureHeight < displayHeight)
                            yPos = displayHeight - pictureHeight;
                    } else {
                        if (xPos <= 0) xPos = 0;
                        if (yPos <= 0) yPos = 0;

                        if (xPos + pictureWidth > displayWidth) xPos = displayWidth - pictureWidth;
                        if (yPos + pictureHeight > displayHeight)
                            yPos = displayHeight - pictureHeight;
                    }

                    // Log.i(TAG, "" + xPos + " " + yPos);
                }
                break;
            case 3:
                zoomSize = 0;
                break;
        }

        pictShiftView.postInvalidate();

        return;
    }

    @Override
    public void onRotate(int mode, float x, float y, float angle) {
    }

    class SwipeTask extends TimerTask {
        public void run() {
            if (swipeVelocity > 0) {
                float dist = (float)Math.sqrt(swipeDistY * swipeDistY + swipeDistX * swipeDistX);
                float x = xPos - (float) ((swipeDistX / dist) * (swipeVelocity / 10));
                float y = yPos - (float) ((swipeDistY / dist) * (swipeVelocity / 10));

                if ((pictureWidth > displayWidth) && (x < displayWidth - pictureWidth || x > 0)
                        || ((pictureWidth <= displayWidth) && (x > displayWidth - pictureWidth || x < 0))) {
                    swipeDistX *= -1;
                    swipeSpeed += .1;
                }

                if ((pictureHeight > displayHeight) && (y < displayHeight - pictureHeight || y > 0)
                        || ((pictureHeight <= displayHeight) && (y > displayHeight - pictureHeight || y < 0))) {
                    swipeDistY *= -1;
                    swipeSpeed += .1;
                }

                xPos -= (float) ((swipeDistX / dist) * (swipeVelocity / 10));
                yPos -= (float) ((swipeDistY / dist) * (swipeVelocity / 10));

                swipeVelocity -= swipeSpeed;
                swipeSpeed += .0001;

                pictShiftView.postInvalidate();
            }
            return;
        }
    }

    class shiftTask extends TimerTask {
        public void run() {
            if (shiftClass.selTile != -1) {
                shiftClass.stepTile(shiftClass.tiles.get(shiftClass.selTile));
                boolean bEnd = shiftClass.stepTile(shiftClass.tiles.get(shiftClass.empTile));

                pictShiftView.postInvalidate();

                if (bEnd) handler.sendEmptyMessage(0);
            }
        }
    }

    @Override
    public void onFingerChange() {
    }

    private void showDlg(String sText) {
        Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(sText);
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bundle bundle = new Bundle();
                bundle.putString("result", result + "/" + shiftClass.stepCnt);

                /*Intent intent = new Intent(PictShiftActivity.this, SampleIdentSplash.class);
                intent.putExtra("bundle", bundle);
                PictShiftActivity.this.startActivity(intent);
                PictShiftActivity.this.finish();*/
            }
        });
        builder.setNegativeButton("Again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                initApp();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

}
