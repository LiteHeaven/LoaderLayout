package com.nykj.loaderlayout.loader;

/**
 * 控制器实现
 * Create by liangy on 2020/7/17
 */
public class LoaderControllerImpl implements LoaderController {

    private LoaderLayout host;
    private ILoaderWidgetFactory factory;

    public LoaderControllerImpl(LoaderLayout host) {
        this.host = host;
    }

    @Override
    public void setWidgetFactory(ILoaderWidgetFactory factory) {
        this.factory = factory;
        host.initializeByFactory(factory);
    }

    public ILoaderWidgetFactory getFactory() {
        return factory;
    }
}
