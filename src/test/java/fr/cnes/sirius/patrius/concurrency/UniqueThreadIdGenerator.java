/**
 * 
 * Copyright 2011-2022 CNES
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
 *
 * 
 * @history creation 16/11/11
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unique thread ID generator.
 * 
 * @since 1.1
 * 
 */
public final class UniqueThreadIdGenerator {

    /** Unique ID provider, as a thread-safe atomic integer. */
    private static final AtomicInteger UNIQUE_ID_MAKER = new AtomicInteger(0);

    /** Unique ID, as a thread-local integer. */
    private static final ThreadLocal<Integer> UNIQUE_THREAD_ID =
        new ThreadLocal<Integer>(){
            @Override
            protected Integer initialValue() {
                return UNIQUE_ID_MAKER.getAndIncrement();
            }
        };

    /** Private constructor. */
    private UniqueThreadIdGenerator() {
    }

    /**
     * Unique ID per thread provider.
     * 
     * @return a unique integer identifying the thread.
     */
    public static int getCurrentThreadId() {
        return UNIQUE_THREAD_ID.get();
    }
}
