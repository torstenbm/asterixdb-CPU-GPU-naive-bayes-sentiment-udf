/**
 * Copyright (c) 2016 - 2018 Syncleus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aparapi.internal.kernel;

import java.util.concurrent.ForkJoinPool.ManagedBlocker;

/**
 * Provides the interface for Aparapi Kernel barriers.
 * 
 * @author CoreRasurae
 */
public interface IKernelBarrier extends ManagedBlocker {
	/**
	 * Cancels the barrier.
	 * 
	 * All threads that may be waiting for the barrier are released and barrier is permanently disabled.
	 */
	public void cancelBarrier();
	
	/**
	 * Breaks the barrier.
	 * 
	 * All threads that may be waiting for the barrier are released and will throw {@link com.aparapi.exception.AparapiBrokenBarrierException}.
	 * @param t the Throwable causing the barrier to break.
	 */
	public void breakBarrier(Throwable e);
}
