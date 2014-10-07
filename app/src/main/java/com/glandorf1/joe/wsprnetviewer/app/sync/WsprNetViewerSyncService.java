/*
 * Copyright (C) 2014 The Android Open Source Project
 * Modifications Copyright (C) 2014 Joseph D. Glandorf
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.glandorf1.joe.wsprnetviewer.app.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class WsprNetViewerSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static com.glandorf1.joe.wsprnetviewer.app.sync.WsprNetViewerSyncAdapter sWsprNetViewerSyncAdapter = null;

    @Override
    public void onCreate() {
       /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized (sSyncAdapterLock) {
            if (sWsprNetViewerSyncAdapter == null) {
                sWsprNetViewerSyncAdapter = new com.glandorf1.joe.wsprnetviewer.app.sync.WsprNetViewerSyncAdapter(getApplicationContext(), true);
            }
        }
       //super.onCreate();
    }

    /**
     * Return an object that allows the system to invoke
     * the sync adapter.
     *
     */
    @Override
    public IBinder onBind(Intent intent) {
        /*
         * Get the object that allows external processes
         * to call onPerformSync(). The object is created
         * in the base class code when the SyncAdapter
         * constructors call super()
         */
        return sWsprNetViewerSyncAdapter.getSyncAdapterBinder();
    }

}
