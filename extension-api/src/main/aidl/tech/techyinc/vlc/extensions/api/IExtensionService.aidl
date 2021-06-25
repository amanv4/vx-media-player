package tech.techyinc.vlc.extensions.api;

import tech.techyinc.vlc.extensions.api.IExtensionHost;
import tech.techyinc.vlc.extensions.api.VLCExtensionItem;

interface IExtensionService {
    // Protocol version 1
    oneway void onInitialize(int index, in IExtensionHost host);
    oneway void browse(String stringId);
    oneway void refresh();
}
