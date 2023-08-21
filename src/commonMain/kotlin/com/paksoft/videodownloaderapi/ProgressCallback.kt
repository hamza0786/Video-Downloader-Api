

package com.paksoft.videodownloaderapi

import com.paksoft.videodownloaderapi.dataholders.Result


interface ProgressCallback {
    fun onProgress(result: Result)
}
