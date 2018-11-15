package com.monke.monkeybook.view.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.monke.basemvplib.impl.IPresenter;
import com.monke.monkeybook.MApplication;
import com.monke.monkeybook.R;
import com.monke.monkeybook.base.MBaseActivity;
import com.monke.monkeybook.widget.modialog.MoProgressHUD;

import java.util.Hashtable;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.monke.monkeybook.update.UpdateChecker;
/**
 * Created by GKF on 2017/12/15.
 * 关于
 */

public class AboutActivity extends MBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tv_version)
    TextView tvVersion;
    @BindView(R.id.vw_version)
    CardView vwVersion;
    @BindView(R.id.tv_donate)
    TextView tvDonate;
    @BindView(R.id.vw_donate)
    CardView vwDonate;
    CardView vwScoring;
    @BindView(R.id.tv_disclaimer)
    TextView tvDisclaimer;
    @BindView(R.id.vw_disclaimer)
    CardView vwDisclaimer;
    @BindView(R.id.ll_content)
    LinearLayout llContent;
    @BindView(R.id.tv_update)
    TextView tvUpdate;
    @BindView(R.id.vw_update)
    CardView vwUpdate;
    @BindView(R.id.tv_qq)
    TextView tvQq;
    @BindView(R.id.vw_qq)
    CardView vwQq;
    @BindView(R.id.tv_app_summary)
    TextView tvAppSummary;
    @BindView(R.id.tv_update_log)
    TextView tvUpdateLog;
    @BindView(R.id.vw_update_log)
    CardView vwUpdateLog;
    @BindView(R.id.tv_faq)
    TextView tvFaq;
    @BindView(R.id.vw_faq)
    CardView vwFaq;
    @BindView(R.id.tv_share)
    TextView tvShare;
    @BindView(R.id.vw_share)
    CardView vwShare;

    private MoProgressHUD moProgressHUD;
    private String qq = "365650856";

    public static void startThis(Context context) {
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected IPresenter initInjector() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onCreateActivity() {
        setContentView(R.layout.activity_about);
    }

    @Override
    protected void initData() {
        moProgressHUD = new MoProgressHUD(this);
    }

    @Override
    protected void bindView() {
        ButterKnife.bind(this);
        this.setSupportActionBar(toolbar);
        setupActionBar();
        tvVersion.setText(getString(R.string.version_name, MApplication.getVersionName()));
        tvQq.setText(getString(R.string.qq_group, qq));
    }

    @Override
    protected void bindEvent() {
        vwDonate.setOnClickListener(view -> DonateActivity.startThis(this));
        vwDisclaimer.setOnClickListener(view -> moProgressHUD.showAssetMarkdown("disclaimer.md"));
        vwUpdate.setOnClickListener(view -> {UpdateChecker.checkForDialog(AboutActivity.this);});
        vwQq.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText(null, qq);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clipData);
                toast(R.string.copy_complete);
            }
        });
        vwUpdateLog.setOnClickListener(view -> moProgressHUD.showAssetMarkdown("updateLog.md"));
        vwFaq.setOnClickListener(view -> moProgressHUD.showAssetMarkdown("faq.md"));
        vwShare.setOnClickListener(view -> {
            String url = "http://qiuyue.vicp.net:85/apk/qiuyue.apk";
            Bitmap bitmap = encodeAsBitmap(url);
            if (bitmap != null) {
                moProgressHUD.showImageText(bitmap, url);
            }
        });
    }

    @Override
    protected void firstRequest() {

    }

    void openIntent(String intentName, String address) {
        try {
            Intent intent = new Intent(intentName);
            intent.setData(Uri.parse(address));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            toast(R.string.can_not_open, ERROR);
        }
    }

    //设置ToolBar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.about);
        }
    }

    //菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Boolean mo = moProgressHUD.onKeyDown(keyCode, event);
        return mo || super.onKeyDown(keyCode, event);
    }

    public Bitmap encodeAsBitmap(String str) {
        Bitmap bitmap = null;
        BitMatrix result;
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            Hashtable<EncodeHintType, Object> hst = new Hashtable();
            hst.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hst.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            result = multiFormatWriter.encode(str, BarcodeFormat.QR_CODE, 600, 600, hst);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            bitmap = barcodeEncoder.createBitmap(result);
        } catch (WriterException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException iae) { // ?
            return null;
        }
        return bitmap;
    }
}