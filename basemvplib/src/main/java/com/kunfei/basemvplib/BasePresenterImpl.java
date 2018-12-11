package com.kunfei.basemvplib;

import android.support.annotation.NonNull;

import com.kunfei.basemvplib.impl.IView;
import com.kunfei.basemvplib.impl.IPresenter;

public abstract class BasePresenterImpl<T extends IView> implements IPresenter {
    protected T mView;

    @Override
    public void attachView(@NonNull IView iView) {
        mView = (T) iView;
    }
}
