package org.kevoree.library.cloud.lightlxc.wrapper

import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException

/**
 * Created by root on 25/02/14.
 */
class Reader(inputStream: InputStream, val nodeName: String, val error: Boolean) : Runnable{
    val br: BufferedReader
    {
        br = BufferedReader(InputStreamReader(inputStream));
    }
   override fun run() {
        var line: String?;
        try {
            line = br.readLine()
            while (line != null) {
                line = nodeName + "/" + line
                if (error) {
                    System.err.println(line);
                } else {
                    System.out.println(line);
                }
                line = br.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace();
        }
    }
}