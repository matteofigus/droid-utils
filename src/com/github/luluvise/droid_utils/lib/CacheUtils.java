/*
 * Copyright 2013 Luluvise Ltd
 *
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
package com.github.luluvise.droid_utils.lib;

import java.io.File;
import java.lang.reflect.Method;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import android.content.Context;
import android.os.Build;

import com.google.common.annotations.Beta;

/**
 * Helper class containing static utility methods for caching mechanisms.<br>
 * 
 * See {@link http://developer.android.com/guide/topics/data/data-storage.html}
 * 
 * @since 1.0
 * @author Marco Salis
 */
@Beta
@Immutable
public class CacheUtils {

	/**
	 * Application storage caches possible locations
	 */
	public enum CacheLocation {
		INTERNAL,
		EXTERNAL
	}

	/**
	 * Default external cache storage path for Android apps
	 */
	private static final String EXT_CACHE_PATH = "Android/data/%s/cache/";

	private CacheUtils() {
		// hidden constructor, no instantiation needed
	}

	/**
	 * Retrieve the current working application cache directory, selected
	 * depending on the current application caching policy, the Android device
	 * platform version and the state of any existing external storage.
	 * 
	 * It is recommended to use a sub-directory of the returned one to avoid
	 * polluting the application's cache root.<br>
	 * 
	 * Note: this method is usually not suitable when large spaces caches are
	 * required in devices with two cache locations. Use
	 * {@code getAppCacheDir(CacheLocation.EXTERNAL, true)} instead.
	 * 
	 * The cache directory is automatically created if not existing, and the
	 * returned File (if not null) is guaranteed to exist and to be writable.
	 * 
	 * @param context
	 *            A {@link Context} to retrieve the caches location
	 * @return A File for the root directory to use for storing caches, or null
	 *         if an unrecoverable error prevented the method from getting any
	 *         suitable cache location
	 */
	@CheckForNull
	public static File getAppCacheDir(@Nonnull Context context) {
		// failover cache location to use when external storage is not mounted
		File cacheDir = context.getCacheDir();

		// attempt to use external storage cache directory
		File extCacheDir = getExternalAppCacheDir(context);
		if (extCacheDir != null) {
			cacheDir = extCacheDir;
		}

		return cacheDir;
	}

	/**
	 * Retrieve the current working application cache directory, trying to use
	 * the specified cache location.
	 * 
	 * When the fallback flag is set to true, if the preferred location is not
	 * available, the method falls back to the other available location (if
	 * any).
	 * 
	 * See {@link CacheUtils#getAppCacheDir()} for other recommendations.
	 * 
	 * @param context
	 *            A {@link Context} to retrieve the caches location
	 * @param location
	 *            The requested cache location
	 * @return a File containing the location, null if the specified caches are
	 *         not available
	 */
	@CheckForNull
	public static File getAppCacheDir(@Nonnull Context context, @Nonnull CacheLocation location,
			boolean fallback) {
		File cacheDir = null;
		switch (location) {
		case INTERNAL:
			cacheDir = getInternalAppCacheDir(context);
			if (cacheDir == null && fallback) {
				cacheDir = getExternalAppCacheDir(context);
			}
			break;
		case EXTERNAL:
			cacheDir = getExternalAppCacheDir(context);
			if (cacheDir == null && fallback) {
				cacheDir = getInternalAppCacheDir(context);
			}
			break;
		}
		return cacheDir;
	}

	/**
	 * Helper method to retrieve the application internal storage cache
	 * directory and make sure it exists.
	 */
	@CheckForNull
	static File getInternalAppCacheDir(@Nonnull Context context) {
		File intCacheDir = context.getCacheDir();
		if (intCacheDir != null && !intCacheDir.exists()) {
			if (!intCacheDir.mkdirs() || !intCacheDir.canWrite()) {
				intCacheDir = null;
			}
		}
		return intCacheDir;
	}

	/**
	 * Helper method to retrieve the application external storage cache
	 * directory from any platform version.
	 */
	static File getExternalAppCacheDir(@Nonnull Context context) {
		File extCacheDir = null;

		if (StorageUtils.isExternalStorageMounted()) { // only works if mounted

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
				// we cannot use getExternalCacheDir(), retrieve it manually
				final File extStorage = android.os.Environment.getExternalStorageDirectory();
				final String packageName = context.getPackageName();

				if (extStorage != null) {
					extCacheDir = new File(extStorage.getAbsolutePath() + File.separator
							+ String.format(EXT_CACHE_PATH, packageName));
				}
			} else { // use reflection here
				try {
					Method getExternalCacheDir = Context.class.getMethod("getExternalCacheDir");
					extCacheDir = (File) getExternalCacheDir.invoke(context);
				} catch (Exception e) { // something unexpected went wrong
					e.printStackTrace();
				}
			}

			// create directory tree if not existing
			if (extCacheDir != null)
				if (!FileUtils.createDir(extCacheDir) || !extCacheDir.canWrite())
					extCacheDir = null;
		}

		return extCacheDir;
	}

}