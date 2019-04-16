package com.kunfei.bookshelf.view.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;
import com.kunfei.basemvplib.impl.IPresenter;
import com.kunfei.bookshelf.R;
import com.kunfei.bookshelf.base.MBaseActivity;
import com.kunfei.bookshelf.bean.BookContentBean;
import com.kunfei.bookshelf.bean.BookShelfBean;
import com.kunfei.bookshelf.bean.ChapterListBean;
import com.kunfei.bookshelf.bean.SearchBookBean;
import com.kunfei.bookshelf.constant.RxBusTag;
import com.kunfei.bookshelf.help.BookshelfHelp;
import com.kunfei.bookshelf.model.UpLastChapterModel;
import com.kunfei.bookshelf.model.WebBookModel;
import com.kunfei.bookshelf.utils.NetworkUtil;
import com.kunfei.bookshelf.utils.RxUtils;
import com.kunfei.bookshelf.utils.SoftInputUtil;
import com.kunfei.bookshelf.utils.StringUtils;
import com.kunfei.bookshelf.utils.TimeUtils;
import com.kunfei.bookshelf.utils.theme.ThemeStore;
import com.victor.loading.rotate.RotateLoading;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.kunfei.bookshelf.model.content.Debug.DEBUG_TIME_FORMAT;

