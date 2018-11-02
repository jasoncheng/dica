package link.mawa.android.util

import android.util.Log

fun Any.eLog(log: String) = Log.e(this::class.java.simpleName, log)
fun Any.iLog(log: String) = Log.i(this::class.java.simpleName, log)
fun Any.dLog(log: String) = Log.d(this::class.java.simpleName, log)