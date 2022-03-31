package com.folioreader.ui.base;

import android.content.Context;
import com.folioreader.Config;
import com.folioreader.Constants;
import com.folioreader.R;

/**
 * @author gautam chibde on 14/6/17.
 */

public final class HtmlUtil {

    /**
     * Function modifies input html string by adding extra css,js and font information.
     *
     * @param context     Activity Context
     * @param htmlContent input html raw data
     * @return modified raw html string
     */
    public static String getHtmlContent(Context context, String htmlContent, Config config) {

        String cssPath =
                String.format(context.getString(R.string.css_tag), "file:///android_asset/css/Style.css");

        String jsPath = String.format(context.getString(R.string.script_tag),
                "file:///android_asset/js/jsface.min.js") + "\n";

        jsPath = jsPath + String.format(context.getString(R.string.script_tag),
                "file:///android_asset/js/jquery-3.4.1.min.js") + "\n";

        jsPath = jsPath + String.format(context.getString(R.string.script_tag),
                "file:///android_asset/js/rangy-core.js") + "\n";

        jsPath = jsPath + String.format(context.getString(R.string.script_tag),
                "file:///android_asset/js/rangy-highlighter.js") + "\n";

        jsPath = jsPath + String.format(context.getString(R.string.script_tag),
                "file:///android_asset/js/rangy-classapplier.js") + "\n";

        jsPath = jsPath + String.format(context.getString(R.string.script_tag),
                "file:///android_asset/js/rangy-serializer.js") + "\n";

        jsPath = jsPath + String.format(context.getString(R.string.script_tag),
                "file:///android_asset/js/Bridge.js") + "\n";

        jsPath = jsPath + String.format(context.getString(R.string.script_tag),
                "file:///android_asset/js/rangefix.js") + "\n";

        jsPath = jsPath + String.format(context.getString(R.string.script_tag),
                "file:///android_asset/js/readium-cfi.umd.js") + "\n";

        jsPath = jsPath + String.format(context.getString(R.string.script_tag_method_call),
                "setMediaOverlayStyleColors('#C0ED72','#C0ED72')") + "\n";

        jsPath = jsPath
                + "<meta name=\"viewport\" content=\"height=device-height, user-scalable=no\" />";

        String toInject = "\n" + cssPath + "\n" + jsPath + "\n</head>";
        htmlContent = htmlContent.replace("</head>", toInject);

        String classes = "";
        switch (config.getFont()) {
            case Constants.FONT_ARIAL:
                classes = "arial";
                break;
            case Constants.FONT_GEORGIA:
                classes = "georgia";
                break;
            case Constants.FONT_IOWAN_OLD_STYLE:
                classes = "iowan_old_style";
                break;
            case Constants.FONT_SF_PRO_DISPLAY:
                classes = "sf_pro_display";
                break;
            case Constants.FONT_TIMES_NEW_ROMAN:
                classes = "times_new_roman";
                break;
            case Constants.FONT_VERDANA:
                classes = "verdana";
                break;
            case Constants.FONT_ANDADA:
                classes = "andada";
                break;
            case Constants.FONT_LATO:
                classes = "lato";
                break;
            case Constants.FONT_LORA:
                classes = "lora";
                break;
            case Constants.FONT_RALEWAY:
                classes = "raleway";
                break;
            default:
                break;
        }

        if (config.isNightMode()) {
            classes += " nightMode";
        }

        switch (config.getFontSize()) {
            case 0:
                classes += " textSize0";
                break;
            case 1:
                classes += " textSize1";
                break;
            case 2:
                classes += " textSize2";
                break;
            case 3:
                classes += " textSize3";
                break;
            case 4:
                classes += " textSize4";
                break;
            case 5:
                classes += " textSize5";
                break;
            case 6:
                classes += " textSize6";
                break;
            case 7:
                classes += " textSize7";
                break;
            case 8:
                classes += " textSize8";
                break;
            case 9:
                classes += " textSize9";
                break;
            case 10:
                classes += " textSize10";
                break;
            case 11:
                classes += " textSize11";
                break;
            default:
                break;
        }

        htmlContent = htmlContent.replace("<html", "<html class=\"" + classes + "\"" +
                " onclick=\"onClickHtml()\"");
        return htmlContent;
    }
}
