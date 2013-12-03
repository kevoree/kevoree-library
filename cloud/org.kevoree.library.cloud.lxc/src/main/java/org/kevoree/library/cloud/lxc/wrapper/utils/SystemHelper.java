/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kevoree.library.cloud.lxc.wrapper.utils;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 15/10/12
 * Time: 17:24
 * To change this template use File | Settings | File Templates.
 */
public class SystemHelper {

    public enum  OS {
        WIN32,
        WIN64,
        OSX32,
        OSX64,
        NIX32,
        NIX64,
        ARM,
        NONE
    }
    public static String getExtension() {
        if (System.getProperty("os.name").toLowerCase().contains("nux")) {
            return ".so";
        }
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            return ".dynlib";
        }
        return null;
    }

    public static String getPathOS() {
        if (System.getProperty("os.name").toLowerCase().contains("nux")) {
            if(isArm())
            {
                return "arm"+ File.separatorChar;
            }
            if (is64()) {
                return "nix64"+ File.separatorChar;
            } else {
                return "nix32"+ File.separatorChar;
            }
        }
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            return "osx"+ File.separatorChar;
        }
        return null;
    }


    public static OS getOS() {
        if (System.getProperty("os.name").toLowerCase().contains("nux")) {
            if(isArm())
            {
                return OS.ARM;
            }
            if (is64()) {
                return OS.NIX64;
            } else {
                return OS.NIX32;
            }
        }
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            if (is64()) {
                return OS.OSX64;
            } else {
                return OS.OSX32;
            }
        }
        return OS.NONE;
    }

    public static boolean isArm() {
        String os = System.getProperty("os.arch").toLowerCase();
        return (os.contains("arm"));
    }


    public static boolean is64() {
        String os = System.getProperty("os.arch").toLowerCase();
        return (os.contains("64"));
    }

}
