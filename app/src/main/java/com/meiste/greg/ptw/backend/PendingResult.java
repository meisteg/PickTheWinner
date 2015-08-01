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

package com.meiste.greg.ptw.backend;

public interface PendingResult<R extends Result> {
    /**
     * Blocks until the task is completed. This is not allowed on the UI thread.
     *
     * @return Result of the task
     */
    R await();

    /**
     * Set the callback here to get the result delivered via a callback when the result is ready.
     *
     * @param callback The callback to call when result is available
     */
    void setResultCallback(ResultCallback<R> callback);
}
