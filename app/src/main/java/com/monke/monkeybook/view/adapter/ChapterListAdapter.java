//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.view.adapter;

import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.monke.monkeybook.R;
import com.monke.monkeybook.base.observer.SimpleObserver;
import com.monke.monkeybook.bean.BookShelfBean;
import com.monke.monkeybook.bean.BookmarkBean;
import com.monke.monkeybook.bean.ChapterListBean;
import com.monke.monkeybook.help.FormatWebText;
import com.monke.monkeybook.widget.AppCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ChapterListAdapter extends RecyclerView.Adapter<ChapterListAdapter.ThisViewHolder> {

    private BookShelfBean bookShelfBean;
    private OnItemClickListener itemClickListener;
    private List<ChapterListBean> chapterListBeans = new ArrayList<>();
    private List<BookmarkBean> bookmarkBeans = new ArrayList<>();
    private int index = 0;
    private int tabPosition;
    private boolean isSearch = false;

    public ChapterListAdapter(BookShelfBean bookShelfBean, @NonNull OnItemClickListener itemClickListener) {
        this.bookShelfBean = bookShelfBean;
        this.itemClickListener = itemClickListener;
    }

    public void upChapter(int index) {
        if (bookShelfBean.getChapterListSize() > index) {
            if (tabPosition == 0 && !isSearch) {
                notifyItemChanged(index, 0);
            }
        }
    }

    public void tabChange(int tabPosition) {
        this.tabPosition = tabPosition;
        notifyDataSetChanged();
    }

    public void search(final String key) {
        chapterListBeans.clear();
        bookmarkBeans.clear();
        if (Objects.equals(key, "")) {
            isSearch = false;
            notifyDataSetChanged();
        } else {
            Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
                if (tabPosition == 0) {
                    for (ChapterListBean chapterListBean : bookShelfBean.getChapterList()) {
                        if (chapterListBean.getDurChapterName().contains(key)) {
                            chapterListBeans.add(chapterListBean);
                        }
                    }
                } else {
                    for (BookmarkBean bookmarkBean : bookShelfBean.getBookInfoBean().getBookmarkList()) {
                        if (bookmarkBean.getChapterName().contains(key)) {
                            bookmarkBeans.add(bookmarkBean);
                        } else if (bookmarkBean.getContent().contains(key)) {
                            bookmarkBeans.add(bookmarkBean);
                        }
                    }
                }
                emitter.onNext(true);
                emitter.onComplete();
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SimpleObserver<Boolean>() {
                        @Override
                        public void onNext(Boolean aBoolean) {
                            isSearch = true;
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onError(Throwable e) {

                        }
                    });
        }
    }

    @NonNull
    @Override
    public ThisViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ThisViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ThisViewHolder holder, final int position) {

    }

    @Override
    public void onBindViewHolder(@NonNull ThisViewHolder holder, int position, @NonNull List<Object> payloads) {
        int realPosition = holder.getLayoutPosition();
        if (realPosition == getItemCount() - 1) {
            holder.line.setVisibility(View.GONE);
        } else {
            holder.line.setVisibility(View.VISIBLE);
        }
        if (tabPosition == 0) {
            if (payloads.size() > 0) {
                holder.tvName.setSelected(true);
                holder.indicator.setSelected(true);
                holder.tvName.getPaint().setFakeBoldText(true);
                return;
            }
            ChapterListBean chapterListBean = isSearch ? chapterListBeans.get(realPosition) : bookShelfBean.getChapter(realPosition);
            if (chapterListBean.getDurChapterIndex() == index) {
                int color = holder.indicator.getResources().getColor(R.color.colorAccent);
                holder.tvName.setTextColor(color);
                AppCompat.setTint(holder.indicator, color);
            } else {
                ColorStateList colors = holder.indicator.getResources().getColorStateList(R.color.color_chapter_item);
                holder.tvName.setTextColor(colors);
                AppCompat.setTintList(holder.indicator, colors);
            }

            holder.tvName.setText(FormatWebText.trim(chapterListBean.getDurChapterName()));
            if (Objects.equals(bookShelfBean.getTag(), BookShelfBean.LOCAL_TAG) || chapterListBean.getHasCache(bookShelfBean.getBookInfoBean())) {
                holder.tvName.setSelected(true);
                holder.indicator.setSelected(true);
                holder.tvName.getPaint().setFakeBoldText(true);
            } else {
                holder.tvName.setSelected(false);
                holder.indicator.setSelected(false);
                holder.tvName.getPaint().setFakeBoldText(false);
            }

            holder.llName.setOnClickListener(v -> {
                setIndex(realPosition);
                itemClickListener.itemClick(chapterListBean.getDurChapterIndex(), 0, tabPosition);
            });
        } else {
            BookmarkBean bookmarkBean = isSearch ? bookmarkBeans.get(realPosition) : bookShelfBean.getBookmark(realPosition);
            holder.tvName.setText(bookmarkBean.getContent());
            holder.llName.setOnClickListener(v -> {
                itemClickListener.itemClick(bookmarkBean.getChapterIndex(), bookmarkBean.getPageIndex(), tabPosition);
            });
            holder.llName.setOnLongClickListener(view -> {
                itemClickListener.itemLongClick(bookmarkBean, tabPosition);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        if (bookShelfBean == null)
            return 0;
        else if (tabPosition == 0) {
            if (isSearch) {
                return chapterListBeans.size();
            }
            return bookShelfBean.getChapterListSize();
        } else {
            if (isSearch) {
                return bookmarkBeans.size();
            }
            return bookShelfBean.getBookmarkListSize();
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        if (tabPosition == 0) {
            this.index = index;
            notifyItemChanged(this.index, 0);
        }
    }

    static class ThisViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private View line;
        private View llName;
        private View indicator;

        ThisViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            line = itemView.findViewById(R.id.v_line);
            llName = itemView.findViewById(R.id.ll_name);
            indicator = itemView.findViewById(R.id.iv_indicator);
        }
    }

    public interface OnItemClickListener {
        void itemClick(int index, int page, int tabPosition);

        void itemLongClick(BookmarkBean bookmarkBean, int tabPosition);
    }
}
