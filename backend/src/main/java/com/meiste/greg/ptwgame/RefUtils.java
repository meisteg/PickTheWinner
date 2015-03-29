/*
 * Copyright (C) 2015 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.meiste.greg.ptwgame;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Ref;

import java.util.List;

public class RefUtils {
    public static class RefFunc<T> implements Function<T, Ref<T>> {
        public static RefFunc<Object> INSTANCE = new RefFunc<>();

        @Override
        public Ref<T> apply(T obj) {
            return ref(obj);
        }
    }

    public static <T> Ref<T> ref(T obj) {
        return obj == null ? null : Ref.create(obj);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> List<Ref<T>> ref(List<T> list) {
        return Lists.transform(list, (RefFunc) RefFunc.INSTANCE);
    }

    public static class DerefFunc<T> implements Function<Ref<T>, T> {
        public static DerefFunc<Object> INSTANCE = new DerefFunc<>();

        @Override
        public T apply(Ref<T> ref) {
            return deref(ref);
        }
    }

    public static <T> T deref(Ref<T> ref) {
        return ref == null ? null : ref.get();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> List<T> deref(List<Ref<T>> reflist) {
        return Lists.transform(reflist, (DerefFunc) DerefFunc.INSTANCE);
    }
}