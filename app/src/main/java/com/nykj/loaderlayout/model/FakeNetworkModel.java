package com.nykj.loaderlayout.model;

import android.os.Handler;

/**
 * 虚假网络请求model
 * Create by liangy on 2020/7/16
 */
public class FakeNetworkModel {

    public static void requestNetwork(final FakeNetworkListener listener){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 假装网络请求返回
                //loaderLayout.stopLoading();
                if (listener != null){
                    listener.onResult(getFakeResult());
                }
            }
        }, 1000);
    }

    private static String getFakeResult(){
        int random = (int) (Math.random() * 3);

        switch (random){
            case 1:
                return "7月21日0时至24时，北京市无新增报告本地确诊病例、疑似病例和无症状感染者，治愈出院病例17例；无新增报告境外输入确诊病例、疑似病例和无症状感染者。\n" +
                        "\n" +
                        "6月11日0时至7月21日24时，累计报告本地确诊病例335例，在院87例，治愈出院248例。尚在观察的无症状感染者9例；无新增报告境外输入新冠肺炎确诊病例、疑似病例和无症状感染者。\n" +
                        "\n" +
                        "全市已连续16天无新增报告确诊病例，具体为平谷区自有疫情以来无报告病例、延庆区180天、怀柔区166天、顺义区164天、密云区161天、石景山区37天、门头沟区36天、房山区36天、东城区35天、通州区31天、朝阳区30天、西城区29天、海淀区26天、昌平区26天、大兴区21天、丰台区16天。全市所有街乡均为低风险地区。";
            case 2:
                return "7月21日0—24时，31个省（自治区、直辖市）和新疆生产建设兵团报告新增确诊病例14例，其中境外输入病例5例（上海2例，广东2例，云南1例），本土病例9例（均在新疆）；无新增死亡病例；无新增疑似病例。\n" +
                        "\n" +
                        "当日新增治愈出院病例23例，解除医学观察的密切接触者434人，重症病例较前一日减少1例。\n" +
                        "\n" +
                        "境外输入现有确诊病例80例（其中重症病例2例），现有疑似病例1例。累计确诊病例2020例，累计治愈出院病例1940例，无死亡病例。\n" +
                        "\n" +
                        "截至7月21日24时，据31个省（自治区、直辖市）和新疆生产建设兵团报告，现有确诊病例233例（其中重症病例6例），累计治愈出院病例78840例，累计死亡病例4634例，累计报告确诊病例83707例，现有疑似病例1例。累计追踪到密切接触者772804人，尚在医学观察的密切接触者6988人。\n" +
                        "\n" +
                        "31个省（自治区、直辖市）和新疆生产建设兵团报告新增无症状感染者22例（境外输入8例）；当日无转为确诊病例；当日解除医学观察7例（境外输入4例）；尚在医学观察无症状感染者164例（境外输入84例）。\n" +
                        "\n" +
                        "累计收到港澳台地区通报确诊病例2519例。其中，香港特别行政区2018例（出院1324例，死亡14例），澳门特别行政区46例（出院46例），台湾地区455例（出院440例，死亡7例）。";
            case 0:
            default:
                return null;
        }
    }
}