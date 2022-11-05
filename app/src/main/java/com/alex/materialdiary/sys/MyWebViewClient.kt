package com.alex.materialdiary.sys

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Settings.Global.getString
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentActivity
import com.alex.materialdiary.MainActivity
import com.alex.materialdiary.R
import com.alex.materialdiary.ui.login.LoginActivity

open class MyWebViewClient() : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return handleUri(view, url)
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri: Uri = request.url
        return handleUri(view, uri.toString())
    }
    open fun handleUri(view: WebView, url: String): Boolean{
        //if (url.equals("https://one.pskovedu.ru/edv/index/error/access_denied")) {

        //}
        return true;
    }
}