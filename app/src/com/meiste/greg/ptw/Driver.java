/*
 * Copyright (C) 2012 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.greg.ptw;

import android.content.Context;
import android.content.res.Resources;

public final class Driver {
	private int mNumber;
	private String mName;
	
	public Driver(Context context, int id) {
		Resources res = context.getResources();
		
		mNumber = res.getIntArray(R.array.driver_nums)[id];
		mName = res.getStringArray(R.array.driver_names)[id];
	}
	
	public static int getNumDrivers(Context context) {
		return context.getResources().getIntArray(R.array.driver_nums).length;
	}
	
	public int getNumber() {
		return mNumber;
	}
	
	public String getName() {
		return mName;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
