/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.helios.macos;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import static org.httprpc.kilo.util.Collections.*;

public interface ObjectiveCRuntime extends Library {
    ObjectiveCRuntime instance = Native.load("objc", ObjectiveCRuntime.class, mapOf(
        entry(Library.OPTION_FUNCTION_MAPPER, (FunctionMapper)(library, method) -> {
            var name = method.getName();

            if (name.equals("objc_msgSend_float")
                || name.equals("objc_msgSend_double")
                || name.equals("objc_msgSend_String")) {
                return "objc_msgSend";
            } else {
                return method.getName();
            }
        })
    ));

    Pointer objc_getClass(String name);

    Pointer sel_registerName(String name);

    long objc_msgSend(Pointer self, Pointer selector);
    long objc_msgSend(Pointer self, Pointer selector, Object arg);
    long objc_msgSend(Pointer self, Pointer selector, Object arg1, Object arg2);

    float objc_msgSend_float(Pointer self, Pointer selector);

    double objc_msgSend_double(Pointer self, Pointer selector);

    String objc_msgSend_String(Pointer self, Pointer selector);

    Pointer alloc = instance.sel_registerName("alloc");
    Pointer release = instance.sel_registerName("release");

    static Pointer alloc(Pointer type) {
        return new Pointer(instance.objc_msgSend(type, alloc));
    }

    static void release(Pointer self) {
        instance.objc_msgSend(self, release);
    }
}
