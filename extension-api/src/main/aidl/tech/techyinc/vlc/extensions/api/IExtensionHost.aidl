package tech.techyinc.vlc.extensions.api;

import tech.techyinc.vlc.extensions.api.VLCExtensionItem;
import android.net.Uri;

interface IExtensionHost {
    // Protocol version 1
    oneway void updateList(in String title, in List<VLCExtensionItem> items, boolean showParams, boolean isRefresh);
    oneway void playUri(in Uri uri, String title);
    oneway void unBind(int index);
}
