package com.kylin.skinlibrary.utils

import android.content.Context
import java.io.*

/**
 * @Description:复制assets文件到本地
 * @Auther: wangqi
 * CreateTime: 2020/8/21.
 */
object AssetsUtils {
    private const val TAG = "AssetsUtils"

    @Throws(IOException::class)
    fun doCopy(context: Context, assetsPath: String, desPath: String) {
        val srcFiles = context.assets.list(assetsPath) //for directory
        for (srcFileName in srcFiles!!) {
            val outFileName = desPath + File.separator + srcFileName
            var inFileName = assetsPath + File.separator + srcFileName
            if (assetsPath == "") { // for first time
                inFileName = srcFileName
            }
            SkinLog.d(TAG, "复制 asset → assets: $assetsPath  filename: $srcFileName  outFile: $outFileName")
            try {
                val inputStream = context.assets.open(inFileName)
                copyAndClose(inputStream, FileOutputStream(outFileName))
            } catch (e: IOException) { //if directory fails exception
                e.printStackTrace()
                File(outFileName).mkdir()
                doCopy(context, inFileName, outFileName)
            }
        }
    }

    private fun closeQuietly(out: OutputStream?) {
        try {
            out?.close()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun closeQuietly(`is`: InputStream?) {
        try {
            `is`?.close()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun copyAndClose(`is`: InputStream, out: OutputStream) {
        copy(`is`, out)
        closeQuietly(`is`)
        closeQuietly(out)
    }

    @Throws(IOException::class)
    private fun copy(`is`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var n: Int
        while (-1 != `is`.read(buffer).also { n = it }) {
            out.write(buffer, 0, n)
        }
    }
}