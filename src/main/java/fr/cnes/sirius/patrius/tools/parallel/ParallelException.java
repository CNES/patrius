/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * END-HISTORY
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
 * @history created 21/08/12
 */
package fr.cnes.sirius.patrius.tools.parallel;

/**
 * Extends RuntimeException for this package.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment extends RuntimeException
 * 
 * @author cardosop
 * 
 * @version $Id: ParallelException.java 17578 2017-05-10 12:20:20Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ParallelException extends RuntimeException {

    /**
     * Serial UID.
     */
    private static final long serialVersionUID = 5510913937832728366L;

    /**
     * Constructor with message.
     * 
     * @param message
     *        the message
     */
    public ParallelException(final String message) {
        super(message);
    }

    /**
     * Constructor with cause.
     * 
     * @param cause
     *        the cause
     */
    public ParallelException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with message and cause.
     * 
     * @param message
     *        the message
     * @param cause
     *        the cause
     */
    public ParallelException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
