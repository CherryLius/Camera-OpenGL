package cherry.android.camera;//package com.hele.hardware.analyser.camera;
//
///**
// * Created by Administrator on 2017/4/10.
// */
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Matrix;
//import android.media.Image;
//import android.media.MediaScannerConnection;
//import android.net.Uri;
//import android.os.Environment;
//import android.support.annotation.NonNull;
//import android.util.DisplayMetrics;
//
//import com.hele.hardware.analyser.util.FileUtils;
//import com.hele.hardware.analyser.util.HLog;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//public class ImageSaver implements Runnable {
//    private static final String TAG = "ImageSaver";
//
//    private Context mContext;
//    private Matrix mMatrix;
//    /**
//     * image data
//     */
//    private byte[] mData;
//    /**
//     * for camera2
//     */
//    private Image mImage;
//
//    private File mFile;
//
//    /**
//     * bitmap
//     */
//    private Bitmap mBitmap;
//
//    private ICamera mFunction;
//
//    public ImageSaver(Context context, byte[] data, ICamera function) {
//        init(context, function);
//        mData = data;
//    }
//
//    public ImageSaver(Context context, Image image, ICamera function) {
//        init(context, function);
//        mImage = image;
//    }
//
//    private void init(Context context, ICamera function) {
//        mContext = context;
//        mFunction = function;
//
//        Date date = new Date();
//        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
//        String fileName = format.format(date) + ".jpg";
//        File parentFile = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        mFile = new File(parentFile, fileName);
//        FileUtils.checkFileExist(mFile.getParentFile());
//    }
//
//
//    @Override
//    public void run() {
//        if (mImage != null) {
//            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
//            byte[] bytes = new byte[buffer.remaining()];
//            buffer.get(bytes);
//            mData = bytes;
//        } else {
//            if (mFunction.isFrontCamera()) {
//                mBitmap = rotateBitmap(BitmapFactory.decodeByteArray(mData, 0, mData.length));
//                ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
//                mData = bos.toByteArray();
//            }
//        }
//
//        if (mBitmap == null) {
//            mBitmap = decodeBitmap(mContext, mData);
//        }
//
//        if (mFunction.getCameraCallback() != null)
//            mFunction.getCameraCallback().onCaptured(mBitmap, mData);
//
//    }
//
//    private void saveImage() {
//        FileOutputStream outputStream = null;
//        try {
//            outputStream = new FileOutputStream(mFile);
//            outputStream.write(mData);
//            outputStream.flush();
//
//            fileScan(mFile.getAbsolutePath());
//            if (mFunction.getCameraCallback() != null) {
//                mFunction.getCameraCallback().onPictureSaved(mBitmap, mFile.getAbsolutePath());
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            HLog.e(TAG, "FileNotFound", e);
//        } catch (IOException e) {
//            e.printStackTrace();
//            HLog.e(TAG, "IOException", e);
//        } finally {
//            if (mImage != null)
//                mImage.close();
//            if (outputStream != null)
//                try {
//                    outputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//        }
//    }
//
//    private void fileScan(String... path) {
//        MediaScannerConnection.scanFile(mContext, path, null, new MediaScannerConnection.OnScanCompletedListener() {
//            @Override
//            public void onScanCompleted(String path, Uri uri) {
//                HLog.d(TAG, "scan file !!!!!!!!!!!!");
//            }
//        });
//    }
//
//    private Bitmap rotateBitmap(@NonNull Bitmap bm) {
//        if (mMatrix == null)
//            mMatrix = new Matrix();
//        mMatrix.reset();
//        mMatrix.postRotate(180);
//        mMatrix.postScale(-1, 1);
//        Bitmap ret = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), mMatrix, true);
//        return ret;
//    }
//
//    private static Bitmap decodeBitmap(Context context, byte[] bytes) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
//        HLog.d(TAG, "options: " + options.outWidth + "x" + options.outHeight);
//
//        DisplayMetrics dm = context.getResources().getDisplayMetrics();
//        int targetDensityDpi = dm.densityDpi;
//        int expectW = dm.widthPixels;
//        int expectH = dm.heightPixels;
//        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, expectW, expectH);
//
//        double xScale = options.outWidth / (float) expectW;
//        double yScale = options.outHeight / (float) expectH;
//        HLog.d(TAG, "xScale=" + xScale + ",yScale=" + yScale
//                + ",targetDensity=" + targetDensityDpi);
//
//        options.inTargetDensity = targetDensityDpi;
//        options.inJustDecodeBounds = false;
//        Bitmap ret = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
//        return ret;
//    }
//
//    private static int calculateInSampleSize(int outWidth, int outHeight, int expectW, int expectH) {
//        int inSampleSize = 1;
//        if (outWidth > expectW || outHeight > expectH) {
//            final int halfHeight = outWidth / 2;
//            final int halfWidth = outHeight / 2;
//
//            // Calculate the largest inSampleSize value that is a power of 2 and
//            // keeps both
//            // height and width larger than the requested height and width.
//            while ((halfHeight / inSampleSize) > expectH
//                    && (halfWidth / inSampleSize) > expectW) {
//                inSampleSize *= 2;
//            }
//        }
//        HLog.i(TAG, "inSampleSize=" + inSampleSize);
//        return inSampleSize;
//    }
//}
