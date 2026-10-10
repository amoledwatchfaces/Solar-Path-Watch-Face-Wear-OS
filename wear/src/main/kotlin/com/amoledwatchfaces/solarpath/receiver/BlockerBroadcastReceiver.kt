/*
 * Copyright (C) 2026 amoledwatchfaces™
 *
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0.html
 */
package com.amoledwatchfaces.solarpath.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BlockerBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: consumes touch events on the Heads Up screen to prevent background complications from being tapped
    }
}
