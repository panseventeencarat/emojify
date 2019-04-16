package com.example.administrator.emojify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";//provider的类名,表示授予 URI 临时访问权限

    @BindView(R.id.image_view)ImageView mImageView;//绑定一个view；id为一个view 变量
    @BindView(R.id.emojify_button) Button mEmojifyButton;
    @BindView(R.id.share_button) FloatingActionButton mShareFab;
    @BindView(R.id.save_button) FloatingActionButton mSaveFab;
    @BindView(R.id.clear_button) FloatingActionButton mClearFab;
    @BindView(R.id.title_text_view) TextView mTitleTextView;

    private String mTempPhotoPath;

    private Bitmap mResultsBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);//绑定view
        Timber.plant(new Timber.DebugTree());  // 创建Timber
    }
    //使用onclick方法让emojifyMebutton启用相机应用
    @OnClick(R.id.emojify_button)
    public void emojifyMe(){
        //检查是否授予外部存储读写权限
         if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
             ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_STORAGE_PERMISSION);
         }//如果未授予权限，则请求权限
        else{
             launchCamera();
         }//如果已授予，就开启相机
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //当请求外部存储读写权限时调用这个函数,grantResults表示调回结果，requestCode表示请求码
        switch (requestCode){
            case REQUEST_STORAGE_PERMISSION:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    launchCamera();//获得权限，开启相机
                }
                else{
                    Toast.makeText(this,R.string.permission_denied,Toast.LENGTH_SHORT).show();//若获取失败，则跳出弹窗
                }
                break;
            }
        }
    }
    private void launchCamera(){
        //使用ACTION_IMAGE_CAPTURE隐式intent，实现用本地相机拍照
        Intent takePictureIntent =new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //确保有相机来处理intent
        if (takePictureIntent.resolveActivity(getPackageManager())!=null){
           //使用BitmapUtils类创建一个临时存放照片的文件
            File photoFile=null;
            try{
                photoFile=BitmapUtils.createTempImageFile(this);
            }
            catch (IOException ex){
                ex.printStackTrace();//创建文件时出错
            }
            //仅在创建文件成功是继续进行操作
            if(photoFile!=null){
                //获得临时文件路径
                mTempPhotoPath=photoFile.getAbsolutePath();
                //获得图片文件的URI
                Uri photoURI= FileProvider.getUriForFile(this,FILE_PROVIDER_AUTHORITY,photoFile);
                //将uri传入intent，使相机可以存储图片
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                //从相机获取结果，是否捕获了照片
                startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //如果以上图像捕获被调用并且成功
      if (requestCode==REQUEST_IMAGE_CAPTURE && requestCode ==RESULT_OK){
          //处理图像并把它放到textview中
          processAndSetImage();
      }
        else {
          //删除临时文件
          BitmapUtils.deleteImageFile(this,mTempPhotoPath);
      }
    }
    //用于处理捕获的图像，并且把它放到Textview中
    private void processAndSetImage() {

        // 切换视图可见性，把隐藏的浮动的按钮同时显示出来
        mEmojifyButton.setVisibility(View.GONE);
        mTitleTextView.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.VISIBLE);
        mClearFab.setVisibility(View.VISIBLE);

        // 对保存的图像重新取样以适应 imageview
        mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);
        //检测面部并覆盖emoji表情
        mResultsBitmap = Emojifier.detectFacesandOverlayEmoji(this, mResultsBitmap);
        // 将生成的图像位图设置为imageview
        mImageView.setImageBitmap(mResultsBitmap);
    }
    //保存按钮的点击事件
    @OnClick(R.id.save_button)
    public void saveMe(){
        //删除临时图片文件
        BitmapUtils.deleteImageFile(this,mTempPhotoPath);
        //在外部存储器中保存处理后的图片
        BitmapUtils.saveImage(this, mResultsBitmap );
    }
//分享按钮的点击事件
    @OnClick(R.id.share_button)
    public void shareMe(){
        //删除临时图片文件
        BitmapUtils.deleteImageFile(this,mTempPhotoPath);
        //在外部存储器中保存处理后的图片
        BitmapUtils.saveImage(this, mResultsBitmap );
        //分享图片
        BitmapUtils.shareImage(this,mTempPhotoPath);
    }
    //删除按钮的点击事件
    @OnClick(R.id.clear_button)
    public void clearImage() {
        //清楚照片并改变视图可见性
        mImageView.setImageResource(0);
        mEmojifyButton.setVisibility(View.VISIBLE);
        mTitleTextView.setVisibility(View.VISIBLE);
        mSaveFab.setVisibility(View.GONE);
        mShareFab.setVisibility(View.GONE);
        mClearFab.setVisibility(View.GONE);
        //删除临时图片文件
        BitmapUtils.deleteImageFile(this,mTempPhotoPath);

    }
}
