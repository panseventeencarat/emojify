package com.example.administrator.emojify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Administrator on 2019/4/15.
 */
 class BitmapUtils {
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";//provider的类名,表示授予 URI 临时访问权限
    /**
     * Resamples 方法会获取设备屏幕的高度和宽度（用像素表示），并重新取样传入的图片以与屏幕大小相符。
     *
     * @param context   :应用程序上下文
     * @param imagePath .要重新采样照片的路径
     * @return 重新取样传入与屏幕大小相符的图片
     */
    static Bitmap resamplePic(Context context, String imagePath) {
        //获取设备屏幕的高度和宽度信息
        DisplayMetrics metrics=new DisplayMetrics();
        WindowManager manager=(WindowManager) context.getSystemService(Context.WINDOW_SERVICE);//获取WindowManager服务
        manager.getDefaultDisplay().getMetrics(metrics);//getDefaultDisplay() 方法将取得的宽高维度存放于DisplayMetrics 对象中,将当前窗口的一些信息放在DisplayMetrics类中，
        int targetH=metrics.heightPixels;//屏幕高度像素
        int targetW=metrics.widthPixels;//屏幕宽度像素
        //获取原始图像的尺寸
        BitmapFactory.Options bmOptions=new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds=true;//并不会真的返回一个Bitmap给你，它仅仅会把它的宽，高取回来给你
        BitmapFactory.decodeFile(imagePath,bmOptions);//获取图片路径，图片高宽
        int photoW=bmOptions.outHeight;//图片高度
        int photoH=bmOptions.outWidth;//图片宽度
       //决定图像的缩放
        int scaleFactor=Math.min(photoW/targetW,photoH/targetH);//min() 方法可返回指定的数字中带有最低值的数字
       //将图像文件解码为位图大小以填充视图
        bmOptions.inJustDecodeBounds=false;//BitmapFactory返回bitmap
        bmOptions.inSampleSize=scaleFactor;

        return BitmapFactory.decodeFile(imagePath);
    }
    /**
     * Creates 该方法会在外部缓存目录下创建临时文件，并返回新的临时文件.
     *
     * @return 返回临时图片文件
     * @throws IOException Thrown if there is an error creating the file
     */
    static File createTempImageFile(Context context) throws IOException{
        String timeStamp=new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalCacheDir();//SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
        return File.createTempFile(imageFileName,".jpg",storageDir );//前缀，后缀，目录
    }
    /**
     * 该方法尝试删除传入路径下的图片。如果删除失败，则显示 Toast 消息。
     *
     * @param context   返回临时图片文件
     * @param imagePath 被删除照片的路径
     */
    static boolean deleteImageFile(Context context,String imagePath){
        //获得图片路径
        File imageFile=new File(imagePath);
        //删除图片
        boolean deleted=imageFile.delete();
        //若删除失败则弹出弹框
        if (!deleted){
            String errorMessage=context.getString(R.string.error);
            Toast.makeText(context,errorMessage,Toast.LENGTH_SHORT).show();
        }
        return deleted;
    }
    private static void galleryAddPic(Context context, String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }


    /**
     * Helper method for saving the image.
     *
     * @param context The application context.
     * @param image   The image to be saved.
     * @return The path of the saved image.
     */
    static String saveImage(Context context, Bitmap image) {

        String savedImagePath = null;

        // Create the new file in the external storage
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        File storageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        + "/Emojify");
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }

        // Save the new Bitmap
        if (success) {
            File imageFile = new File(storageDir, imageFileName);
            savedImagePath = imageFile.getAbsolutePath();
            try {
                OutputStream fOut = new FileOutputStream(imageFile);
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Add the image to the system gallery
            galleryAddPic(context, savedImagePath);

            // Show a Toast with the save location
            String savedMessage = context.getString(R.string.saved_message, savedImagePath);
            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show();
        }

        return savedImagePath;
    }

    /**
     * Helper method for sharing an image.
     *
     * @param context   The image context.
     * @param imagePath The path of the image to be shared.
     */
    static void shareImage(Context context, String imagePath) {
        // Create the share intent and start the share activity
        File imageFile = new File(imagePath);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        Uri photoURI = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, imageFile);
        shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI);
        context.startActivity(shareIntent);
    }
}

