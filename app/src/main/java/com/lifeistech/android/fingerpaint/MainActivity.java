package com.lifeistech.android.fingerpaint;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;

public class MainActivity extends ActionBarActivity implements View.OnTouchListener {
    Canvas mCanvas;
    //テキストやビットマップを描写するための色情報とスタイル情報を保持するPaintクラスを宣言
    Paint mPaint;
    Path mPath;
    Bitmap mBitmap;
    ImageView mImageView;
    AlertDialog.Builder mAlertBuilder;
    float x1,y1;
    int width,height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Viewの貼り付け
        setContentView(R.layout.activity_main);
        //フィールドmImageViewにimageViewの参照をmImageiewに代入
        mImageView=(ImageView)findViewById(R.id.imageView);
        //OnTouchListenerを呼んでリスナを登録
        mImageView.setOnTouchListener(this);

        //android.graphics.Paintクラスのインスタンス化
        mPaint= new Paint();
        //線の幅を設定
        mPaint.setStrokeWidth(5.0f);
        //グラフィックのスタイルを設定
        mPaint.setStyle(Paint.Style.STROKE);
        //線と線のつなぎ目を丸く設定
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        //線の端を丸く設定
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        //Pathのインスタンスを生成
        mPath=new Path();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mBitmap == null) {
            //mImageViewの幅と高さに合わせたBitmap画像を作る
            //WidthとHeightを取得する
            width= mImageView.getWidth();
            height= mImageView.getHeight();

            mBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);

            //新しいCanvasにその保存先のBitmapをセット
            mCanvas= new Canvas(mBitmap);
            //画像を白く塗りつぶす
            mCanvas.drawColor(Color.WHITE);
            //ImageViewにビットマップを表示
            mImageView.setImageBitmap(mBitmap);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x=event.getX();
        float y=event.getY();

        switch (event.getAction()) {
            //指が画面に触れた時
            case MotionEvent.ACTION_DOWN:
                mPath.reset();
                mPath.moveTo(x, y);
                break;
            //画面をドラッグしている時
            case MotionEvent.ACTION_MOVE:
                //指が画面から離れた時
            case MotionEvent.ACTION_UP:
                //パスをつなげる
                mPath.quadTo(x1, y1, x, y);
                //Canvasに今のパスを描く
                mCanvas.drawPath(mPath, mPaint);
                break;
        }
        //触った場所を記憶する
        x1= x;
        y1= y;
        //表示を更新する
        mImageView.setImageBitmap(mBitmap);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi =getMenuInflater();
        mi.inflate(R.menu.menu_main,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    //メニューが押された時の処理
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                save();
                break;
            case R.id.menu_open:
                Intent intent= new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(intent, 0);
                break;
            case R.id.menu_color_change:
                final String[] items=getResources().getStringArray(R.array.ColorNames);
                final int[] colors=getResources().getIntArray(R.array.ColorValues);
                mAlertBuilder=new AlertDialog.Builder(this);
                mAlertBuilder.setTitle(R.string.menu_color_change);
                mAlertBuilder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        mPaint.setColor(colors[item]);
                    }
                });
                mAlertBuilder.show();
                break;
            case R.id.menu_new:
                mAlertBuilder=new AlertDialog.Builder(this);
                mAlertBuilder.setTitle(R.string.menu_new);
                mAlertBuilder.setMessage("作業内容が破棄されます。よろしいですか？");
                mAlertBuilder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int which) {
                                mCanvas.drawColor(Color.WHITE);
                                mImageView.setImageBitmap(mBitmap);
                            }
                        });
                mAlertBuilder.setNegativeButton("キャンセル",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int which) {
                                //キャンセルボタンが押された時の処理
                            }
                        });
                mAlertBuilder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        Uri uri=data.getData();
        try {
            mBitmap=loadImage(uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCanvas= new Canvas(mBitmap);
        mImageView.setImageBitmap(mBitmap);
    }

    Bitmap loadImage(Uri uri) throws IOException{
        boolean landscape= false;
        Bitmap bm;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds= true;
        InputStream is= getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(is,null,options);
        is.close();
        int oh = options.outHeight;
        int ow = options.outWidth;
        if (ow > oh) {
            landscape=true;
            oh=options.outWidth;
            ow=options.outHeight;
        }

        options.inJustDecodeBounds= false;
        options.inSampleSize=Math.max(ow/width,oh/height);
        InputStream is2=getContentResolver().openInputStream(uri);
        bm=BitmapFactory.decodeStream(is2,null,options);
        is2.close();
        if (landscape) {
            Matrix matrix= new Matrix();
            matrix.setRotate(90.0f);
            bm=Bitmap.createBitmap(bm,0,0,
                    bm.getWidth(),bm.getHeight(),matrix,false);
        }
        bm=Bitmap.createScaledBitmap(bm,width,(int)(width*((double)oh / (double)ow)),false);
            Bitmap offBitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas offCanvas=new Canvas(offBitmap);
        offCanvas.drawBitmap(bm,0,(height - bm.getHeight()) /2, null);
        bm=offBitmap;
        return bm;
    }

    void save() {
        //SharedPreferencesオブジェクトを得る
        SharedPreferences prefs=getSharedPreferences("FingerPaintPreferences",MODE_PRIVATE);
        //SharedPreferencesよりデータを取得
        int imageNumber=prefs.getInt("imageNumber",1);
        File file =null;
        //DecimalFormatオブジェクトの生成を行う(10進数)
        DecimalFormat form =new DecimalFormat("0000");
        //SDカードへ書き込む
        String dirPath= Environment.getExternalStorageDirectory()+"/FingerPaint";
        File outDir=new File(dirPath);

        if (!outDir.exists())outDir.mkdir();
            do {
                file=new File(dirPath+"img"+form.format(imageNumber)+".png");
                imageNumber ++;
            } while (file.exists());
        if (writeImage(file)) {
            scanMedia(file.getPath());
            //Editorの設定
            SharedPreferences.Editor editor=prefs.edit();
            //Editorに値を代入
            editor.putInt("imageNumber",imageNumber +1);
            //データの保存
            editor.commit();
        }
    }

    boolean writeImage(File file) {
        try {
            FileOutputStream fo=new FileOutputStream(file);
            mBitmap.compress(Bitmap.CompressFormat.PNG,100,fo);
            fo.flush();
            fo.close();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    MediaScannerConnection mc;
    void scanMedia(final String fp){
        mc=new MediaScannerConnection(this,
                new MediaScannerConnection.MediaScannerConnectionClient(){
             public void onScanCompleted(String path, Uri uri) {
                 mc.disconnect();
             }

             public void onMediaScannerConnected() {
                 mc.scanFile(fp, "image/*");
             }
                }
        );
        mc.connect();
    }
}
