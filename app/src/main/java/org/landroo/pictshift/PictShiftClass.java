package org.landroo.pictshift;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;

public class PictShiftClass
{
	private static final int SHIFT_STEP = 2;

	private boolean debug = false;
	
	private int displayWidth = 0; // display width
	private int displayHeight = 0; // display height
	private Paint paint;
	private Bitmap bitmap;

	public ArrayList<Tile> tiles = new ArrayList<Tile>();
	public int selTile = -1;
	public int empTile = -1;
	public int tileXNum = 3;
	public int tileYNum = 4;
	public int stepCnt = 0;
	public boolean bEnd = false;
	
	// tile data class
	public class Tile
	{
		public float tilPosX = 0;				// actual position
		public float tilPosY = 0;
		public BitmapDrawable bitmapDrawable;	// bitmap
		public Bitmap bitmap;
		public boolean visible = true;

		public float desPosX = -1;				// scroll destination
		public float desPosY = -1;

		public float oriPosX = 0;				// original position
		public float oriPosY = 0;
	}
	
	// constructor
	public PictShiftClass(int width, int height, Bitmap bmp)
	{
		this.stepCnt = 0; 
		this.bEnd = false;
		this.tiles.clear();
		
		this.displayWidth = width;
		this.displayHeight = height;
		
		if(debug == false)
			bitmap = Bitmap.createScaledBitmap(bmp, displayWidth, displayHeight, true);
		
		paint = new Paint();
		paint.setTextSize(48);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		paint.setFakeBoldText(true);
		paint.setShadowLayer(3, 0, 0, Color.BLACK);
		
		initBitmaps(displayWidth, displayHeight);
	}

	// create tiles
	private void initBitmaps(int width, int height)
	{
		int i = 1;
		for (int y = 0; y < tileYNum; y++)
		{
			for (int x = 0; x < tileXNum; x++)
			{
				Tile tile = new Tile();
				tile.tilPosX = x * (width / tileXNum);
				tile.tilPosY = y * (height / tileYNum);
				tile.oriPosX = tile.tilPosX;
				tile.oriPosY = tile.tilPosY;

				if (debug) tile.bitmap = bitmapNum(width / tileXNum, height / tileYNum, i++);
				else tile.bitmap = bitmapPict(width / tileXNum, height / tileYNum, i, x, y);

				tile.bitmapDrawable = new BitmapDrawable(tile.bitmap);
				tile.bitmapDrawable.setBounds(0, 0, width / tileXNum, height / tileYNum);

				tiles.add(tile);
			}
		}

		tiles.get(tiles.size() - 1).visible = false;
		empTile = tiles.size() - 1;

		changeUpTiles(this.tiles.size() * 5);

		return;
	}

	// return a tile of the position
	private Bitmap bitmapPict(int width, int height, int iNo, int x, int y)
	{

		Bitmap part = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(part);

		Rect src = new Rect(x * width, y * height, x * width + width, y * height + height);
		Rect dst = new Rect(0, 0, width, height);
		canvas.drawBitmap(bitmap, src, dst, paint);

		return part;
	}

	// create tile with numbers
	private Bitmap bitmapNum(int width, int height, int iNo)
	{
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
		bitmap.eraseColor(Color.TRANSPARENT);

		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setTextSize(width / 2);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		paint.setFakeBoldText(true);
		paint.setShadowLayer(3, 0, 0, Color.BLACK);

		String sText = "" + iNo;
		float tw = paint.measureText(sText);
		float x = (width - tw) / 2;
		float y = (height - width / 2) / 2 + width / 2;

		canvas.drawText(sText, x, y, paint);

		Path path = new Path();
		path.moveTo(0, 0);
		path.lineTo(width, 0);
		path.lineTo(width, height);
		path.lineTo(0, height);
		path.lineTo(0, 0);

		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(3);
		paint.setColor(Color.WHITE);

		canvas.drawPath(path, paint);

		return bitmap;
	}

	// select a tile by screen position
	public boolean changeTile(float x, float y)
	{
		boolean bOK = false;
		if (selTile != -1) return bOK;

		// identify selected tile
		int iNo = 0;
		for (Tile tile : tiles)
		{
			// selected tile
			if (tile.visible && tile.tilPosX < x && tile.tilPosX + tile.bitmap.getWidth() > x && tile.tilPosY < y
					&& tile.tilPosY + tile.bitmap.getHeight() > y) break;
			iNo++;
		}

		// check surrounding tiles
		if (iNo != tiles.size())
		{
			Tile selTile = tiles.get(iNo);
			Tile empTile = tiles.get(this.empTile);

			// empty is left or right
			if ((selTile.tilPosX - selTile.bitmap.getWidth() == empTile.tilPosX || selTile.tilPosX + selTile.bitmap.getWidth() == empTile.tilPosX)
					&& selTile.tilPosY == empTile.tilPosY)
			{
				selTile.desPosX = empTile.tilPosX;
				empTile.desPosX = selTile.tilPosX;
				this.selTile = iNo;
				bOK = true;
				stepCnt++;
			}

			// empty is above or under
			if ((selTile.tilPosY - selTile.bitmap.getHeight() == empTile.tilPosY || selTile.tilPosY
					+ selTile.bitmap.getHeight() == empTile.tilPosY)
					&& selTile.tilPosX == empTile.tilPosX)
			{
				selTile.desPosY = empTile.tilPosY;
				empTile.desPosY = selTile.tilPosY;
				this.selTile = iNo;
				bOK = true;
				stepCnt++;
			}
		}

		return bOK;
	}

