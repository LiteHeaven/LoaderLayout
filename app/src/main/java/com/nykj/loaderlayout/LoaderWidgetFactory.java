package com.nykj.loaderlayout;

import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Keep;

import com.nykj.loaderlayout.loader.IErrorViewHolder;
import com.nykj.loaderlayout.loader.ILoaderWidgetFactory;
import com.nykj.loaderlayout.loader.IProgressViewHolder;

/**
 * LoadLayout小组件工厂实现
 * Create by liangy on 2020/7/21
 */
@Keep
public class LoaderWidgetFactory implements ILoaderWidgetFactory {
    @Override
    public IErrorViewHolder createErrorView(ViewGroup parent) {
        return ErrorViewHolder.create(parent);
    }

    @Override
    public IProgressViewHolder createProgressView(ViewGroup parent) {
        return ProgressViewHolder.create(parent);
    }

    private static class ErrorViewHolder implements IErrorViewHolder {
        private View itemView;

        public ErrorViewHolder(View itemView) {
            this.itemView = itemView;
        }

        @Override
        public View getItemView() {
            return itemView;
        }

        public static ErrorViewHolder create(ViewGroup parent){
            return new ErrorViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_error, parent, false));
        }
    }

    private static class ProgressViewHolder implements IProgressViewHolder {
        private View itemView;

        public ProgressViewHolder(View itemView) {
            this.itemView = itemView;
            ImageView iv = (ImageView) itemView;
            AnimationDrawable ad = (AnimationDrawable) iv.getDrawable();
            ad.start();
        }

        @Override
        public View getItemView() {
            return itemView;
        }

        public static ProgressViewHolder create(ViewGroup parent){
            return new ProgressViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_progress, parent, false));
        }
    }
}