public class SourceDebugActivity extends MBaseActivity {
    public static String DEBUG_TAG;
    private final int REQUEST_QR = 202;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.searchView)
    SearchView searchView;
    @BindView(R.id.loading)
    RotateLoading loading;
    @BindView(R.id.action_bar)
    AppBarLayout actionBar;
    @BindView(R.id.tv_content)
    TextView tvContent;

    private CompositeDisposable compositeDisposable;

    public static void startThis(Context context, String sourceUrl) {
        if (TextUtils.isEmpty(sourceUrl)) return;
        Intent intent = new Intent(context, SourceDebugActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("sourceUrl", sourceUrl);
        context.startActivity(intent);
    }

    /**
     * P层绑定   若无则返回null;
     */
    @Override
    protected IPresenter initInjector() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RxBus.get().register(this);
    }

    @Override
    protected void onDestroy() {
        DEBUG_TAG = null;
        RxBus.get().unregister(this);
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        super.onDestroy();
    }

    /**
     * 布局载入  setContentView()
     */
    @Override
    protected void onCreateActivity() {
        getWindow().getDecorView().setBackgroundColor(ThemeStore.backgroundColor(this));
        setContentView(R.layout.activity_source_debug);
        ButterKnife.bind(this);
        this.setSupportActionBar(toolbar);
        setupActionBar();
    }

    //设置ToolBar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    // 添加菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_debug_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_scan:
            scan();
            break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 数据初始化
     */
    @Override
    protected void initData() {
        DEBUG_TAG = getIntent().getStringExtra("sourceUrl");
        initSearchView();
    }

    private void initSearchView() {
        searchView.setQueryHint(getString(R.string.debug_hint));
        searchView.onActionViewExpanded();
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (TextUtils.isEmpty(query))
                    return false;
                startDebug(query);
                SoftInputUtil.hideIMM(searchView);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void startDebug(String key) {
        UpLastChapterModel.getInstance().onDestroy();
        if (TextUtils.isEmpty(DEBUG_TAG)) return;
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
        loading.start();
        if (NetworkUtil.isUrl(key)) {
            tvContent.setText(String.format("%s %s", TimeUtils.getNowString(DEBUG_TIME_FORMAT), "≡关键字为Url"));
            BookShelfBean bookShelfBean = new BookShelfBean();
            bookShelfBean.setTag(DEBUG_TAG);
            bookShelfBean.setNoteUrl(key);
            bookShelfBean.setDurChapter(0);
            bookShelfBean.setGroup(0);
            bookShelfBean.setDurChapterPage(0);
            bookShelfBean.setFinalDate(System.currentTimeMillis());
            bookInfoDebug(bookShelfBean);
        } else {
            searchDebug(key);
        }
    }

    private void searchDebug(String key) {
        tvContent.setText(String.format("%s %s", TimeUtils.getNowString(DEBUG_TIME_FORMAT), "≡开始搜索指定关键字"));
        WebBookModel.getInstance().searchBook(key, 1, DEBUG_TAG)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new Observer<List<SearchBookBean>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onNext(List<SearchBookBean> searchBookBeans) {
                        SearchBookBean searchBookBean = searchBookBeans.get(0);
                        if (!TextUtils.isEmpty(searchBookBean.getNoteUrl())) {
                            bookInfoDebug(BookshelfHelp.getBookFromSearchBook(searchBookBean));
                        } else {
                            loading.stop();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvContent.setText(String.format("%s\n%s └%s", tvContent.getText(), TimeUtils.getNowString(DEBUG_TIME_FORMAT), e.getMessage()));
                        loading.stop();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void bookInfoDebug(BookShelfBean bookShelfBean) {
        tvContent.setText(String.format("%s\n\n%s ≡开始获取详情页", tvContent.getText(), TimeUtils.getNowString(DEBUG_TIME_FORMAT)));
        WebBookModel.getInstance().getBookInfo(bookShelfBean)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new Observer<BookShelfBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(BookShelfBean bookShelfBean) {
                        bookChapterListDebug(bookShelfBean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvContent.setText(String.format("%s\n\n%s └%s", tvContent.getText(), TimeUtils.getNowString(DEBUG_TIME_FORMAT), e.getMessage()));
                        loading.stop();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void bookChapterListDebug(BookShelfBean bookShelfBean) {
        tvContent.setText(String.format("%s\n\n%s ≡开始获取目录页", tvContent.getText(), TimeUtils.getNowString(DEBUG_TIME_FORMAT)));
        WebBookModel.getInstance().getChapterList(bookShelfBean)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new Observer<BookShelfBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onNext(BookShelfBean bookShelfBean) {
                        if (bookShelfBean.getChapterList().size() > 0) {
                            ChapterListBean chapterListBean = bookShelfBean.getChapter(0);
                            if (!TextUtils.isEmpty(chapterListBean.getDurChapterUrl())) {
                                bookContentDebug(chapterListBean, bookShelfBean.getBookInfoBean().getName());
                            } else {
                                loading.stop();
                            }
                        } else {
                            loading.stop();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvContent.setText(String.format("%s\n%s └%s", tvContent.getText(), TimeUtils.getNowString(DEBUG_TIME_FORMAT), e.getMessage()));
                        loading.stop();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void bookContentDebug(ChapterListBean chapterListBean, String bookName) {
        tvContent.setText(String.format("%s\n\n%s ≡开始获取正文页", tvContent.getText(), TimeUtils.getNowString(DEBUG_TIME_FORMAT)));
        WebBookModel.getInstance().getBookContent(chapterListBean, bookName)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new Observer<BookContentBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(BookContentBean bookContentBean) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        tvContent.setText(String.format("%s\n%s └%s", tvContent.getText(), TimeUtils.getNowString(DEBUG_TIME_FORMAT), e.getMessage()));
                        loading.stop();
                    }

                    @Override
                    public void onComplete() {
                        loading.stop();
                    }
                });
    }

    private void scan() {
        Intent intent = new Intent(this, QRCodeScanActivity.class);
        startActivityForResult(intent, REQUEST_QR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_QR:
                    String result = data.getStringExtra("result");
                    if (!StringUtils.isTrimEmpty(result)) {
                        searchView.setQuery(result, true);
                    }
            }
        }
    }

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.PRINT_DEBUG_LOG)})
    public void printDebugLog(String msg) {
        tvContent.setText(String.format("%s\n%s", tvContent.getText(), msg));
    }

}