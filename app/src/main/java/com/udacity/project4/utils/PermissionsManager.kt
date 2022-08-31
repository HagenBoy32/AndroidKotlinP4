package com.udacity.project4.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import org.koin.core.context.GlobalContext
import java.util.*

open class PermissionManager : Fragment() {
    companion object {
        private val requests: Queue<PermissionRequest> = LinkedList()
        private val newRequest = MutableLiveData<Unit>()

        private const val REQUEST_CODE = 420

        @MainThread
        fun requestPermissions(
            vararg permissions: String,
            handler: (PermissionsResultEvent) -> Unit
        ) {
            requests.offer(PermissionRequest(permissions, handler))
            newRequest.postValue({}())
        }

        fun arePermissionsGranted(vararg permissions: String): Boolean = permissions.all {
            val context = GlobalContext.getOrNull()?.koin?.get<Application>() ?: return false
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED

        }
    }

    private var currentRequest: PermissionRequest? = null

    @SuppressLint("FragmentLiveDataObserve")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d("<<PermissionsMgr>> ", "onActivityCreated: ")
        askNextRequest()

        newRequest.observe(this) {
            askNextRequest()
        }
    }

    private fun askNextRequest() {
        Log.d("<<PermissionsMgr>> ", "askNextRequest: " + REQUEST_CODE)
        Log.d("<<PermissionsMgr>> ", "b = currentRequest: " + currentRequest)
        currentRequest = requests.poll() ?: return
        Log.d("<<PermissionsMgr>> ", "a = currentRequest: " + currentRequest)
        requestPermissions(currentRequest!!.permissions, REQUEST_CODE)
        Log.d("<<requestPermissions>> ", "a = currentRequest: " + REQUEST_CODE)
    }

}

data class PermissionsResultEvent(
    val permissions: Array<out String>,
    val shouldShowRequestRationale: Boolean,
    val grantResults: IntArray
) {
    val areAllGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PermissionsResultEvent

        if (!permissions.contentEquals(other.permissions)) return false
        if (shouldShowRequestRationale != other.shouldShowRequestRationale) return false
        if (!grantResults.contentEquals(other.grantResults)) return false
        Log.d("<<PermissionMgr>> ", "equals: other.permission " + other.permissions)
        Log.d("<<PermissionMgr>> ", "equals: other.shouldShowRequestRationale " + other.permissions)
        Log.d("<<PermissionMgr>> ", "equals: other.grantResults " + other.grantResults)
        return true
    }

   // override fun hashCode(): Int {
   //     var result = permissions.contentHashCode()
   //     result = 31 * result + shouldShowRequestRationale.hashCode()
   //     result = 31 * result + grantResults.contentHashCode()
   //     Log.d("<<PermissionMgr>>", "hashCode:result " + result)
   //     return result
   // }
}

private data class PermissionRequest(
    val permissions: Array<out String>,
    val handler: (PermissionsResultEvent) -> Unit
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PermissionRequest

        if (!permissions.contentEquals(other.permissions)) return false
        if (handler != other.handler) return false

        return true
    }

    override fun hashCode(): Int {
        Log.d("<<PermissionsMgr>>", "hashCode: ")
        var result = permissions.contentHashCode()
        result = 31 * result + handler.hashCode()
        return result
    }
}