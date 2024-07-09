/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history creation 18/10/2011
 */

package fr.cnes.sirius.patrius.frames;

/**
 * This class allows creating a frame that is not linked to the Orekit frames tree.
 * 
 * @since 1.1
 * @author houdroger
 */
public final class OrphanFrame {

    /**
     * Private constructor.
     */
    private OrphanFrame() {
    }

    /**
     * This method creates an Orphan Frame.
     * 
     * @param name
     *        Orphan frame name
     * @return the created orphan frame
     */
    public static Frame getNewOrphanFrame(final String name) {
        return new Frame(name, false);
    }

}