	// scroll the selected tile to the end position
	public boolean stepTile(Tile tile)
	{
		float step = SHIFT_STEP;
		if (tile.desPosX != -1)
		{
			if (tile.desPosX > tile.tilPosX)
			{
				if (tile.desPosX <= tile.tilPosX + step)
				{
					selTile = -1;
					tile.tilPosX = tile.desPosX;
					tile.desPosX = -1;
					bEnd = checkEnd();
				}
				else tile.tilPosX += step;
			}
			else if (tile.desPosX < tile.tilPosX)
			{
				if (tile.desPosX >= tile.tilPosX - step)
				{
					selTile = -1;
					tile.tilPosX = tile.desPosX;
					tile.desPosX = -1;
					bEnd = checkEnd();
				}
				else tile.tilPosX -= step;
			}
		}

		if (tile.desPosY != -1)
		{
			if (tile.desPosY > tile.tilPosY)
			{
				if (tile.desPosY <= tile.tilPosY + step)
				{
					selTile = -1;
					tile.tilPosY = tile.desPosY;
					tile.desPosY = -1;
					bEnd = checkEnd();
				}
				else tile.tilPosY += step;
			}
			else if (tile.desPosY < tile.tilPosY)
			{
				if (tile.desPosY >= tile.tilPosY - step)
				{
					selTile = -1;
					tile.tilPosY = tile.desPosY;
					tile.desPosY = -1;
					bEnd = checkEnd();
				}
				else tile.tilPosY -= step;
			}
		}
		
		return bEnd;
	}

	// check the tiles positions equals the original position  
	private boolean checkEnd()
	{
		if (bEnd) return true;
		boolean bOk = false;
		int iCnt = 1;
		for (Tile tile : tiles)
			if (tile.tilPosX == tile.oriPosX && tile.tilPosY == tile.oriPosY) iCnt++;
		if (iCnt == tiles.size()) bOk = true;
		if (bOk) tiles.get(empTile).visible = true;

		return bOk;
	}

	// random change up two tile
	private void changeUpTiles(int iNum)
	{
		Tile empTile = tiles.get(this.empTile);
		int ir;
		for (int i = 0; i < iNum; i++)
		{
			for (Tile tile : tiles)
			{
				ir = random(1, 10, 1);
				if ((tile.tilPosX - tile.bitmap.getWidth() == empTile.tilPosX || tile.tilPosX + tile.bitmap.getWidth() == empTile.tilPosX)
						&& tile.tilPosY == empTile.tilPosY && ir < 5)
				{
					float x = tile.tilPosX;
					float y = tile.tilPosY;
					tile.tilPosX = empTile.tilPosX;
					tile.tilPosY = empTile.tilPosY;
					empTile.tilPosX = x;
					empTile.tilPosY = y;
				}
				ir = random(1, 10, 1);
				if ((tile.tilPosY - tile.bitmap.getHeight() == empTile.tilPosY || tile.tilPosY + tile.bitmap.getHeight() == empTile.tilPosY)
						&& tile.tilPosX == empTile.tilPosX && ir < 5)
				{
					float x = tile.tilPosX;
					float y = tile.tilPosY;
					tile.tilPosX = empTile.tilPosX;
					tile.tilPosY = empTile.tilPosY;
					empTile.tilPosX = x;
					empTile.tilPosY = y;
				}
			}
		}
	}

	// random number
	public int random(int nMinimum, int nMaximum, int nRoundToInterval)
	{
		if (nMinimum > nMaximum)
		{
			int nTemp = nMinimum;
			nMinimum = nMaximum;
			nMaximum = nTemp;
		}

		int nDeltaRange = (nMaximum - nMinimum) + (1 * nRoundToInterval);
		double nRandomNumber = Math.random() * nDeltaRange;

		nRandomNumber += nMinimum;

		int nRet = (int) (Math.floor(nRandomNumber / nRoundToInterval) * nRoundToInterval);

		return nRet;
	}

}
