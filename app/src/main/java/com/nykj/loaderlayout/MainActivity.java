package com.nykj.loaderlayout;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nykj.loaderlayout.model.FakeNetworkListener;
import com.nykj.loaderlayout.model.FakeNetworkModel;
import com.nykj.loaderlayout.loader.LoaderLayout;
import com.nykj.loaderlayout.loader.LoaderListenerAdapter;

/**
 * 使用{@link LoaderWidgetFactory}实例化LoaderLayout
 */
public class MainActivity extends AppCompatActivity {

    private LoaderLayout loaderLayout;
    private TextView tvData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvData = findViewById(R.id.tv_data);
        loaderLayout = findViewById(R.id.fl_load);
        loaderLayout.getController().setWidgetFactory(new LoaderWidgetFactory());
        loaderLayout.setLoaderListener(new LoaderListenerAdapter() {
            @Override
            public void onHeaderLoading() {
                fakeRequestNetwork();
            }

            @Override
            public void onFooterLoading() {
                fakeRequestNetwork();
            }
        });
        loaderLayout.startHeaderLoading();
    }

    private void fakeRequestNetwork(){
        FakeNetworkModel.requestNetwork(new FakeNetworkListener() {
            @Override
            public void onResult(String result) {
                loaderLayout.stopLoading();

                if (TextUtils.isEmpty(result)){
                    loaderLayout.showErrorLayout(true);
                    tvData.setText("");
                }else{
                    loaderLayout.showErrorLayout(false);
                    tvData.setText(result);
                }
            }
        });
    }
}