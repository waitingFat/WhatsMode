package com.whatsmode.shopify.mvp.loader;

import android.content.Context;
import android.support.v4.content.Loader;

import com.whatsmode.shopify.mvp.MvpPresenter;

public abstract class PresenterLoader<T extends MvpPresenter> extends Loader<T> {

    private T presenter;

    public PresenterLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {

        // 如果已经有Presenter实例那就直接返回
        if (presenter != null) {
            deliverResult(presenter);
            return;
        }

        // 如果没有
        forceLoad();
    }

    @Override
    protected void onForceLoad() {
        // 通过工厂方法来实例化Presenter
        presenter = create();
        // 返回Presenter
        deliverResult(presenter);
    }

    abstract T create();

    @Override
    protected void onReset() {
        presenter = null;
    }
}