package ibox.pro.sdk.external.example;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

public class BitmapUtils {

	public static Bitmap decodeBitmap(String realUri, int width, int height) {
        Bitmap b = null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(realUri, o);

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = calculateInSampleSize(o, width, height);

        b = BitmapFactory.decodeFile(realUri, o2);
        return b;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int longSize, int shortSize) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (width > height) {
            if (height > shortSize || width > longSize) {
                inSampleSize = (int) Math.round((float) height / (float) shortSize);
            }
        } else {
            if (width > shortSize || height > longSize) {
                inSampleSize = (int) Math.round((float) width / (float) shortSize);
            }
        }

        return inSampleSize;
    }
    
    public static Bitmap getScaledBitmap(Bitmap b, int width, int height, boolean filter) {
        Bitmap img = Bitmap.createScaledBitmap(b, width, height, filter);
        if (img != b)
        	b.recycle();
        img = img.copy(Bitmap.Config.RGB_565, false);
        return img;
    }

    public static Bitmap compressedBitmap(String realUri, int width, int height) {
        Bitmap bufferBitmap = null;
        try {
            bufferBitmap = decodeBitmap(realUri, width, height);
            ExifInterface exif = new ExifInterface(realUri);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotate = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            int mWidth = bufferBitmap.getWidth();
            int mHeight = bufferBitmap.getHeight();

            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.preRotate(rotate);
                bufferBitmap = Bitmap.createBitmap(bufferBitmap, 0, 0, mWidth, mHeight, matrix, false);
                bufferBitmap = bufferBitmap.copy(Bitmap.Config.ARGB_8888, true);
                mWidth = bufferBitmap.getWidth();
                mHeight = bufferBitmap.getHeight();
            }

            if (mWidth > height || mHeight > height) {
                boolean portrait = mWidth > mHeight;

                double kWidth = 1.0 * mWidth / (portrait ? width : height);
                double kHeight = 1.0 * mHeight / (portrait ? height : width);

                double k = Math.max(kWidth, kHeight);

                int w = (int) Math.round(1.0 * mWidth / k);
                int h = (int) Math.round(1.0 * mHeight / k);

                Bitmap bufferBitmap2 = getScaledBitmap(bufferBitmap, w, h, true);                
                
               bufferBitmap.recycle();
               bufferBitmap = bufferBitmap2;
            }
        } catch (Exception e) {
        }

        return bufferBitmap;
    }

}
