package com.nykj.loaderlayout.loader;

import android.view.ViewGroup;

/**
 * LoaderLayout小组件工厂
 * Create by liangy on 2020/7/21
 */
public interface ILoaderWidgetFactory {

    IErrorViewHolder createErrorView(ViewGroup parent);

    IProgressViewHolder createProgressView(ViewGroup parent);
}
