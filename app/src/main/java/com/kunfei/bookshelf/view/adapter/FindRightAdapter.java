package com.kunfei.bookshelf.view.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.kunfei.bookshelf.R;
import com.kunfei.bookshelf.bean.FindKindBean;
import com.kunfei.bookshelf.bean.FindKindGroupBean;

import java.util.ArrayList;
import java.util.List;

public class FindRightAdapter extends RecyclerView.Adapter<FindRightAdapter.MyViewHolder> {
    private List<FindKindGroupBean> datas = new ArrayList<>();
    private Context context;

    public void setDatas(List<FindKindGroupBean> datas) {
        this.datas.clear();
        this.datas.addAll(datas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        context = viewGroup.getContext();
        return new MyViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_find_right, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        myViewHolder.sourceName.setText(datas.get(i).getGroupName());
        TextView tagView;
        for (FindKindBean findKindBean : datas.get(i).getChildrenList()) {
            tagView = (TextView) LayoutInflater.from(context).inflate(R.layout.item_search_history, myViewHolder.flexboxLayout, false);
            tagView.setText(findKindBean.getKindName());
            tagView.setOnClickListener(view -> {

            });
            myViewHolder.flexboxLayout.addView(tagView);
        }
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        View findRight;
        TextView sourceName;
        FlexboxLayout flexboxLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            findRight = itemView.findViewById(R.id.find_right);
            sourceName = itemView.findViewById(R.id.tv_source_name);
            flexboxLayout = itemView.findViewById(R.id.tfl_find_kind);
        }
    }
}